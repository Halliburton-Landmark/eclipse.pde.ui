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
package org.eclipse.pde.api.tools.internal.provisional.model;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.service.resolver.ResolverError;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.core.plugin.IPluginModelBase;

/**
 * A collection of related API components that together make up
 * an API profile that can be compared with another profile.
 * 
 * @since 1.0.0
 */
public interface IApiBaseline extends IApiParent, IApiElement {
	
	/**
	 * Returns all API components in this profile. The components
	 * are returned in the order that components are searched when
	 * performing name lookup for a type (simulates the order
	 * components would be searched when performing class loading
	 * at runtime or name resolution at compile time).
	 *  
	 * @return all API components in this profile
	 */
	public IApiComponent[] getApiComponents();
	
	/**
	 * Returns a human readable name for this API profile.
	 * 
	 * @return baseline name
	 */
	public String getName();
	
	/**
	 * Allows the name of the profile to be changed to the new name.
	 * If the new name is <code>null</code>, no changes are made.
	 * @param name the new name for the profile
	 */
	public void setName(String name);
	
	/**
	 * Adds the given API components to this profile, excluding all the source components.
	 * 
	 * @param components components to add
	 */
	public void addApiComponents(IApiComponent[] components);
	
	/**
	 * Returns the API components that provides the specified package when referenced from
	 * the specified source component or an empty array if none, never <code>null</code>.
	 * 
	 * @param sourceComponent component referencing the package
	 * @param packageName name of referenced package
	 * @return API components providing the package or an empty array
	 * @exception CoreException if an exception occurs
	 */
	public IApiComponent[] resolvePackage(IApiComponent sourceComponent, String packageName) throws CoreException;
	
	/**
	 * Creates and returns a new API component for this profile at the specified
	 * location or <code>null</code> if the location specified does not contain
	 * a valid API component. The component is not added to the profile.
	 * 
	 * @param location absolute path in the local file system to the API component
	 * @return API component or <code>null</code> if the location specified does not contain a valid
	 * 	API component
	 * @exception CoreException if unable to create the component
	 */
	public IApiComponent newApiComponent(String location) throws CoreException;

	/**
	 * Creates and returns a new API component for this profile based on the given
	 * model or <code>null</code> if the given model cannot be resolved or does not contain
	 * a valid API component. The component is not added to the profile.
	 *
	 * @param model the given model
	 * @return API component or <code>null</code> if the given model cannot be resolved or does not contain
	 * a valid API component
	 * @exception CoreException if unable to create the component
	 */
	public IApiComponent newApiComponent(IPluginModelBase model) throws CoreException;

	/**
	 * Returns the API component in this profile with the given symbolic name
	 * or <code>null</code> if none.
	 * 
	 * @param id component symbolic name
	 * @return API component or <code>null</code>
	 */
	public IApiComponent getApiComponent(String id);
	
	/**
	 * Returns the execution environment this profile is resolved with, or
	 * <code>null</code> if none (not yet determined or unable to bind to an
	 * execution environment). A profile can be created with a specific execution
	 * environment, or be created to automatically resolve an execution environment
	 * as components are added to it.
	 * <p> 
	 * An execution environment is represented by a unique identifier
	 * as defined by OSGi - for example "J2SE-1.4" or "CDC-1.0/Foundation-1.0".
	 * </p>
	 * @return execution environment identifier or <code>null</code>
	 */
	public String getExecutionEnvironment();
	
	/**
	 * Disposes this API profile. Clients must call this method when done
	 * with a profile in order to free system resources.
	 * <p>
	 * All API components in this profile are disposed.
	 * </p>
	 */
	public void dispose();
	
	/**
	 * Closes all components in this profile. The profile may still be used after closing,
	 * but clients should close the profile when they are done with it to free
	 * system resources.
	 * 
	 * @throws CoreException if closing fails
	 */
	public void close() throws CoreException;	
	
	/**
	 * Returns all components in this profile depending on the given components.
	 * The returned collection includes the given components and all dependents.
	 * 
	 * @param components the initial set of components
	 * @return an array of components for the given roots and all
	 * 	components in the state that depend on them
	 */
	public IApiComponent[] getDependentComponents(IApiComponent[] components);

	/**
	 * Returns all the prerequisite components in this profile for the given components. 
	 * The returned collection includes the given components and all prerequisites.
	 * 
	 * @param components the initial set of components
	 * @return an array of components for the given leaves and their
	 * 	prerequisite components in this profile
	 */
	public IApiComponent[] getPrerequisiteComponents(IApiComponent[] components);
	
	/**
	 * Returns all errors in the underlying {@link org.eclipse.osgi.service.resolver.State}
	 * from an attempt to resolve the state. 
	 * 
	 * If there are no resolution errors an empty array is returned.
	 * 
	 * @return state errors
	 */
	public ResolverError[] getResolutionErrors();
	
	/**
	 * Returns the combined status of the {@link IApiBaseline}
	 * 
	 * @return the combined status of this baseline
	 */
	public IStatus getStatus();
}
