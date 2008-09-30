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
package org.eclipse.pde.api.tools.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.BundleSpecification;
import org.eclipse.osgi.service.resolver.ExportPackageDescription;
import org.eclipse.osgi.service.resolver.HostSpecification;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.osgi.service.resolver.StateObjectFactory;
import org.eclipse.osgi.util.ManifestElement;
import org.eclipse.pde.api.tools.internal.model.ApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiFilterStore;
import org.eclipse.pde.api.tools.internal.provisional.IClassFileContainer;
import org.eclipse.pde.api.tools.internal.provisional.IRequiredComponentDescription;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.scanner.ApiDescriptionProcessor;
import org.eclipse.pde.api.tools.internal.util.SourceDefaultHandler;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.internal.core.TargetWeaver;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Implementation of an API component based on a bundle in the file system.
 * 
 * @since 1.0.0
 */
public class BundleApiComponent extends AbstractApiComponent {
	
	/**
	 * Dictionary parsed from MANIFEST.MF
	 */
	private Dictionary fManifest;
	
	/**
	 * Whether there is an underlying .api_description file
	 */
	private boolean fHasApiDescription = false;
	
	/**
	 * Root location of component in the file system
	 */
	private String fLocation;
	
	/**
	 * Underlying bundle description (OSGi model of a bundle)
	 */
	private BundleDescription fBundleDescription;

