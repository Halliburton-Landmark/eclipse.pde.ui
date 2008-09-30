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
package org.eclipse.pde.api.tools.internal.provisional;

import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;


/**
 * Interface describing the {@link ApiBaselineManager}
 * 
 * @noimplement this interface is not to be implemented by clients
 * @since 1.0.0
 */
public interface IApiProfileManager {
	
	/**
	 * Allows a new profile to be added to the managers' cache of profiles.
	 * New profiles are added to the cache on a replace policy, meaning if there
	 * already exists an {@link IApiBaseline} entry it will be replaced with the 
	 * new profile provided. If <code>null</code> is passed in as a new profile 
	 * no work is done.
	 * 
	 * @param newprofile the new profile to add to the manager
	 */
	public void addApiProfile(IApiBaseline newprofile);
	
	/**
	 * Returns the complete listing of {@link IApiBaseline}s contained in the 
	 * manager or an empty array, never <code>null</code>.
	 * 
	 * @return the complete listing of {@link IApiBaseline}s or an empty array
	 */
	public IApiBaseline[] getApiProfiles();
	
	/**
	 * Returns the {@link IApiBaseline} object with the given id, or
	 * <code>null</code> if there is no profile with the given id.
	 * 
	 * @param name the name of the profile to fetch
	 * @return the {@link IApiBaseline} with the given id or <code>null</code>
	 */
	public IApiBaseline getApiProfile(String name);
	
	/**
	 * Removes the {@link IApiBaseline} with the given id from 
	 * the manager, which propagates to the file-system to remove the 
	 * underlying stored profile (if it exists).
	 * 
	 * @param name the unique name of the profile to remove from the manager
	 * @return true if the removal was successful false otherwise. A successful removal 
	 * constitutes the associated profile being removed from the manager and/or the 
	 * persisted state file being removed from disk.
	 */
	public boolean removeApiProfile(String name);
	
	/**
	 * Allows the {@link IApiBaseline} with the specified id to be set as 
	 * the default profile. This method will accept <code>null</code>, which will remove
	 * a default {@link IApiBaseline} setting.
	 * @param name the name of the {@link IApiBaseline} to be the default
	 */
	public void setDefaultApiProfile(String name);
	
	/**
	 * Returns the {@link IApiBaseline} that is the current default, or <code>null</code>
	 * if one has not been set, or the currently specified id for the default profile no longer exists.
	 * 
	 * @return the default {@link IApiBaseline} or <code>null</code>
	 */
	public IApiBaseline getDefaultApiProfile();
	
	/**
	 * Returns the workspace profile. Creates a new one if one does not exist.
	 * If this method is called without the framework running it returns <code>null</code>
	 * <p>
	 * The workspace profile should be re-retrieved each time it is required
	 * as some workspace modifications cause the underlying profile object to
	 * change (for example, modification of MANIFEST.MF, build.properties,
	 * or project build paths).
	 * </p>
	 * @return the workspace profile or <code>null</code>
	 */
	public IApiBaseline getWorkspaceProfile();
}
