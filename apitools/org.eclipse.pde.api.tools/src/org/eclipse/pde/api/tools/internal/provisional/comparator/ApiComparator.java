/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal.provisional.comparator;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.pde.api.tools.internal.comparator.ClassFileComparator;
import org.eclipse.pde.api.tools.internal.comparator.Delta;
import org.eclipse.pde.api.tools.internal.comparator.TypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.ClassFileContainerVisitor;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IClassFile;
import org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.osgi.framework.Version;

/**
 * This class defines a comparator to get a IDelta out of the comparison of two elements.
 *
 * @since 1.0
 */
public class ApiComparator {
	public static final IDelta NO_DELTA = new Delta();

	/**
	 * Constant used for controlling tracing in the API comparator
	 */
	private static boolean DEBUG = Util.DEBUG;
	
	/**
	 * Method used for initializing tracing in the API comparator
	 */
	public static void setDebug(boolean debugValue) {
		DEBUG = debugValue || Util.DEBUG;
	}

	/**
	 * Returns a delta that corresponds to the comparison of the given class file with the reference. 
	 * 
	 * @param classFile2 the given class file that comes from the <code>component2</code>
	 * @param component the given API component from the reference
	 * @param component2 the given API component to compare with
	 * @param referenceProfile the given API profile from which the given component <code>component</code> is coming from
	 * @param profile the given API profile from which the given component <code>component2</code> is coming from
	 * @param visibilityModifiers the given visibility that triggers what visibility should be used for the comparison
	 *
	 * @return a delta, an empty delta if no difference is found or null if the delta detection failed
	 * @exception IllegalArgumentException if:<ul>
	 * <li>the given class file is null</li>
	 * <li>one of the given components is null</li>
	 * <li>one of the given profiles is null</li>
	 * </ul>
	 */
	public static IDelta compare(
			final IClassFile classFile2,
			final IApiComponent component,
			final IApiComponent component2,
			final IApiBaseline referenceProfile,
			final IApiBaseline profile,
			final int visibilityModifiers) {
		
		if (classFile2 == null) {
			throw new IllegalArgumentException("The given class file is null"); //$NON-NLS-1$
		}
		if (component == null || component2 == null) {
			throw new IllegalArgumentException("One of the given components is null"); //$NON-NLS-1$
		}
		if (referenceProfile == null || profile == null) {
			throw new IllegalArgumentException("One of the given profiles is null"); //$NON-NLS-1$
		}

		try {
			TypeDescriptor typeDescriptor2 = new TypeDescriptor(classFile2);
			if (typeDescriptor2.isNestedType()) {
				// we skip nested types (member, local and anonymous)
				return NO_DELTA;
			}
			String typeName = classFile2.getTypeName();
			IClassFile classFile = component.findClassFile(typeName);
			final IApiDescription apiDescription2 = component2.getApiDescription();
			IApiAnnotations elementDescription2 = apiDescription2.resolveAnnotations(Factory.typeDescriptor(typeName));
			int visibility = 0;
			if (elementDescription2 != null) {
				visibility = elementDescription2.getVisibility();
			}
			final IApiDescription referenceApiDescription = component.getApiDescription();
			IApiAnnotations refElementDescription = referenceApiDescription.resolveAnnotations(Factory.typeDescriptor(typeName));
			int refVisibility = 0;
			if (refElementDescription != null) {
				refVisibility = refElementDescription.getVisibility();
			}
			String deltaComponentID = Util.getDeltaComponentID(component2);
			if (classFile == null) {
				if (isAPI(visibility, typeDescriptor2)) {
					return new Delta(
							deltaComponentID,
							IDelta.API_COMPONENT_ELEMENT_TYPE,
							IDelta.ADDED,
							IDelta.TYPE,
							elementDescription2 != null ? elementDescription2.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS,
							typeDescriptor2.access,
							typeName,
							typeName,
							new String[] { typeName, deltaComponentID});
				}
				return NO_DELTA;
			}
			TypeDescriptor typeDescriptor = new TypeDescriptor(classFile);
			if ((visibility & visibilityModifiers) == 0) {
				if ((refVisibility & visibilityModifiers) == 0) {
					// no delta
					return NO_DELTA;
				}
				if (isAPI(refVisibility, typeDescriptor)) {
					return new Delta(
							deltaComponentID,
							IDelta.API_COMPONENT_ELEMENT_TYPE,
							IDelta.REMOVED,
							IDelta.API_TYPE,
							elementDescription2 != null ? elementDescription2.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS,
							typeDescriptor2.access,
							typeName,
							typeName,
							new String[] { typeName, deltaComponentID});
				}
			} else if (!isAPI(refVisibility, typeDescriptor)
					&& isAPI(visibility, typeDescriptor2)) {
				return new Delta(
						deltaComponentID,
						IDelta.API_COMPONENT_ELEMENT_TYPE,
						IDelta.ADDED,
						IDelta.TYPE,
						elementDescription2 != null ? elementDescription2.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS,
						typeDescriptor2.access,
						typeName,
						typeName,
						new String[] { typeName, deltaComponentID});
			}
			if (visibilityModifiers == VisibilityModifiers.API) {
				// if the visibility is API, we only consider public and protected types
				if (Util.isDefault(typeDescriptor2.access)
							|| Util.isPrivate(typeDescriptor2.access)) {
					// we need to check if the reference contains the type to report a reduced visibility
					if (Util.isPublic(typeDescriptor.access)
							|| Util.isProtected(typeDescriptor.access)) {
						return new Delta(
							deltaComponentID,
							IDelta.API_COMPONENT_ELEMENT_TYPE,
							IDelta.REMOVED,
							IDelta.API_TYPE,
							elementDescription2 != null ? elementDescription2.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS,
							typeDescriptor2.access,
							typeName,
							typeName,
							new String[] { typeName, deltaComponentID});
					} else {
						return NO_DELTA;
					}
				}
			}
			ClassFileComparator comparator = new ClassFileComparator(typeDescriptor, classFile2, component, component2, referenceProfile, profile, visibilityModifiers);
			IDelta delta = comparator.getDelta();
			IStatus status = comparator.getStatus();
			if(status != null) {
				ApiPlugin.log(status);
			}
			return delta;
		} catch (CoreException e) {
			return null;
		}
	}

