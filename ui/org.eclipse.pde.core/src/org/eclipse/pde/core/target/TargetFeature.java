/*******************************************************************************
 *  Copyright (c) 2000, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core.target;

import java.io.File;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.target.Messages;

/**
 * Describes a single feature in a target definition.
 * 
 * @since 3.8
 */
public class TargetFeature implements IAdaptable {

	private IFeatureModel featureModel;

	/**
	 * Constructs a target feature for a feature on the local filesystem. The 
	 * file may point at the feature.xml or a folder containing the feature.xml.
	 * The feature.xml will be read to collect the information about the feature.
	 * 
	 * @param featureLocation the location of the feature (feature.xml or directory containing it)
	 * @throws CoreException if there is a problem opening the feature.xml
	 */
	public TargetFeature(File featureLocation) throws CoreException {
		initialize(featureLocation);
	}

	/**
	 * Returns the id of this feature or <code>null</code> if no id is set.
	 * 
	 * @return id or <code>null</code>
	 */
	public String getId() {
		if (featureModel == null)
			return null;
		return featureModel.getFeature().getId();
	}

	/**
	 * Returns the version of this feature or <code>null</code> if no version is set.
	 * 
	 * @return version or <code>null</code>
	 */
	public String getVersion() {
		if (featureModel == null)
			return null;
		return featureModel.getFeature().getVersion();
	}

	/**
	 * Returns the string path to the directory containing the feature.xml or 
	 * <code>null</code> if no install location is known.
	 * 
	 * @return install location path or <code>null</code>
	 */
	public String getLocation() {
		if (featureModel == null)
			return null;
		return featureModel.getInstallLocation();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (IFeatureModel.class == adapter) {
			return featureModel;
		}
		return null;
	}

	/**
	 * Initializes the content of this target feature by reading the feature.xml
	 * 
	 * @param file feature.xml or directory containing it
	 */
	private void initialize(File file) throws CoreException {
		if (file == null || !file.exists()) {
			throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.TargetFeature_FileDoesNotExist, file)));
		}
		File featureXML;
		if (ICoreConstants.FEATURE_FILENAME_DESCRIPTOR.equalsIgnoreCase(file.getName())) {
			featureXML = file;
		} else {
			featureXML = new File(file, ICoreConstants.FEATURE_FILENAME_DESCRIPTOR);
			if (!featureXML.exists()) {
				throw new CoreException(new Status(IStatus.ERROR, PDECore.PLUGIN_ID, NLS.bind(Messages.TargetFeature_FileDoesNotExist, featureXML)));
			}
		}
		featureModel = ExternalFeatureModelManager.createModel(featureXML);
	}

}
