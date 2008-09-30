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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemFilter;



/**
 * Describes the API of a software component. An API component
 * is composed of a set of class files owned by the component and
 * a description (manifest) of the component's API. 
 * 
 * @since 1.0.0
 */
public interface IApiComponent extends IClassFileContainer {
	
	/**
	 * Returns a human readable name for this component.
	 * 
	 * @return component name
	 * @exception CoreException if unable to retrieve the name
	 */
	public String getName() throws CoreException;
	
	/**
	 * Returns this component's symbolic name.
	 * 
	 * @return component's symbolic name
	 */
	public String getId();
	
	/**
	 * Returns this component's API description.
	 * 
	 * @return API manifest
	 * @throws CoreException if there was a problem creating the API description for this component
	 */
	public IApiDescription getApiDescription() throws CoreException;
	
	/**
	 * Returns whether this component has an underlying API description. Even if a component
	 * has no underlying description it will return one from {@link #getApiDescription()},
	 * but it will be empty. This method allows clients to know if there was any thing used
	 * to populate the description.
	 * 
	 * @return whether this component has an underlying API description
	 */
	public boolean hasApiDescription();
	
	/**
	 * Returns this component's version identifier.
	 * 
	 * @return component version
	 */
	public String getVersion(); 
	
	/**
	 * Returns the execution environments required by this component for building
	 * and running. An execution environment is represented by a unique identifier
	 * as defined by OSGi - for example "J2SE-1.4" or "CDC-1.0/Foundation-1.0".
	 * Any of the environments will allow this component to be resolved.
	 * 
	 * @return execution environment identifier
	 */
	public String[] getExecutionEnvironments();
	
	/**
	 * Returns class file containers containing the class files associated with
	 * this component in the order they would appear on the build path
	 * or class path.
	 * 
	 * @return class file containers
	 */
	public IClassFileContainer[] getClassFileContainers();
	
	/**
	 * Returns class file containers containing the class files associated with
	 * this component that comes from the given component id in the order they 
	 * would appear on the build path or class path.
	 * 
	 * This is used to filter out the class file containers coming from a fragment
	 * 
	 * @param id the given component id
	 * @return class file containers
	 */
	public IClassFileContainer[] getClassFileContainers(String id);

	/**
	 * Returns a collection of descriptions of components required by this
	 * component, or an empty collection if none.
	 * 
	 * @return required component descriptions, possibly empty
	 */
	public IRequiredComponentDescription[] getRequiredComponents();	
	
	/**
	 * Returns the location of this API component.
	 * 
	 * @return location
	 */
	public String getLocation();
	
	/**
	 * Returns if the component is a system component (a JRE definition for example) or not.
	 * System components are not persisted with profiles, as they are recreated dynamically
	 * by the framework 
	 * @return true if the component is a system component, false otherwise
	 */
	public boolean isSystemComponent();
	
	/**
	 * Returns if the component is a source component or not.
	 * A source component is not persisted with profiles, as they don't contain any binaries.
	 * 
	 * <p>A component is a source component if and only if one of the following conditions is true:</p>
	 * <ul>
	 * <li>its manifest contains an entry called <code>Eclipse-SourceBundle</code></li>
	 * <li>its <code>plugin.xml</code> file contains an extension for the extension point if the
	 * component is not a fragment.
	 * <li>its <code>fragment.xml</code> file contains an extension for the extension point if the
	 * component is a fragment.
	 * <code>org.eclipse.pde.core.source</code></li>
	 * </ul>
	 *
	 * @return true if the component is a source component, false otherwise
	 */
	public boolean isSourceComponent();
	/**
	 * Disposes this API component. Clients must call this method when done
	 * with an API component. Note that disposing an {@link IApiBaseline} will dispose all of
	 * its components. 
	 */
	public void dispose();
	
	/**
	 * Returns the profile this component is contained in.
	 * 
	 * @return API profile
	 */
	public IApiBaseline getProfile();
		
	/**
	 * Returns a store of problem filters defined for this component or <code>null</code>
	 * if none.
	 * 
	 * @return filter store or <code>null</code>
	 * @throws CoreException
	 */
	public IApiFilterStore getFilterStore() throws CoreException;
	
	/**
	 * Creates and returns a new problem filter for the given 
	 * {@link IApiProblem}
	 * 
	 * @param problem
	 * @return new problem filter
	 */
	public IApiProblemFilter newProblemFilter(IApiProblem problem);
	
	/**
	 * Returns whether this API component is a fragment.
	 * 
	 * @return whether this API component is a fragment
	 */
	public boolean isFragment();
	
	/**
	 * Returns whether this API component is the host of one or more fragments.
	 * 
	 * @return whether this API component is the host of one or more fragments
	 */
	public boolean hasFragments();
}