	private static boolean isAPI(int visibility,
			TypeDescriptor typeDescriptor) {
		int access = typeDescriptor.access;
		return (visibility & VisibilityModifiers.API) != 0
			&& (Util.isPublic(access) || Util.isProtected(access));
	}

	/**
	 * Returns a delta that corresponds to the comparison of the given class file. 
	 * 
	 * @param classFile the given class file
	 * @param classFile2 the given class file to compare with
	 * @param component the given API component from which the given class file is coming from
	 * @param component2 the given API component to compare with
	 * @param referenceProfile the given API profile from which the given component <code>component</code> is coming from
	 * @param profile the given API profile from which the given component <code>component2</code> is coming from
	 * @param visibilityModifiers the given visibility that triggers what visibility should be used for the comparison
	 *
	 * @return a delta, an empty delta if no difference is found or <code>null</code> if the delta detection failed
	 * @exception IllegalArgumentException if:<ul>
	 * <li>one of the given components is null</li>
	 * <li>one of the given profiles is null</li>
	 * </ul>
	 */
	public static IDelta compare(
			final IClassFile classFile,
			final IClassFile classFile2,
			final IApiComponent component,
			final IApiComponent component2,
			final IApiBaseline referenceProfile,
			final IApiBaseline profile,
			final int visibilityModifiers) {
		
		if (classFile == null || classFile2 == null) {
			throw new IllegalArgumentException("One of the given class files is null"); //$NON-NLS-1$
		}
		if (component == null || component2 == null) {
			throw new IllegalArgumentException("One of the given components is null"); //$NON-NLS-1$
		}
		if (referenceProfile == null || profile == null) {
			throw new IllegalArgumentException("One of the given profiles is null"); //$NON-NLS-1$
		}
		IDelta delta = null;
		try {
			ClassFileComparator comparator =
				new ClassFileComparator(
						classFile,
						classFile2,
						component,
						component2,
						referenceProfile,
						profile,
						visibilityModifiers);
			delta = comparator.getDelta();
			IStatus status = comparator.getStatus();
			if(status != null) {
				ApiPlugin.log(status);
			}
		}
		catch(CoreException e) {
			ApiPlugin.log(e);
		}
		return delta;
	}

