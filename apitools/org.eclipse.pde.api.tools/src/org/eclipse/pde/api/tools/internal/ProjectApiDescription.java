/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.pde.api.tools.internal.model.ApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.scanner.TagScanner;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Implementation of an API description for a Java project.
 * 
 * @since 1.0
 */
public class ProjectApiDescription extends ApiDescription {
		
	/**
	 * Associated API component.
	 */
	private IJavaProject fProject;
	
	/**
	 * Only valid when connected to a bundle. Used to initialize
	 * package exports.
	 */
	private BundleDescription fBundle;
	
	/**
	 * Time stamp at which package information was created
	 */
	long fPackageTimeStamp = 0L;
	
	/** 
	 * Whether a package refresh is in progress
	 */
	private boolean fRefreshingInProgress = false;
	
	/**
	 * Associated manifest file
	 */
	protected IFile fManifestFile;
	
	/**
	 * Class file container cache used for tag scanning.
	 * Maps output locations to containers.
	 * 
	 * TODO: these could become out of date with class path changes.
	 */
	private Map fClassFileContainers;
	
	/**
	 * Whether this API description is in synch with its project. Becomes
	 * false if anything in a project changes. When true, visiting can
	 * be performed by traversing the cached nodes, rather than traversing
	 * the java model elements (effectively building the cache).
	 */
	private boolean fInSynch = false;
			
	/**
	 * A node for a package.
	 */
	class PackageNode extends ManifestNode {

		private IPackageFragment[] fFragments;
		/**
		 * Constructs a new node.
		 * 
		 * @param parent
		 * @param element
		 * @param visibility
		 * @param restrictions
		 */
		public PackageNode(IPackageFragment fragments[], ManifestNode parent, IElementDescriptor element, int visibility, int restrictions) {
			super(parent, element, visibility, restrictions);
			fFragments = fragments;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.ApiDescription.ManifestNode#refresh()
		 */
		public ManifestNode refresh() {
			refreshPackages();
			for (int i = 0; i < fFragments.length; i++) {
				if (!fFragments[i].exists()) {
					modified();
					return null;		
				}
			}
			return this;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.ApiDescription.ManifestNode#persistXML(org.w3c.dom.Document, org.w3c.dom.Element, java.lang.String)
		 */
		void persistXML(Document document, Element parent) {
			Element pkg = document.createElement(IApiXmlConstants.ELEMENT_PACKAGE);
			for (int i = 0; i < fFragments.length; i++) {
				Element fragment = document.createElement(IApiXmlConstants.ELEMENT_PACKAGE_FRAGMENT);
				fragment.setAttribute(IApiXmlConstants.ATTR_HANDLE, fFragments[i].getHandleIdentifier());
				pkg.appendChild(fragment);
			}
			persistAnnotations(pkg);
			persistChildren(document, pkg, children);
			parent.appendChild(pkg);
		}		
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.ApiDescription.ManifestNode#toString()
		 */
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			String name = ((IPackageDescriptor)element).getName();
			buffer.append("Package Node: ").append(name.equals("") ? "<default package>" : name); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			buffer.append("\nVisibility: ").append(Util.getVisibilityKind(visibility)); //$NON-NLS-1$
			buffer.append("\nRestrictions: ").append(Util.getRestrictionKind(restrictions)); //$NON-NLS-1$
			if(fFragments != null) {
				buffer.append("\nFragments:"); //$NON-NLS-1$
				IPackageFragment fragment = null;
				for(int i = 0; i < fFragments.length; i++) {
					fragment = fFragments[i];
					buffer.append("\n\t").append(fragment.getElementName()); //$NON-NLS-1$
					buffer.append(" ["); //$NON-NLS-1$
					buffer.append(fragment.getParent().getElementName());
					buffer.append("]"); //$NON-NLS-1$
				}
			}
			return buffer.toString();
		}
	}
	
	/**
	 * Node for a reference type.
	 */
	class TypeNode extends ManifestNode {
		
		long fTimeStamp = -1L;
		
		private boolean fRefreshing = false;
		
		private IType fType;

