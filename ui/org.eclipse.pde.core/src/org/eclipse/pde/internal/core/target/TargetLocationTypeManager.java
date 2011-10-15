/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.util.HashMap;
import java.util.Map;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.target.ITargetLocation;
import org.eclipse.pde.core.target.ITargetLocationFactory;
import org.eclipse.pde.internal.core.PDECore;

/**
 * Keeps a track of the contributed Target Locations and provides helper functions to 
 * access them
 *
 */
public class TargetLocationTypeManager {

	private static final String ATTR_TYPE = "type"; //$NON-NLS-1$
	private static final String ATTR_CAN_UPDATE = "canUpdate"; //$NON-NLS-1$
	private static final String ATTR_LOCFACTORY = "locationFactory"; //$NON-NLS-1$
	private static final String TARGET_LOC_EXTPT = "targetLocations"; //$NON-NLS-1$

	private Map fExtentionMap;
	private Map fFactoryMap;
	private Map fUpdateMap;

	static TargetLocationTypeManager INSTANCE;

	private TargetLocationTypeManager() {
		//singleton
		fExtentionMap = new HashMap(4);
		fFactoryMap = new HashMap(4);
		fUpdateMap = new HashMap(4);
		readExtentions();
	}

	/**
	 * @return the singleton instanceof this manager
	 */
	public static TargetLocationTypeManager getInstance() {
		if (INSTANCE == null) {
			INSTANCE = new TargetLocationTypeManager();
		}
		return INSTANCE;
	}

	public boolean canUpdate(String type) {
		Boolean status = (Boolean) fUpdateMap.get(type);
		if (status == null) {
			return false;
		}
		return status.booleanValue();

	}

	/**
	 * Returns an instance of {@link ITargetLocation} from the factory that supports
	 * the location type.  Will throw a {@link CoreException} if no factory can be
	 * found or the xml is invalid.
	 * 
	 * @param type string identifying the type of target location, see {@link ITargetLocation#getType()}
	 * @param serializedXML xml string containing serialized location, see {@link ITargetLocation#serialize()}
	 * @return an instance of <code>ITargetLocation</code>
	 * @throws CoreException if there is a problem finding a factory or the xml is invalid 
	 */
	public ITargetLocation getTargetLocation(String type, String serializedXML) throws CoreException {
		ITargetLocationFactory factory = getFactory(type);
		if (factory != null) {
			return factory.getTargetLocation(type, serializedXML);
		}
		return null;
	}

	private ITargetLocationFactory getFactory(String type) {
		ITargetLocationFactory factory = (ITargetLocationFactory) fFactoryMap.get(type);
		if (factory == null) {
			IConfigurationElement extension = (IConfigurationElement) fExtentionMap.get(type);
			if (extension != null) {
				factory = (ITargetLocationFactory) createExecutableExtension(extension);
				if (factory != null) {
					fFactoryMap.put(type, factory);
					return factory;
				}
			}
		}
		return factory;
	}

	private Object createExecutableExtension(IConfigurationElement element) {
		try {
			return element.createExecutableExtension(ATTR_LOCFACTORY);
		} catch (CoreException e) {
			return null;
		}
	}

	private void readExtentions() {
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IExtensionPoint point = registry.getExtensionPoint(PDECore.PLUGIN_ID, TARGET_LOC_EXTPT);
		if (point == null)
			return;
		IExtension[] extensions = point.getExtensions();
		for (int i = 0; i < extensions.length; i++) {
			IConfigurationElement[] elements = extensions[i].getConfigurationElements();
			for (int j = 0; j < elements.length; j++) {
				String type = elements[j].getAttribute(ATTR_TYPE);
				if (type != null) {
					fExtentionMap.put(type, elements[j]);
				}
				String update = elements[j].getAttribute(ATTR_CAN_UPDATE);
				if (update != null) {
					fUpdateMap.put(type, Boolean.valueOf(update));
				}
			}
		}
	}

}