	/**
	 * Returns a delta that corresponds to the comparison of the two given API profiles. 
	 * 
	 * @param referenceProfile the given API profile which is the reference
	 * @param profile the given API profile to compare with
	 * @param force a flag to force the comparison of nested API components with the same versions 
	 *
	 * @return a delta, an empty delta if no difference is found or null if the delta detection failed
	 * @throws IllegalArgumentException if one of the two profiles is null
	 */
	public static IDelta compare(
			final IApiBaseline referenceProfile,
			final IApiBaseline profile,
			final boolean force) {
		return compare(referenceProfile, profile, VisibilityModifiers.ALL_VISIBILITIES, force);
	}

	/**
	 * Returns a delta that corresponds to the comparison of the two given API profiles.
	 * Nested API components with the same versions are not compared.
	 * <p>Equivalent to: compare(profile, profile2, false);</p>
	 * 
	 * @param referenceProfile the given API profile which is the reference
	 * @param profile the given API profile to compare with
	 *
	 * @return a delta, an empty delta if no difference is found or null if the delta detection failed
	 * @throws IllegalArgumentException if one of the two profiles is null
	 */
	public static IDelta compare(
			final IApiBaseline referenceProfile,
			final IApiBaseline profile) {
		return compare(referenceProfile, profile, VisibilityModifiers.ALL_VISIBILITIES, false);
	}

	/**
	 * Returns a delta that corresponds to the comparison of the two given API profiles. 
	 * Nested API components with the same versions are not compared.
	 * <p>Equivalent to: compare(profile, profile2, visibilityModifiers, false);</p>
	 * 
	 * @param referenceProfile the given API profile which is the reference
	 * @param profile the given API profile to compare with
	 * @param visibilityModifiers the given visibility that triggers what visibility should be used for the comparison
	 *
	 * @return a delta, an empty delta if no difference is found or null if the delta detection failed
	 * @throws IllegalArgumentException if one of the two profiles is null
	 */
	public static IDelta compare(
			final IApiBaseline referenceProfile,
			final IApiBaseline profile,
			final int visibilityModifiers) {
		return compare(referenceProfile, profile, visibilityModifiers, false);
	}