		/**
		 * Constructs a node for a reference type.
		 * 
		 * @param type
		 * @param parent
		 * @param element
		 * @param visibility
		 * @param restrictions
		 */
		public TypeNode(IType type, ManifestNode parent, IElementDescriptor element, int visibility, int restrictions) {
			super(parent, element, visibility, restrictions);
			fType = type;
			if (parent instanceof TypeNode) {
				fTimeStamp = ((TypeNode)parent).fTimeStamp;
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.ApiDescription.ManifestNode#refresh()
		 */
		public synchronized ManifestNode refresh() {
			if (fRefreshing) {
				return this;
			}
			try {
				fRefreshing = true;
				ICompilationUnit unit = fType.getCompilationUnit();
				if (unit != null) {
					IResource resource = null;
					try {
						resource = unit.getUnderlyingResource();
					} catch (JavaModelException e) {
						// exception if the resource does not exist
						if (!e.getJavaModelStatus().isDoesNotExist()) {
							ApiPlugin.log(e.getStatus());
							return this;
						}
					}
					if (resource != null && resource.exists()) {
						long stamp = resource.getModificationStamp();
						if (stamp != fTimeStamp) {
							modified();
							children.clear();
							restrictions = RestrictionModifiers.NO_RESTRICTIONS;
							fTimeStamp = resource.getModificationStamp();
							try {
								TagScanner.newScanner().scan(unit, ProjectApiDescription.this,
									getClassFileContainer((IPackageFragmentRoot) fType.getPackageFragment().getParent()));
							} catch (CoreException e) {
								ApiPlugin.log(e.getStatus());
							}
						}
					} else {
						// element has been removed
						modified();
						parent.children.remove(element);
						return null;
					}
				} else {
					// TODO: binary type
				}
			} finally {
				fRefreshing = false;
			}
			return this;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.ApiDescription.ManifestNode#persistXML(org.w3c.dom.Document, org.w3c.dom.Element, java.lang.String)
		 */
		void persistXML(Document document, Element parent) {
			Element type = document.createElement(IApiXmlConstants.ELEMENT_TYPE);
			type.setAttribute(IApiXmlConstants.ATTR_HANDLE, fType.getHandleIdentifier());
			persistAnnotations(type);
			type.setAttribute(IApiXmlConstants.ATTR_MODIFICATION_STAMP, Long.toString(fTimeStamp));
			persistChildren(document, type, children);
			parent.appendChild(type);
		}	
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.internal.ApiDescription.ManifestNode#toString()
		 */
		public String toString() {
			StringBuffer buffer = new StringBuffer();
			buffer.append("Type Node: ").append(fType.getFullyQualifiedName()); //$NON-NLS-1$
			buffer.append("\nVisibility: ").append(Util.getVisibilityKind(visibility)); //$NON-NLS-1$
			buffer.append("\nRestrictions: ").append(Util.getRestrictionKind(restrictions)); //$NON-NLS-1$
			if(parent != null) {
				String pname = parent.element.getElementType() == IElementDescriptor.T_PACKAGE ?
						((IPackageDescriptor)parent.element).getName() : ((IReferenceTypeDescriptor)parent.element).getQualifiedName();
				buffer.append("\nParent: ").append(pname); //$NON-NLS-1$
			}
			return buffer.toString();
		}
	}
	
	/**
	 * Constructs a new API description for the given Java project.
	 * 
	 * @param component
	 */
	public ProjectApiDescription(IJavaProject project) {
		super(project.getElementName());
		fProject = project;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiDescription#accept(org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor)
	 */
	public synchronized void accept(ApiDescriptionVisitor visitor) {
		boolean completeVisit = true;
		if (fInSynch) {
			super.accept(visitor);
		} else {
			try {
				IPackageFragment[] fragments = getLocalPackageFragments();
				IJavaElement[] children = null;
				IJavaElement child = null;
				ICompilationUnit unit = null;
				for (int j = 0; j < fragments.length; j++) {
					if (DEBUG) {
						System.out.println("\t" + fragments[j].getElementName().toString()); //$NON-NLS-1$
					}
					IPackageDescriptor packageDescriptor = Factory.packageDescriptor(fragments[j].getElementName());
					// visit package
					ManifestNode pkgNode = findNode(packageDescriptor, false);
					if (pkgNode != null) {
						IApiAnnotations annotations = resolveAnnotations(pkgNode, packageDescriptor);
						if (visitor.visitElement(packageDescriptor, annotations)) {
							children = fragments[j].getChildren();
							for (int k = 0; k < children.length; k++) {
								child = children[k];
								if (child instanceof ICompilationUnit) {
									unit = (ICompilationUnit) child;
									String cuName = unit.getElementName(); 
									String tName = cuName.substring(0, cuName.length() - ".java".length()); //$NON-NLS-1$
									visit(visitor, unit.getType(tName));
								} else if (child instanceof IClassFile) {
									visit(visitor, ((IClassFile)child).getType());
								}
							}
						} else {
							completeVisit = false;
						}
						visitor.endVisitElement(packageDescriptor, annotations);
					}
				}
			} catch (JavaModelException e) {
				completeVisit = false;
				ApiPlugin.log(e.getStatus());
			} finally {
				if (completeVisit) {
					fInSynch = true;
				}
			}
		}
	}
	
	/**
	 * Visits a type.
	 * 
	 * @param visitor
	 * @param owningComponent
	 * @param type
	 */
	private void visit(ApiDescriptionVisitor visitor, IType type) {
		IElementDescriptor element = getElementDescriptor(type);
		ManifestNode typeNode = findNode(element, false);
		if (typeNode != null) {
			IApiAnnotations annotations = resolveAnnotations(typeNode, element);
			if (visitor.visitElement(element, annotations)) {
				// children
				if (typeNode.children != null) {
					visitChildren(visitor, typeNode.children);
				}
			}
			visitor.endVisitElement(element, annotations);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.ApiDescription#isInsertOnResolve(org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor)
	 */
	protected boolean isInsertOnResolve(IElementDescriptor elementDescriptor) {
		switch (elementDescriptor.getElementType()) {
			case IElementDescriptor.T_METHOD:
			case IElementDescriptor.T_FIELD:
				return false;
			case IElementDescriptor.T_REFERENCE_TYPE:
				// no need to insert member types
				return ((IReferenceTypeDescriptor) elementDescriptor).getEnclosingType() == null;
			default:
				return true;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.ApiDescription#createNode(org.eclipse.pde.api.tools.internal.ApiDescription.ManifestNode, org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor)
	 */
	protected ManifestNode createNode(ManifestNode parentNode, IElementDescriptor element) {
		switch (element.getElementType()) {
			case IElementDescriptor.T_PACKAGE:
				try {
					IPackageDescriptor pkg = (IPackageDescriptor) element;
					IPackageFragmentRoot[] roots = getJavaProject().getPackageFragmentRoots();
					List fragments = new ArrayList(1);
					for (int i = 0; i < roots.length; i++) {
						IPackageFragmentRoot root = roots[i];
						IClasspathEntry entry = root.getRawClasspathEntry();
						switch (entry.getEntryKind()) {
						case IClasspathEntry.CPE_SOURCE:
						case IClasspathEntry.CPE_LIBRARY:
							IPackageFragment fragment = root.getPackageFragment(pkg.getName());
							if (fragment.exists()) {
								fragments.add(fragment);
							}
						}
					}
					if (fragments.isEmpty()) {
						return null;
					} else {
						return newPackageNode((IPackageFragment[])fragments.toArray(new IPackageFragment[fragments.size()]), parentNode, element, VisibilityModifiers.PRIVATE, RestrictionModifiers.NO_RESTRICTIONS);
					}
					
				} catch (CoreException e) {
					return null;
				}
			case IElementDescriptor.T_REFERENCE_TYPE:
				IReferenceTypeDescriptor descriptor = (IReferenceTypeDescriptor) element;
				try {
					IType type = null;
					String name = descriptor.getName();
					if (parentNode instanceof PackageNode) {
						IPackageFragment[] fragments = ((PackageNode) parentNode).fFragments; 
						for (int i = 0; i < fragments.length; i++) {
							IPackageFragment fragment = fragments[i];
							if (fragment.getKind() == IPackageFragmentRoot.K_SOURCE) {
								ICompilationUnit unit = fragment.getCompilationUnit(name + ".java"); //$NON-NLS-1$
								try {
									IResource resource = unit.getUnderlyingResource();
									if (resource != null) {
										type = unit.getType(name);
									}
								} catch (JavaModelException jme) {
									// exception if the resource does not exist
									if (!jme.getJavaModelStatus().isDoesNotExist()) {
										throw jme;
									}
								}
							} else {
								IClassFile file = fragment.getClassFile(name + ".class"); //$NON-NLS-1$
								if (file.exists()) {
									type = file.getType();
								}
							}							
						}
					} else if (parentNode instanceof TypeNode) {
						type = ((TypeNode)parentNode).fType.getType(name);
					}
					if (type != null) {
						return newTypeNode(type, parentNode, element, VISIBILITY_INHERITED, RestrictionModifiers.NO_RESTRICTIONS);
					}
				} catch (CoreException e ) {
					return null;
				}
				return null;
		}
		return super.createNode(parentNode, element);
	}

	/** 
	 * Constructs and returns a new node for the given package fragment.
	 * 
	 * @param fragment
	 * @param parent
	 * @param descriptor
	 * @param vis
	 * @param res
	 * @return
	 */
	PackageNode newPackageNode(IPackageFragment[] fragments, ManifestNode parent, IElementDescriptor descriptor, int vis, int res) {
		return new PackageNode(fragments, parent, descriptor, vis, res);
	}

	/**
	 * Constructs and returns a new node for the given type.
	 * 
	 * @param type
	 * @param parent
	 * @param descriptor
	 * @param vis
	 * @param res
	 * @return
	 */
	TypeNode newTypeNode(IType type, ManifestNode parent, IElementDescriptor descriptor, int vis, int res) {
		return new TypeNode(type, parent, descriptor, vis, res);
	}
	
	/**
	 * Constructs a new manifest node.
	 * 
	 * @param parent
	 * @param element
	 * @param vis
	 * @param res
	 * @return
	 */
	ManifestNode newNode(ManifestNode parent, IElementDescriptor element, int vis, int res) {
		return new ManifestNode(parent, element, vis, res);
	}

	/**
	 * Refreshes package nodes if required.
	 */
	private synchronized void refreshPackages() {
		if (fRefreshingInProgress) {
			return;
		}
		// check if in synch
		if (fManifestFile == null || (fManifestFile.getModificationStamp() != fPackageTimeStamp)) {
			try {
				modified();
				fRefreshingInProgress = true;
				// set all existing packages to PRIVATE (could clear
				// the map, but it would be less efficient)
				Iterator iterator = fPackageMap.values().iterator();
				while (iterator.hasNext()) {
					PackageNode node = (PackageNode) iterator.next();
					node.visibility = VisibilityModifiers.PRIVATE;
				}
				fManifestFile = getJavaProject().getProject().getFile(JarFile.MANIFEST_NAME);
				if (fManifestFile.exists()) {
					try {
						IPackageFragment[] fragments = getLocalPackageFragments();
						Set names = new HashSet();
						for (int i = 0; i < fragments.length; i++) {
							names.add(fragments[i].getElementName());
						}
						BundleApiComponent.initializeApiDescription(this, fBundle, names);
						fPackageTimeStamp = fManifestFile.getModificationStamp();
					} catch (CoreException e) {
						ApiPlugin.log(e.getStatus());
					}
				}
			} finally {
				fRefreshingInProgress = false;
			}
		}
	}

	private IElementDescriptor getElementDescriptor(IJavaElement element) {
		switch (element.getElementType()) {
			case IJavaElement.PACKAGE_FRAGMENT:
				return Factory.packageDescriptor(element.getElementName());
			case IJavaElement.TYPE:
				return Factory.typeDescriptor(((IType)element).getFullyQualifiedName('$'));
			default:
				return null;
		}
	}
	
	/**
	 * Returns the Java project associated with this component.
	 * 
	 * @return associated Java project
	 */
	private IJavaProject getJavaProject() {
		return fProject;
	}

	/**
	 * Returns a class file container for the given package fragment root, or <code>null</code>
	 * if none.
	 * 
	 * @param root package fragment root
	 * @return class file container or <code>null</code> if none
	 */
	private synchronized IClassFileContainer getClassFileContainer(IPackageFragmentRoot root) throws CoreException {
		if (fClassFileContainers == null) {
			fClassFileContainers = new HashMap(8);
		}
		IPath location = root.getRawClasspathEntry().getOutputLocation();
		if (location == null) {
			location = root.getJavaProject().getOutputLocation();
		}
		IClassFileContainer container = (IClassFileContainer) fClassFileContainers.get(location);
		if (container == null) {
			IContainer folder = root.getJavaProject().getProject().getWorkspace().getRoot().getFolder(location);
			if (folder.exists()) {
				container = new FolderClassFileContainer(folder, getJavaProject().getElementName());
				fClassFileContainers.put(location, container);
			}
		}
		return container;
	}
	
	/**
	 * Returns all package fragments that originate from this project.
	 * 
	 * @return all package fragments that originate from this project
	 */
	private IPackageFragment[] getLocalPackageFragments() {
		List local = new ArrayList();
		try {
			IPackageFragmentRoot[] roots = getJavaProject().getPackageFragmentRoots();
			for (int i = 0; i < roots.length; i++) {
				IPackageFragmentRoot root = roots[i];
				// only care about roots originating from this project (binary or source)
				IResource resource = root.getCorrespondingResource();
				if (resource != null && resource.getProject().equals(getJavaProject().getProject())) {
					IJavaElement[] children = root.getChildren();
					for (int j = 0; j < children.length; j++) {
						local.add(children[j]);
					}
				}
			}
		} catch (JavaModelException e) {
			// ignore
		}
		return (IPackageFragment[]) local.toArray(new IPackageFragment[local.size()]);
	}
	
	/**
	 * Connects this API description to the given bundle.
	 * 
	 * @param bundle bundle description
	 */
	synchronized void connect(BundleDescription bundle) {
		if (fBundle != null && fBundle != bundle) {
			throw new IllegalStateException("Already connected to a bundle"); //$NON-NLS-1$
		}
		fBundle = bundle;
	}
	
	/**
	 * Returns the bundle this description is connected to or <code>null</code>
	 * 
	 * @return connected bundle or <code>null</code>
	 */
	BundleDescription getConnection() {
		return fBundle;
	}
	
	/**
	 * Disconnects this API description from the given bundle.
	 * 
	 * @param bundle bundle description
	 */
	synchronized void disconnect(BundleDescription bundle) {
		if (bundle.equals(fBundle)) {
			fBundle = null;
		} else if (fBundle != null) {
			throw new IllegalStateException("Not connected to same bundle"); //$NON-NLS-1$
		}
	}
	
	/**
	 * Returns this API description as XML.
	 * 
	 * @throws CoreException
	 */
	synchronized String getXML() throws CoreException {
		Document document = Util.newDocument();	
		Element component = document.createElement(IApiXmlConstants.ELEMENT_COMPONENT);
		component.setAttribute(IApiXmlConstants.ATTR_ID, getJavaProject().getElementName());
		component.setAttribute(IApiXmlConstants.ATTR_MODIFICATION_STAMP, Long.toString(fPackageTimeStamp));
		component.setAttribute(IApiXmlConstants.ATTR_VERSION, IApiXmlConstants.API_DESCRIPTION_CURRENT_VERSION);
		document.appendChild(component);
		persistChildren(document, component, fPackageMap);
		return Util.serializeDocument(document);
	}

	/**
	 * Persists the elements in the given map as XML elements, appended
	 * to the given xmlElement.
	 *  
	 * @param document XML document
	 * @param xmlElement node to append children no
	 * @param elementMap elements to persist
	 */
	void persistChildren(Document document, Element xmlElement, Map elementMap) {
		Iterator iterator = elementMap.values().iterator();
		while (iterator.hasNext()) {
			ManifestNode node = (ManifestNode) iterator.next();
			node.persistXML(document, xmlElement);
		}
	}
	
	/**
	 * Cleans this API description so it will be re-populated with fresh data.
	 */
	synchronized void clean() {
		fPackageMap.clear();
		fPackageTimeStamp = -1L;
		fInSynch = false;
		modified();
	}
	
	/**
	 * Notes that the underlying project has changed in some way and that the
	 * description cache is no longer in synch with the project.
	 */
	synchronized void projectChanged() {
		fInSynch = false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.ApiDescription#toString()
	 */
	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("Project API description for: ").append(getJavaProject().getElementName()); //$NON-NLS-1$
		return buffer.toString();
	}
}