	/**
	 * Constructs a new API component from the specified location in the file system
	 * in the given profile.
	 * 
	 * @param profile owning profile
	 * @param location directory or jar file
	 * @exception CoreException if unable to create a component from the specified location
	 */
	public BundleApiComponent(IApiBaseline profile, String location) throws CoreException {
		super(profile);
		fLocation = location;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.descriptors.AbstractApiComponent#dispose()
	 */
	public void dispose() {
		try {
			super.dispose();
		} finally {
			fManifest = null;
			fBundleDescription = null;
		}
	}
	
	/**
	 * Returns this bundle's manifest as a dictionary.
	 * 
	 * @return manifest dictionary
	 * @exception CoreException if something goes terribly wrong
	 */
	protected synchronized Dictionary getManifest() throws CoreException {
		if(fManifest == null) {
			try {
				fManifest = (Dictionary) loadManifest(new File(fLocation));
			} catch (IOException e) {
				abort("Unable to load manifest due to IO error", e); //$NON-NLS-1$
			}
		}
		return fManifest;
	}

	/**
	 * Returns if the bundle at the specified location is a valid bundle or not.
	 * Validity is determined via the existence of a readable manifest file
	 * @param location
	 * @return true if the bundle at the given location is valid false otherwise
	 * @throws IOException
	 */
	public boolean isValidBundle() throws CoreException {
		Dictionary manifest = getManifest();
		return manifest != null && (manifest.get(Constants.BUNDLE_NAME) != null && manifest.get(Constants.BUNDLE_VERSION) != null);
	}
	
	/**
	 * Initializes component state from the underlying bundle for the given
	 * state.
	 * 
	 * @param state PDE state
	 * @throws CoreException on failure
	 */
	public void init(State state, long bundleId) throws CoreException {
		try {
			Dictionary manifest = getManifest();
			if (isBinaryBundle() && ApiBaselineManager.WORKSPACE_API_PROFILE_ID.equals(getProfile().getName())) {
				// must account for bundles in development mode - look for class files in output
				// folders rather than jars
				TargetWeaver.weaveManifest(manifest);
			}
			StateObjectFactory factory = StateObjectFactory.defaultFactory;
			fBundleDescription = factory.createBundleDescription(state, manifest, fLocation, bundleId);
		} catch (BundleException e) {
			abort("Unable to create API component from specified location: " + fLocation, e); //$NON-NLS-1$
		}
	}
	
	/**
	 * Returns whether this API component represents a binary bundle versus a project bundle.
	 * 
	 * @return whether this API component represents a binary bundle
	 */
	protected boolean isBinaryBundle() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.AbstractApiComponent#createApiDescription()
	 */
	protected IApiDescription createApiDescription() throws CoreException {
		BundleDescription[] fragments = getBundleDescription().getFragments();
		if (fragments.length == 0) {
			return createLocalApiDescription();
		}
		// build a composite description
		IApiDescription[] descriptions = new IApiDescription[fragments.length + 1];
		for (int i = 0; i < fragments.length; i++) {
			BundleDescription fragment = fragments[i];
			BundleApiComponent component = (BundleApiComponent) getProfile().getApiComponent(fragment.getSymbolicName());
			descriptions[i + 1] = component.getApiDescription();
		}
		descriptions[0] = createLocalApiDescription();
		return new CompositeApiDescription(descriptions);
	}

	/**
	 * Creates and returns this component's API description based on packages
	 * supplied by this component, exported packages, and associated directives.
	 * 
	 * @return API description
	 * @throws CoreException if unable to initialize 
	 */
	protected IApiDescription createLocalApiDescription() throws CoreException {
		IApiDescription apiDesc = new ApiDescription(getId());
		// first mark all packages as internal
		initializeApiDescription(apiDesc, getBundleDescription(), getLocalPackageNames());
		try {
			String xml = loadApiDescription(new File(fLocation));
			setHasApiDescription(xml != null);
			if (xml != null) {
				ApiDescriptionProcessor.annotateApiSettings(null, apiDesc, xml);
			}
		} catch (IOException e) {
			abort("Unable to load component.xml", e); //$NON-NLS-1$
		}
		return apiDesc;
	}
	
	/**
	 * Returns the names of all packages that originate from this bundle.
	 * Does not include packages that originate from fragments or a host.
	 * 
	 * @return local package names
	 * @throws CoreException
	 */
	protected Set getLocalPackageNames() throws CoreException {
		Set names = new HashSet();
		IClassFileContainer[] containers = getClassFileContainers();
		for (int i = 0; i < containers.length; i++) {
			if (containers[i].getOrigin().equals(getId())) {
				String[] packageNames = containers[i].getPackageNames();
				for (int j = 0; j < packageNames.length; j++) {
					names.add(packageNames[j]);
				}
			}
		}
		return names;
	}	
	

	/**
	 * Initializes the given API description based on package exports in the manifest.
	 * The API description for a bundle only contains packages that originate from
	 * this bundle (so a host will not contain API descriptions for packages that
	 * originate from fragments). However, a host's API description will be represented
	 * by a proxy that delegates to the host and all of its fragments to provide
	 * a complete description of the host.
	 * 
	 * @param apiDesc API description to initialize
	 * @param bundle the bundle to load from
	 * @param packages the complete set of packages names originating from the backing
	 * 		component
	 * @throws CoreException if an error occurs
	 */
	protected static void initializeApiDescription(IApiDescription apiDesc, BundleDescription bundle, Set packages) throws CoreException {
		Iterator iterator = packages.iterator();
		while (iterator.hasNext()) {
			String name = (String) iterator.next();
			apiDesc.setVisibility(Factory.packageDescriptor(name), VisibilityModifiers.PRIVATE);
		}
		// then process exported packages that originate from this bundle
		// considering host and fragment package exports
		List supplied = new ArrayList();
		ExportPackageDescription[] exportPackages = bundle.getExportPackages();
		addSuppliedPackages(packages, supplied, exportPackages);
		HostSpecification host = bundle.getHost();
		if (host != null) {
			BundleDescription[] hosts = host.getHosts();
			for (int i = 0; i < hosts.length; i++) {
				addSuppliedPackages(packages, supplied, hosts[i].getExportPackages());
			}
		}
		BundleDescription[] fragments = bundle.getFragments();
		for (int i = 0; i < fragments.length; i++) {
			addSuppliedPackages(packages, supplied, fragments[i].getExportPackages());
		}
		
		annotateExportedPackages(apiDesc, (ExportPackageDescription[]) supplied.toArray(new ExportPackageDescription[supplied.size()]));
	}

	/**
	 * Adds package exports to the given list if the associated package originates
	 * from this bundle.
	 *   
	 * @param packages names of packages supplied by this bundle
	 * @param supplied list to append package exports to
	 * @param exportPackages package exports to consider
	 */
	protected static void addSuppliedPackages(Set packages, List supplied, ExportPackageDescription[] exportPackages) {
		for (int i = 0; i < exportPackages.length; i++) {
			ExportPackageDescription pkg = exportPackages[i];
			String name = pkg.getName();
			if (name.equals(".")) { //$NON-NLS-1$
				// translate . to default package
				name = Util.DEFAULT_PACKAGE_NAME;
			}
			if (packages.contains(name)) {
				supplied.add(pkg);
			}
		}
	}
	
	/**
	 * Annotates the API description with exported packages.
	 * 
	 * @param apiDesc description to annotate
	 * @param exportedPackages packages that are exported
	 */
	protected static void annotateExportedPackages(IApiDescription apiDesc, ExportPackageDescription[] exportedPackages) {
		for(int i = 0; i < exportedPackages.length; i++) {
			ExportPackageDescription pkg = exportedPackages[i];
			boolean internal = ((Boolean) pkg.getDirective("x-internal")).booleanValue(); //$NON-NLS-1$
			String[] friends = (String[]) pkg.getDirective("x-friends"); //$NON-NLS-1$
			String pkgName = pkg.getName();
			if (pkgName.equals(".")) { //$NON-NLS-1$
				// default package
				pkgName = ""; //$NON-NLS-1$
			}
			IPackageDescriptor pkgDesc = Factory.packageDescriptor(pkgName);
			if(internal) {
				apiDesc.setVisibility(pkgDesc, VisibilityModifiers.PRIVATE);
			}
			if (friends != null) {
				apiDesc.setVisibility(pkgDesc, VisibilityModifiers.PRIVATE);
			}
			if (!internal && friends == null) {
				//there could have been directives that have nothing to do with
				//visibility, so we need to add the package as API in that case
				apiDesc.setVisibility(pkgDesc, VisibilityModifiers.API);
			}			
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.AbstractApiComponent#createApiFilterStore()
	 */
	protected IApiFilterStore createApiFilterStore() throws CoreException {
		//always return a new empty store since we do not support filtering from bundles
		return null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.AbstractClassFileContainer#createClassFileContainers()
	 */
	protected List createClassFileContainers() throws CoreException {
		List containers = new ArrayList(5);
		try {
			List all = new ArrayList();
			// build the classpath from bundle and all fragments
			all.add(this);
			boolean considerFragments = true;
			if ("org.eclipse.swt".equals(getId())) { //$NON-NLS-1$
				// if SWT is a project to be built/analyzed don't consider its fragments
				considerFragments = !isApiEnabled();
			}
			if (considerFragments) { 
				BundleDescription[] fragments = fBundleDescription.getFragments();
				for (int i = 0; i < fragments.length; i++) {
					BundleDescription fragment = fragments[i];
					BundleApiComponent component = (BundleApiComponent) getProfile().getApiComponent(fragment.getSymbolicName());
					if (component != null) {
						// force initialization of the fragment so we can retrieve its class file containers
						component.getClassFileContainers();
						all.add(component);
					}
				}
			}
			Iterator iterator = all.iterator();
			Set entryNames = new HashSet(5);
			BundleApiComponent other = null;
			while (iterator.hasNext()) {
				BundleApiComponent component = (BundleApiComponent) iterator.next();
				String[] paths = getClasspathEntries(component.getManifest());
				for (int i = 0; i < paths.length; i++) {
					String path = paths[i];
					// don't re-process the same entry twice (except default entries ".")
					if (!(".".equals(path))) { //$NON-NLS-1$
						if (entryNames.contains(path)) {
							continue;
						}
					}
					IClassFileContainer container = component.createClassFileContainer(path);
					if (container == null) {
						for(Iterator iter = all.iterator(); iter.hasNext();) {
							other = (BundleApiComponent) iter.next();
							if (other != component) {
								container = other.createClassFileContainer(path);
							}
						}
					}
					if (container != null) {
						containers.add(container);
						if (!(".".equals(path))) { //$NON-NLS-1$
							entryNames.add(path);
						}
					}
				}
			}
		} catch (BundleException e) {
			abort("Unable to parse bundle classpath", e); //$NON-NLS-1$
		} catch (IOException e) {
			abort("Unable to initialize class file containers", e); //$NON-NLS-1$
		}
		return containers;
	}
	
	/**
	 * Returns whether this API component is enabled for API analysis by the API builder.
	 * 
	 * @return whether this API component is enabled for API analysis by the API builder.
	 */
	protected boolean isApiEnabled() {
		return false;
	}
	
	/**
	 * Returns classpath entries defined in the given manifest.
	 * 
	 * @param manifest
	 * @return classpath entries as bundle relative paths
	 * @throws BundleException
	 */
	protected String[] getClasspathEntries(Dictionary manifest) throws BundleException {
		ManifestElement[] classpath = ManifestElement.parseHeader(Constants.BUNDLE_CLASSPATH, (String) manifest.get(Constants.BUNDLE_CLASSPATH));
		String elements[] = null;
		if (classpath == null) {
			// default classpath is '.'
			elements = new String[]{"."}; //$NON-NLS-1$
		} else {
			elements = new String[classpath.length];
			for (int i = 0; i < classpath.length; i++) {
				elements[i] = classpath[i].getValue();
			}
		}
		return elements;
	}
	
	/**
	 * Creates and returns a class file container at the specified path in
	 * this bundle, or <code>null</code> if the class file container does not
	 * exist. The path is the name (path) of entries specified by the
	 * <code>Bundle-ClassPath:</code> header.
	 * 
	 * @param path relative path to a class file container in this bundle
	 * @return class file container or <code>null</code>
	 * @exception IOException
	 */
	protected IClassFileContainer createClassFileContainer(String path) throws IOException {
		File bundle = new File(fLocation);
		if (bundle.isDirectory()) {
			// bundle is folder
			File entry = new File(bundle, path);
			if (entry.exists()) {
				if (entry.isFile()) {
					return new ArchiveClassFileContainer(entry.getCanonicalPath(), this.getId());
				} else {
					return new DirectoryClassFileContainer(entry.getCanonicalPath(), this.getId());
				}
			}
		} else {
			// bundle is jar'd
			ZipFile zip = null;
			try {
				if (path.equals(".")) { //$NON-NLS-1$
					return new ArchiveClassFileContainer(fLocation, this.getId());
				} else {
					// TODO: use temporary space from OSGi if in a framework
					zip = new ZipFile(fLocation);
					ZipEntry entry = zip.getEntry(path);
					if (entry != null) {
						InputStream inputStream = null;
						File tempFile;
						FileOutputStream outputStream = null;
						try {
							inputStream = zip.getInputStream(entry);
							tempFile = File.createTempFile("api", "tmp"); //$NON-NLS-1$ //$NON-NLS-2$
							tempFile.deleteOnExit();
							outputStream = new FileOutputStream(tempFile);
							byte[] bytes = new byte[8096];
							while (inputStream.available() > 0) {
								int read = inputStream.read(bytes);
								if (read > 0) {
									outputStream.write(bytes, 0, read);
								}
							}
						} finally {
							if (inputStream != null) {
								try {
									inputStream.close();
								} catch(IOException e) {
									ApiPlugin.log(e);
								}
							}
							if (outputStream != null) {
								try {
									outputStream.close();
								} catch(IOException e) {
									ApiPlugin.log(e);
								}
							}
						}
						return new ArchiveClassFileContainer(tempFile.getCanonicalPath(), this.getId());
					}
				}
			} finally {
				if (zip != null) {
					zip.close();
				}
			}
		}
		return null;
	}
		
	/**
	 * Parses a bunlde's manifest into a dictionary. The bundle may be in a jar
	 * or in a directory at the specified location.
	 * 
	 * @param bundleLocation root location of the bundle
	 * @return bundle manifest dictionary or <code>null</code> if none
	 * @throws IOException if unable to parse
	 */
	protected Map loadManifest(File bundleLocation) throws IOException {
		ZipFile jarFile = null;
		InputStream manifestStream = null;
		String extension = new Path(bundleLocation.getName()).getFileExtension();
		try {
			if (extension != null && extension.equals("jar") && bundleLocation.isFile()) { //$NON-NLS-1$
				jarFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
				ZipEntry manifestEntry = jarFile.getEntry(JarFile.MANIFEST_NAME);
				if (manifestEntry != null) {
					manifestStream = jarFile.getInputStream(manifestEntry);
				}
			} else {
				File file = new File(bundleLocation, JarFile.MANIFEST_NAME);
				if (file.exists())
					manifestStream = new FileInputStream(file);
			}
			if (manifestStream == null) {
				return null;
			}
			return ManifestElement.parseBundleManifest(manifestStream, new Hashtable(10));
		} catch (BundleException e) {
			ApiPlugin.log(e);
		} finally {
			closingZipFileAndStream(manifestStream, jarFile);
		}
		return null;
	}
	
	/**
	 * Reads and returns this bunlde's manifest in a Manifest object.
	 * The bundle may be in a jar or in a directory at the specified location.
	 * 
	 * @param bundleLocation root location of the bundle
	 * @return manifest or <code>null</code> if not present
	 * @throws IOException if unable to parse
	 */
	protected Manifest readManifest(File bundleLocation) throws IOException {
		ZipFile jarFile = null;
		InputStream manifestStream = null;
		try {
			String extension = new Path(bundleLocation.getName()).getFileExtension();
			if (extension != null && extension.equals("jar") && bundleLocation.isFile()) { //$NON-NLS-1$
				jarFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
				ZipEntry manifestEntry = jarFile.getEntry(JarFile.MANIFEST_NAME);
				if (manifestEntry != null) {
					manifestStream = jarFile.getInputStream(manifestEntry);
				}
			} else {
				File file = new File(bundleLocation, JarFile.MANIFEST_NAME);
				if (file.exists())
					manifestStream = new FileInputStream(file);
			}
			if (manifestStream == null) {
				return null;
			}
			return new Manifest(manifestStream);
		} finally {
			closingZipFileAndStream(manifestStream, jarFile);
		}
	}

	void closingZipFileAndStream(InputStream stream, ZipFile jarFile) {
		try {
			if (stream != null) {
				stream.close();
			}
		} catch (IOException e) {
			ApiPlugin.log(e);
		}
		try {
			if (jarFile != null) {
				jarFile.close();
			}
		} catch (IOException e) {
			ApiPlugin.log(e);
		}
	}
	
	/**
	 * Reads and returns the file contents corresponding to the given file name.
	 * The bundle may be in a jar or in a directory at the specified location.
	 * 
	 * @param xmlFileName the given file name
	 * @param bundleLocation the root location of the bundle
	 * @return the file contents or <code>null</code> if not present
	 */
	protected String readFileContents(String xmlFileName, File bundleLocation) {
		ZipFile jarFile = null;
		InputStream stream = null;
		try {
			String extension = new Path(bundleLocation.getName()).getFileExtension();
			if (extension != null && extension.equals("jar") && bundleLocation.isFile()) { //$NON-NLS-1$
				jarFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
				ZipEntry manifestEntry = jarFile.getEntry(xmlFileName);
				if (manifestEntry != null) {
					stream = jarFile.getInputStream(manifestEntry);
				}
			} else {
				File file = new File(bundleLocation, xmlFileName);
				if (file.exists()) {
					stream = new FileInputStream(file);
				}
			}
			if (stream == null) {
				return null;
			}
			return new String(Util.getInputStreamAsCharArray(stream, -1, IApiCoreConstants.UTF_8));
		} catch(IOException e) {
			ApiPlugin.log(e);
		} finally {
			closingZipFileAndStream(stream, jarFile);
		}
		return null;
	}

	/**
	 * Parses a bundle's .api_description XML into a string. The file may be in a jar
	 * or in a directory at the specified location.
	 * 
	 * @param bundleLocation root location of the bundle
	 * @return API description XML as a string or <code>null</code> if none
	 * @throws IOException if unable to parse
	 */
	protected String loadApiDescription(File bundleLocation) throws IOException {
		ZipFile jarFile = null;
		InputStream stream = null;
		String contents = null;
		try {
			String extension = new Path(bundleLocation.getName()).getFileExtension();
			if (extension != null && extension.equals("jar") && bundleLocation.isFile()) { //$NON-NLS-1$
				jarFile = new ZipFile(bundleLocation, ZipFile.OPEN_READ);
				ZipEntry manifestEntry = jarFile.getEntry(IApiCoreConstants.API_DESCRIPTION_XML_NAME);
				if (manifestEntry != null) {
					// new file is present
					stream = jarFile.getInputStream(manifestEntry);
				}
			} else {
				File file = new File(bundleLocation, IApiCoreConstants.API_DESCRIPTION_XML_NAME);
				if (file.exists()) {
					// use new file
					stream = new FileInputStream(file);
				}
			}
			if (stream == null) {
				return null;
			}
			char[] charArray = Util.getInputStreamAsCharArray(stream, -1, IApiCoreConstants.UTF_8);
			contents = new String(charArray);
		} finally {
			closingZipFileAndStream(stream, jarFile);
		}
		return contents;
	}
	
	
	/**
	 * Returns a URL describing a file inside a bundle.
	 * 
	 * @param bundleLocation root location of the bundle. May be a
	 *  directory or a file (jar)
	 * @param filePath bundle relative path to desired file
	 * @return URL to the file
	 * @throws MalformedURLException 
	 */
	protected URL getFileInBundle(File bundleLocation, String filePath) throws MalformedURLException {
		String extension = new Path(bundleLocation.getName()).getFileExtension();
		StringBuffer urlSt = new StringBuffer();
		if (extension != null && extension.equals("jar") && bundleLocation.isFile()) { //$NON-NLS-1$
			urlSt.append("jar:file:"); //$NON-NLS-1$
			urlSt.append(bundleLocation.getAbsolutePath());
			urlSt.append("!/"); //$NON-NLS-1$
			urlSt.append(filePath);
		} else {
			urlSt.append("file:"); //$NON-NLS-1$
			urlSt.append(bundleLocation.getAbsolutePath());
			urlSt.append(File.separatorChar);
			urlSt.append(filePath);
		}	
		return new URL(urlSt.toString());
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.manifest.IApiComponent#getExecutionEnvironments()
	 */
	public String[] getExecutionEnvironments() {
		return fBundleDescription.getExecutionEnvironments();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.manifest.IApiComponent#getId()
	 */
	public String getId() {
		return fBundleDescription.getSymbolicName();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.manifest.IApiComponent#getName()
	 */
	public String getName() throws CoreException {
		return (String)getManifest().get(Constants.BUNDLE_NAME);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.manifest.IApiComponent#getRequiredComponents()
	 */
	public IRequiredComponentDescription[] getRequiredComponents() {
		BundleSpecification[] requiredBundles = fBundleDescription.getRequiredBundles();
		IRequiredComponentDescription[] req = new IRequiredComponentDescription[requiredBundles.length];
		for (int i = 0; i < requiredBundles.length; i++) {
			BundleSpecification bundle = requiredBundles[i];
			req[i] = new RequiredComponentDescription(bundle.getName(),
					new BundleVersionRange(bundle.getVersionRange()),
					bundle.isOptional(),
					bundle.isExported());
		}
		return req;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.manifest.IApiComponent#getVersion()
	 */
	public String getVersion() {
		return fBundleDescription.getVersion().toString();
	}
	
	/**
	 * Returns this component's bundle description.
	 * 
	 * @return bundle description
	 */
	public BundleDescription getBundleDescription() {
		return fBundleDescription;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		if (fBundleDescription != null) {
			return fBundleDescription.toString();
		}
		return super.toString();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiComponent#getLocation()
	 */
	public String getLocation() {
		return fLocation;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.model.component.IApiComponent#isSystemComponent()
	 */
	public boolean isSystemComponent() {
		return false;
	}
	
	/**
	 * Returns a boolean option from the map or the default value if not present.
	 * 
	 * @param options option map
	 * @param optionName option name
	 * @param defaultValue default value for option if not present
	 * @return boolean value
	 */
	protected boolean getBooleanOption(Map options, String optionName, boolean defaultValue) {
		Boolean optionB = (Boolean)options.get(optionName);
		if (optionB != null) {
			return optionB.booleanValue();
		}
		return defaultValue;
	}
	
	/* (non-Javadoc)
	 * @see IApiComponent#isSourceComponent()
	 */
	public boolean isSourceComponent() {
		ManifestElement[] sourceBundle = null;
		try {
			sourceBundle = ManifestElement.parseHeader(IApiCoreConstants.ECLIPSE_SOURCE_BUNDLE, (String) fManifest.get(IApiCoreConstants.ECLIPSE_SOURCE_BUNDLE));
		} catch (BundleException e) {
			// ignore
		}
		if (sourceBundle != null) {
			// this is a source bundle with the new format
			return true;
		}
		// check for the old format
		String pluginXMLContents = readFileContents(IApiCoreConstants.PLUGIN_XML_NAME,new File(getLocation()));
		if (pluginXMLContents != null) {
			if (containsSourceExtensionPoint(pluginXMLContents)) {
				return true;
			}
		}
		// check if it contains a fragment.xml with the appropriate extension point
		pluginXMLContents = readFileContents(IApiCoreConstants.FRAGMENT_XML_NAME,new File(getLocation()));
		if (pluginXMLContents != null) {
			if (containsSourceExtensionPoint(pluginXMLContents)) {
				return true;
			}
		}
		// parse XML contents to find extension points
		return false;
	}

	/**
	 * Check if the given source contains an source extension point.
	 * 
	 * @param pluginXMLContents the given file contents
	 * @return true if it contains a source extension point, false otherwise
	 */
	private boolean containsSourceExtensionPoint(String pluginXMLContents) {
		SAXParserFactory factory = null;
		try {
			factory = SAXParserFactory.newInstance();
		} catch (FactoryConfigurationError e) {
			return false;
		}
		SAXParser saxParser = null;
		try {
			saxParser = factory.newSAXParser();
		} catch (ParserConfigurationException e) {
			// ignore
		} catch (SAXException e) {
			// ignore
		}

		if (saxParser == null) {
			return false;
		}

		// Parse
		InputSource inputSource = new InputSource(new BufferedReader(new StringReader(pluginXMLContents)));
		try {
			SourceDefaultHandler defaultHandler = new SourceDefaultHandler();
			saxParser.parse(inputSource, defaultHandler);
			return defaultHandler.isSource();
		} catch (SAXException e) {
			// ignore
		} catch (IOException e) {
			// ignore
		}
		return false;
	}	

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiComponent#isFragment()
	 */
	public boolean isFragment() {
		return fBundleDescription.getHost() != null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.IApiComponent#hasFragments()
	 */
	public boolean hasFragments() {
		return fBundleDescription.getFragments().length != 0;
	}
	
	public String getOrigin() {
		return this.getId();
	}

	/**
	 * Sets whether this bundle has an underlying API description file.
	 * 
	 * @param hasApiDescription whether this bundle has an underlying API description file
	 */
	protected void setHasApiDescription(boolean hasApiDescription) {
		fHasApiDescription = hasApiDescription;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.api.tools.internal.provisional.IApiComponent#hasApiDescription()
	 */
	public boolean hasApiDescription() {
		// ensure initialized
		try {
			getApiDescription();
		} catch (CoreException e) {
		}
		return fHasApiDescription;
	}
}