	/**
	 * Returns a delta that corresponds to the difference between the given profile and the reference.
	 * 
	 * @param referenceProfile the given API profile which is used as the reference
	 * @param profile the given API profile to compare with
	 * @param visibilityModifiers the given visibility that triggers what visibility should be used for the comparison
	 * @param force a flag to force the comparison of nested API components with the same versions 
	 *
	 * @return a delta, an empty delta if no difference is found or null if the delta detection failed
	 * @throws IllegalArgumentException if one of the two profiles is null
	 */
	public static IDelta compare(
			final IApiBaseline referenceProfile,
			final IApiBaseline profile,
			final int visibilityModifiers,
			final boolean force) {
		if (referenceProfile == null || profile == null) {
			throw new IllegalArgumentException("None of the profiles must be null"); //$NON-NLS-1$
		}
		IApiComponent[] apiComponents = referenceProfile.getApiComponents();
		IApiComponent[] apiComponents2 = profile.getApiComponents();
		Set apiComponentsIds = new HashSet();
		final Delta globalDelta = new Delta();
		for (int i = 0, max = apiComponents.length; i < max; i++) {
			IApiComponent apiComponent = apiComponents[i];
			if (!apiComponent.isSystemComponent()) {
				String id = apiComponent.getId();
				IApiComponent apiComponent2 = profile.getApiComponent(id);
				IDelta delta = null;
				if (apiComponent2 == null) {
					// report removal of an API component
					delta =
						new Delta(
								null,
								IDelta.API_PROFILE_ELEMENT_TYPE,
								IDelta.REMOVED,
								IDelta.API_COMPONENT,
								null,
								id,
								id);
				} else {
					apiComponentsIds.add(id);
					String versionString = apiComponent.getVersion();
					String versionString2 = apiComponent2.getVersion();
					checkBundleVersionChanges(apiComponent2, id, versionString, versionString2, globalDelta);
					if (!versionString.equals(versionString2)
							|| force) {
						long time = System.currentTimeMillis();
						try {
							delta = compare(apiComponent, apiComponent2, referenceProfile, profile, visibilityModifiers);
						} finally {
							if (DEBUG) {
								System.out.println("Time spent for " + id+ " " + versionString + " : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
							}
						}
					}
				}
				if (delta != null && delta != NO_DELTA) {
					globalDelta.add(delta);
				}
			}
		}
		for (int i = 0, max = apiComponents2.length; i < max; i++) {
			IApiComponent apiComponent = apiComponents2[i];
			if (!apiComponent.isSystemComponent()) {
				String id = apiComponent.getId();
				if (!apiComponentsIds.contains(id)) {
					// addition of an API component
					globalDelta.add(
							new Delta(
									null,
									IDelta.API_PROFILE_ELEMENT_TYPE,
									IDelta.ADDED,
									IDelta.API_COMPONENT,
									null,
									id,
									id));
				}
			}
		}
		return globalDelta.isEmpty() ? NO_DELTA : globalDelta;
	}

	private static void checkBundleVersionChanges(IApiComponent apiComponent2, String id, String apiComponentVersion, String apiComponentVersion2, Delta globalDelta) {
		Version version = null;
		try {
			version = new Version(apiComponentVersion);
		} catch (IllegalArgumentException e) {
			// ignore
		}
		Version version2 = null;
		try {
			version2 = new Version(apiComponentVersion2);
		} catch (IllegalArgumentException e) {
			// ignore
		}
		if (version != null && version2 != null) {
			// add check for bundle versions
			if (version.getMajor() != version2.getMajor()) {
				globalDelta.add(
					new Delta(
						Util.getDeltaComponentID(apiComponent2),
						IDelta.API_COMPONENT_ELEMENT_TYPE,
						IDelta.CHANGED,
						IDelta.MAJOR_VERSION,
						RestrictionModifiers.NO_RESTRICTIONS,
						0,
						null,
						id,
						new String[] {
							id,
							apiComponentVersion,
							apiComponentVersion2
						}));
			} else if (version.getMinor() != version2.getMinor()) {
				globalDelta.add(
					new Delta(
						Util.getDeltaComponentID(apiComponent2),
						IDelta.API_COMPONENT_ELEMENT_TYPE,
						IDelta.CHANGED,
						IDelta.MINOR_VERSION,
						RestrictionModifiers.NO_RESTRICTIONS,
						0,
						null,
						id,
						new String[] {
							id,
							apiComponentVersion,
							apiComponentVersion2
						}));
			}
		}
	}
	/**
	 * Returns a delta that corresponds to the difference between the given component and the reference profile.
	 * 
	 * @param component the given component to compare with the given reference profile
	 * @param referenceProfile the given API profile which is used as the reference
	 * @param visibilityModifiers the given visibility that triggers what visibility should be used for the comparison
	 * @param force a flag to force the comparison of nested API components with the same versions 
	 *
	 * @return a delta, an empty delta if no difference is found or null if the delta detection failed
	 * @exception IllegalArgumentException if:<ul>
	 * <li>the given component is null</li>
	 * <li>the reference profile is null</li>
	 * </ul>
	 */
	public static IDelta compare(
			final IApiComponent component,
			final IApiBaseline referenceProfile,
			final int visibilityModifiers,
			final boolean force) {
		
		if (component == null) {
			throw new IllegalArgumentException("The composent cannot be null"); //$NON-NLS-1$
		}
		if (referenceProfile == null) {
			throw new IllegalArgumentException("The reference profile cannot be null"); //$NON-NLS-1$
		}
		IDelta delta = null;
		if (!component.isSystemComponent()) {
			String id = component.getId();
			IApiComponent apiComponent2 = referenceProfile.getApiComponent(id);
			if (apiComponent2 == null) {
				// report addition of an API component
				delta =
					new Delta(
						null,
						IDelta.API_PROFILE_ELEMENT_TYPE,
						IDelta.ADDED,
						IDelta.API_COMPONENT,
						null,
						id,
						id);
			} else {
				if (!component.getVersion().equals(apiComponent2.getVersion())
						|| force) {
					long time = System.currentTimeMillis();
					try {
						delta = compare(apiComponent2, component, visibilityModifiers);
					} finally {
						if (DEBUG) {
							System.out.println("Time spent for " + id+ " " + component.getVersion() + " : " + (System.currentTimeMillis() - time) + "ms"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
						}
					}
				}
			}
			if (delta != null && delta != NO_DELTA) {
				return delta;
			}
		}
		return NO_DELTA;
	}

	/**
	 * Returns a delta that corresponds to the difference between the given component and the given reference component.
	 * The given component cannot be null.
	 * 
	 * @param referenceComponent the given API component that is used as the reference
	 * @param component the given component to compare with
	 * @param visibilityModifiers the given visibility that triggers what visibility should be used for the comparison
	 * @param force a flag to force the comparison of nested API components with the same versions 
	 *
	 * @return a delta, an empty delta if no difference is found or null if the delta detection failed
	 * @exception IllegalArgumentException if:<ul>
	 * <li>one of the given components is null</li>
	 * </ul>
	 */
	public static IDelta compare(
			final IApiComponent referenceComponent,
			final IApiComponent component,
			final int visibilityModifiers) {

		if (referenceComponent == null || component == null) {
			throw new IllegalArgumentException("One of the given components is null"); //$NON-NLS-1$
		}
		return compare(referenceComponent, component, referenceComponent.getProfile(), component.getProfile(), visibilityModifiers);
	}
	/**
	 * Returns a delta that corresponds to the comparison of the two given API components.
	 * The two components are compared even if their versions are identical.
	 * 
	 * @param referenceComponent the given API component from which the given class file is coming from
	 * @param component2 the given API component to compare with
	 * @param referenceProfile the given API profile from which the given component <code>component</code> is coming from
	 * @param profile the given API profile from which the given component <code>component2</code> is coming from
	 * @param visibilityModifiers the given visibility that triggers what visibility should be used for the comparison
	 *
	 * @return a delta, an empty delta if no difference is found or null if the delta detection failed
	 * @exception IllegalArgumentException if:<ul>
	 * <li>both given components are null</li>
	 * <li>one of the profiles is null</li>
	 * </ul>
	 */
	public static IDelta compare(
			final IApiComponent referenceComponent,
			final IApiComponent component2,
			final IApiBaseline referenceProfile,
			final IApiBaseline profile) {
		return compare(referenceComponent, component2, referenceProfile, profile, VisibilityModifiers.ALL_VISIBILITIES);
	}

	/**
	 * Returns a delta that corresponds to the comparison of the two given API components.
	 * The two components are compared even if their versions are identical.
	 * 
	 * @param referenceComponent the given API component
	 * @param component2 the given API component to compare with
	 * @param referenceProfile the given API profile from which the given component <code>component</code> is coming from
	 * @param profile the given API profile from which the given component <code>component2</code> is coming from
	 * @param visibilityModifiers the given visibility that triggers what visibility should be used for the comparison
	 *
	 * @return a delta, an empty delta if no difference is found or null if the delta detection failed
	 * @exception IllegalArgumentException if:<ul>
	 * <li>both given components are null</li>
	 * <li>one of the profiles is null</li>
	 * </ul>
	 */
	public static IDelta compare(
			final IApiComponent referenceComponent,
			final IApiComponent component2,
			final IApiBaseline referenceProfile,
			final IApiBaseline profile,
			final int visibilityModifiers) {
	
		if (referenceProfile == null || profile == null) {
			throw new IllegalArgumentException("The profiles cannot be null"); //$NON-NLS-1$
		}
		if (referenceComponent == null) {
			if (component2 == null) {
				throw new IllegalArgumentException("Both components cannot be null"); //$NON-NLS-1$
			}
			return new Delta(
					null,
					IDelta.API_PROFILE_ELEMENT_TYPE,
					IDelta.ADDED,
					IDelta.API_COMPONENT,
					null,
					component2.getId(),
					Util.getDeltaComponentID(component2));
		} else if (component2 == null) {
			String referenceComponentId = referenceComponent.getId();
			return new Delta(
					null,
					IDelta.API_PROFILE_ELEMENT_TYPE,
					IDelta.REMOVED,
					IDelta.API_COMPONENT,
					null,
					referenceComponentId,
					Util.getDeltaComponentID(referenceComponent));
		}
		String referenceComponentId = referenceComponent.getId();
		final Delta globalDelta = new Delta();

		// check the EE first
		Set referenceEEs = Util.convertAsSet(referenceComponent.getExecutionEnvironments());
		Set componentsEEs = Util.convertAsSet(component2.getExecutionEnvironments());
		
		for (Iterator iterator = referenceEEs.iterator(); iterator.hasNext(); ) {
			String currentEE = (String) iterator.next();
			if (!componentsEEs.remove(currentEE)) {
				globalDelta.add(
						new Delta(
								Util.getDeltaComponentID(referenceComponent),
								IDelta.API_COMPONENT_ELEMENT_TYPE,
								IDelta.REMOVED,
								IDelta.EXECUTION_ENVIRONMENT,
								RestrictionModifiers.NO_RESTRICTIONS,
								0,
								null,
								referenceComponentId,
								new String[] { currentEE, Util.getDeltaComponentID(referenceComponent)}));
			}
		}
		for (Iterator iterator = componentsEEs.iterator(); iterator.hasNext(); ) {
			String currentEE = (String) iterator.next();
			globalDelta.add(
					new Delta(
							Util.getDeltaComponentID(referenceComponent),
							IDelta.API_COMPONENT_ELEMENT_TYPE,
							IDelta.ADDED,
							IDelta.EXECUTION_ENVIRONMENT,
							RestrictionModifiers.NO_RESTRICTIONS,
							0,
							null,
							referenceComponentId,
							new String[] { currentEE, Util.getDeltaComponentID(referenceComponent)}));
		}
		try {
			return internalCompare(referenceComponent, component2, referenceProfile, profile, visibilityModifiers, globalDelta);
		} catch(CoreException e) {
			// null means an error case
			return null;
		}
	}

	private static IDelta internalCompare(
			final IApiComponent component,
			final IApiComponent component2,
			final IApiBaseline referenceProfile,
			final IApiBaseline profile,
			final int visibilityModifiers,
			final Delta globalDelta) throws CoreException {

		final Set classFileBaseLineNames = new HashSet();
		final String id = component.getId();
		IClassFileContainer[] classFileContainers = null;
		IClassFileContainer[] classFileContainers2 = null;
		
		final boolean isSWT = "org.eclipse.swt".equals(id); //$NON-NLS-1$
		if (isSWT) {
			classFileContainers = component.getClassFileContainers();
			classFileContainers2 = component2.getClassFileContainers();
		} else {
			classFileContainers = component.getClassFileContainers(id);
			classFileContainers2 = component2.getClassFileContainers(id);
		}
		final IApiDescription apiDescription = component.getApiDescription();
		final IApiDescription apiDescription2 = component2.getApiDescription();

		if (classFileContainers != null) {
			for (int i = 0, max = classFileContainers.length; i < max; i++) {
				IClassFileContainer container = classFileContainers[i];
				try {
					container.accept(new ClassFileContainerVisitor() {
						public void visit(String packageName, IClassFile classFile) {
							String typeName = classFile.getTypeName();
							IApiAnnotations elementDescription = apiDescription.resolveAnnotations(Factory.typeDescriptor(typeName));
							try {
								TypeDescriptor typeDescriptor = new TypeDescriptor(classFile);
								if (typeDescriptor.isNestedType()) {
									// we skip nested types (member, local and anonymous)
									return;
								}
								int visibility = 0;
								if (elementDescription != null) {
									visibility = elementDescription.getVisibility();
								}
								IClassFile classFile2 = null;
								if (isSWT) {
									classFile2 = component2.findClassFile(typeName);
								} else{
									classFile2 = component2.findClassFile(typeName, id);
								}
								String deltaComponentID = Util.getDeltaComponentID(component2);
								if(classFile2 == null) {
									if ((visibility & visibilityModifiers) == 0) {
										// we skip the class file according to their visibility
										return;
									}
									if (visibilityModifiers == VisibilityModifiers.API) {
										// if the visibility is API, we only consider public and protected types
										if (Util.isDefault(typeDescriptor.access)
													|| Util.isPrivate(typeDescriptor.access)) {
											return;
										}
									}
									globalDelta.add(
											new Delta(
													deltaComponentID,
													IDelta.API_COMPONENT_ELEMENT_TYPE,
													IDelta.REMOVED,
													IDelta.TYPE,
													RestrictionModifiers.NO_RESTRICTIONS,
													typeDescriptor.access,
													typeName,
													typeName,
													new String[] { typeName, deltaComponentID}));
								} else {
									if ((visibility & visibilityModifiers) == 0) {
										// we skip the class file according to their visibility
										return;
									}
									TypeDescriptor typeDescriptor2 = new TypeDescriptor(classFile2);
									IApiAnnotations elementDescription2 = apiDescription2.resolveAnnotations(Factory.typeDescriptor(typeName));
									int visibility2 = 0;
									if (elementDescription2 != null) {
										visibility2 = elementDescription2.getVisibility();
									}
									if (visibilityModifiers == VisibilityModifiers.API) {
										// if the visibility is API, we only consider public and protected types
										if (Util.isDefault(typeDescriptor.access)
												|| Util.isPrivate(typeDescriptor.access)) {
											return;
										}
									}
									if (isAPI(visibility, typeDescriptor) && !isAPI(visibility2, typeDescriptor2)) {
										globalDelta.add(
												new Delta(
														deltaComponentID,
														IDelta.API_COMPONENT_ELEMENT_TYPE,
														IDelta.REMOVED,
														IDelta.API_TYPE,
														elementDescription2 != null ? elementDescription2.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS,
														typeDescriptor2.access,
														typeName,
														typeName,
														new String[] { typeName, deltaComponentID}));
										return;
									}
									if ((visibility2 & visibilityModifiers) == 0) {
										// we simply report a changed visibility
										globalDelta.add(
												new Delta(
														deltaComponentID,
														IDelta.API_COMPONENT_ELEMENT_TYPE,
														IDelta.CHANGED,
														IDelta.TYPE_VISIBILITY,
														elementDescription2 != null ? elementDescription2.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS,
														typeDescriptor2.access,
														typeName,
														typeName,
														new String[] { typeName, deltaComponentID}));
									}
									classFileBaseLineNames.add(typeName);
									ClassFileComparator comparator = new ClassFileComparator(typeDescriptor, classFile2, component, component2, referenceProfile, profile, visibilityModifiers);
									IDelta delta = comparator.getDelta();
									IStatus status = comparator.getStatus();
									if(status != null) {
										ApiPlugin.log(status);
									}
									if (delta != null && delta != NO_DELTA) {
										globalDelta.add(delta);
									}
								}
							} catch (CoreException e) {
								ApiPlugin.log(e);
							}
						}
					});
				} catch (CoreException e) {
					ApiPlugin.log(e);
				}
			}
		}
		if (classFileContainers2 != null) {
			for (int i = 0, max = classFileContainers2.length; i < max; i++) {
				IClassFileContainer container = classFileContainers2[i];
				try {
					container.accept(new ClassFileContainerVisitor() {
						public void visit(String packageName, IClassFile classFile) {
							String typeName = classFile.getTypeName();
							IApiAnnotations elementDescription = apiDescription2.resolveAnnotations(Factory.typeDescriptor(typeName));
							try {
								TypeDescriptor typeDescriptor = new TypeDescriptor(classFile);
								if (typeDescriptor.isNestedType()) {
									// we skip nested types (member, local and anonymous)
									return;
								}
								if (filterType(visibilityModifiers, elementDescription, typeDescriptor)) {
									return;
								}
								if (classFileBaseLineNames.contains(typeName)) {
									// already processed
									return;
								}
								classFileBaseLineNames.add(typeName);
								String deltaComponentID = Util.getDeltaComponentID(component2);
								globalDelta.add(
										new Delta(
												deltaComponentID,
												IDelta.API_COMPONENT_ELEMENT_TYPE,
												IDelta.ADDED,
												IDelta.TYPE,
												elementDescription != null ? elementDescription.getRestrictions() : RestrictionModifiers.NO_RESTRICTIONS,
												typeDescriptor.access,
												typeName,
												typeName,
												new String[] { typeName, deltaComponentID}));
							} catch (CoreException e) {
								ApiPlugin.log(e);
							}
						}
					});
				} catch (CoreException e) {
					ApiPlugin.log(e);
				}
			}
		}
		return globalDelta.isEmpty() ? NO_DELTA : globalDelta;
	}
	
	/* (no javadoc)
	 * Returns true, if the given type descriptor should be skipped, false otherwise.
	 */
	static boolean filterType(final int visibilityModifiers,
			IApiAnnotations elementDescription,
			TypeDescriptor typeDescriptor) {
		if (elementDescription != null && (elementDescription.getVisibility() & visibilityModifiers) == 0) {
			// we skip the class file according to their visibility
			return true;
		}
		if (visibilityModifiers == VisibilityModifiers.API) {
			// if the visibility is API, we only consider public and protected types
			if (Util.isDefault(typeDescriptor.access)
						|| Util.isPrivate(typeDescriptor.access)) {
				return true;
			}
		}
		return false;
	}
}
