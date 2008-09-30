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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.pde.api.tools.internal.ApiDescriptionManager;
import org.eclipse.pde.api.tools.internal.ApiFilterStore;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.JavadocTagManager;
import org.eclipse.pde.api.tools.internal.PluginProjectApiComponent;
import org.eclipse.pde.api.tools.internal.builder.ApiAnalysisBuilder;
import org.eclipse.pde.api.tools.internal.comparator.ClassFileComparator;
import org.eclipse.pde.api.tools.internal.descriptors.ElementDescriptorImpl;
import org.eclipse.pde.api.tools.internal.model.ApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.comparator.ApiComparator;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblemTypes;
import org.eclipse.pde.api.tools.internal.provisional.scanner.TagScanner;
import org.eclipse.pde.api.tools.internal.search.ClassFileVisitor;
import org.eclipse.pde.api.tools.internal.search.SearchEngine;
import org.osgi.framework.BundleContext;

/**
 * API Tools core plug-in.
 * API tools can be run with or without an OSGi framework.
 * 
 * @since 1.0.0
 */
public class ApiPlugin extends Plugin implements ISaveParticipant {
	
	/**
	 * Constant representing the name of the javadoc tag extension point.
	 * Value is <code>apiJavadocTags</code>
	 */
	public static final String EXTENSION_JAVADOC_TAGS = "apiJavadocTags"; //$NON-NLS-1$
	/**
	 * The plug-in identifier of the PDE API tool support
	 * (value <code>"org.eclipse.pde.api.tools"</code>).
	 */
	public static final String PLUGIN_ID = "org.eclipse.pde.api.tools" ; //$NON-NLS-1$
	/**
	 * The API tooling nature id
	 * (value <code>"org.eclipse.pde.api.tools.apiAnalysisNature"</code>).
	 */
	public static final String NATURE_ID = PLUGIN_ID + ".apiAnalysisNature" ; //$NON-NLS-1$
	/**
	 * Status code indicating an unexpected internal error.
	 */
	public static final int INTERNAL_ERROR = 120;
	/**
	 * Status code indicating an unexpected error
	 */
	public static final int ERROR = 121;
	
	/**
	 * Constant representing severity levels for error/warning preferences
	 * Value is: <code>0</code>
	 */
	public static final int SEVERITY_IGNORE = 0;
	/**
	 * Constant representing severity levels for error/warning preferences
	 * Value is: <code>1</code>
	 */
	public static final int SEVERITY_WARNING = 1;
	/**
	 * Constant representing severity levels for error/warning preferences
	 * Value is: <code>2</code>
	 */
	public static final int SEVERITY_ERROR = 2;
	
	/**
	 * Constant representing the preference value 'ignore'.
	 * Value is: <code>Ignore</code>
	 */
	public static final String VALUE_IGNORE = "Ignore"; //$NON-NLS-1$
	/**
	 * Constant representing the preference value 'warning'.
	 * Value is: <code>Warning</code>
	 */
	public static final String VALUE_WARNING = "Warning"; //$NON-NLS-1$
	/**
	 * Constant representing the preference value 'error'.
	 * Value is: <code>Error</code>
	 */
	public static final String VALUE_ERROR = "Error"; //$NON-NLS-1$
	/**
	 * Constant representing the preference value 'disabled'.
	 * Value is: <code>Disabled</code>
	 */
	public static final String VALUE_DISABLED = "Disabled"; //$NON-NLS-1$
	/**
	 * Constant representing the preference value 'enabled'.
	 * Value is: <code>Enabled</code>
	 */
	public static final String VALUE_ENABLED = "Enabled"; //$NON-NLS-1$
	/**
	 * The identifier for the Api builder
	 * Value is: <code>"org.eclipse.pde.api.tools.apiAnalysisBuilder"</code>
	 */
	public static final String BUILDER_ID = PLUGIN_ID + ".apiAnalysisBuilder" ; //$NON-NLS-1$
	
	/**
	 * Singleton instance of the plugin
	 */
	private static ApiPlugin fgDefault = null;
	/**
	 * Singleton instance of the {@link JavadocTagManager}
	 */
	private static JavadocTagManager fgTagManager = null;

	/**
	 * Private debug options
	 */
	private static final String BUILDER_DEBUG = PLUGIN_ID + "/debug/builder" ; //$NON-NLS-1$
	private static final String DELTA_DEBUG = PLUGIN_ID + "/debug/delta" ; //$NON-NLS-1$
	private static final String CLASSFILE_VISITOR_DEBUG = PLUGIN_ID + "/debug/classfilevisitor" ; //$NON-NLS-1$
	private static final String SEARCH_DEBUG = PLUGIN_ID + "/debug/search" ; //$NON-NLS-1$
	private static final String DESCRIPTOR_FRAMEWORK_DEBUG = PLUGIN_ID + "/debug/descriptor/framework" ; //$NON-NLS-1$
	private static final String TAG_SCANNER_DEBUG = PLUGIN_ID + "/debug/tagscanner" ; //$NON-NLS-1$
	private static final String PLUGIN_WORKSPACE_COMPONENT_DEBUG = PLUGIN_ID + "/debug/pluginworkspacecomponent"; //$NON-NLS-1$
	private static final String API_PROFILE_MANAGER_DEBUG = PLUGIN_ID + "/debug/profilemanager"; //$NON-NLS-1$
	private static final String API_PROFILE_DEBUG = PLUGIN_ID + "/debug/apiprofile" ; //$NON-NLS-1$
	private static final String API_FILTER_STORE_DEBUG = PLUGIN_ID + "/debug/apifilterstore"; //$NON-NLS-1$
	private static final String API_ANALYZER_DEBUG = PLUGIN_ID + "/debug/apianalyzer"; //$NON-NLS-1$

	public final static String TRUE = "true"; //$NON-NLS-1$

	public static String[] AllCompatibilityKeys = new String[] {
		IApiProblemTypes.API_COMPONENT_REMOVED_TYPE,
		IApiProblemTypes.API_COMPONENT_REMOVED_API_TYPE,
		IApiProblemTypes.ANNOTATION_REMOVED_FIELD,
		IApiProblemTypes.ANNOTATION_REMOVED_METHOD,
		IApiProblemTypes.ANNOTATION_REMOVED_TYPE_MEMBER,
		IApiProblemTypes.ANNOTATION_CHANGED_TYPE_CONVERSION,
		IApiProblemTypes.ANNOTATION_ADDED_METHOD_NO_DEFAULT_VALUE,
		IApiProblemTypes.INTERFACE_ADDED_FIELD,
		IApiProblemTypes.INTERFACE_ADDED_METHOD,
		IApiProblemTypes.INTERFACE_ADDED_TYPE_PARAMETER,
		IApiProblemTypes.INTERFACE_REMOVED_TYPE_PARAMETER,
		IApiProblemTypes.INTERFACE_REMOVED_FIELD,
		IApiProblemTypes.INTERFACE_REMOVED_METHOD,
		IApiProblemTypes.INTERFACE_REMOVED_TYPE_MEMBER,
		IApiProblemTypes.INTERFACE_CHANGED_TYPE_CONVERSION,
		IApiProblemTypes.INTERFACE_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
		IApiProblemTypes.INTERFACE_CHANGED_RESTRICTIONS,
		IApiProblemTypes.ENUM_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
		IApiProblemTypes.ENUM_CHANGED_TYPE_CONVERSION,
		IApiProblemTypes.ENUM_REMOVED_FIELD,
		IApiProblemTypes.ENUM_REMOVED_ENUM_CONSTANT,
		IApiProblemTypes.ENUM_REMOVED_METHOD,
		IApiProblemTypes.ENUM_REMOVED_TYPE_MEMBER,
		IApiProblemTypes.CLASS_ADDED_METHOD,
		IApiProblemTypes.CLASS_ADDED_TYPE_PARAMETER,
		IApiProblemTypes.CLASS_CHANGED_CONTRACTED_SUPERINTERFACES_SET,
		IApiProblemTypes.CLASS_CHANGED_NON_ABSTRACT_TO_ABSTRACT,
		IApiProblemTypes.CLASS_CHANGED_NON_FINAL_TO_FINAL,
		IApiProblemTypes.CLASS_CHANGED_TYPE_CONVERSION,
		IApiProblemTypes.CLASS_CHANGED_DECREASE_ACCESS,
		IApiProblemTypes.CLASS_CHANGED_RESTRICTIONS,
		IApiProblemTypes.CLASS_REMOVED_FIELD,
		IApiProblemTypes.CLASS_REMOVED_METHOD,
		IApiProblemTypes.CLASS_REMOVED_CONSTRUCTOR,
		IApiProblemTypes.CLASS_REMOVED_SUPERCLASS,
		IApiProblemTypes.CLASS_REMOVED_TYPE_MEMBER,
		IApiProblemTypes.CLASS_REMOVED_TYPE_PARAMETER,
		IApiProblemTypes.FIELD_ADDED_VALUE,
		IApiProblemTypes.FIELD_CHANGED_TYPE,
		IApiProblemTypes.FIELD_CHANGED_VALUE,
		IApiProblemTypes.FIELD_CHANGED_DECREASE_ACCESS,
		IApiProblemTypes.FIELD_CHANGED_FINAL_TO_NON_FINAL_STATIC_CONSTANT,
		IApiProblemTypes.FIELD_CHANGED_NON_FINAL_TO_FINAL,
		IApiProblemTypes.FIELD_CHANGED_STATIC_TO_NON_STATIC,
		IApiProblemTypes.FIELD_CHANGED_NON_STATIC_TO_STATIC,
		IApiProblemTypes.FIELD_REMOVED_VALUE,
		IApiProblemTypes.FIELD_REMOVED_TYPE_ARGUMENT,
		IApiProblemTypes.METHOD_ADDED_TYPE_PARAMETER,
		IApiProblemTypes.METHOD_CHANGED_VARARGS_TO_ARRAY,
		IApiProblemTypes.METHOD_CHANGED_DECREASE_ACCESS,
		IApiProblemTypes.METHOD_CHANGED_NON_ABSTRACT_TO_ABSTRACT,
		IApiProblemTypes.METHOD_CHANGED_NON_STATIC_TO_STATIC,
		IApiProblemTypes.METHOD_CHANGED_RESTRICTIONS,
		IApiProblemTypes.METHOD_CHANGED_STATIC_TO_NON_STATIC,
		IApiProblemTypes.METHOD_CHANGED_NON_FINAL_TO_FINAL,
		IApiProblemTypes.METHOD_REMOVED_ANNOTATION_DEFAULT_VALUE,
		IApiProblemTypes.METHOD_REMOVED_TYPE_PARAMETER,
		IApiProblemTypes.CONSTRUCTOR_ADDED_TYPE_PARAMETER,
		IApiProblemTypes.CONSTRUCTOR_CHANGED_VARARGS_TO_ARRAY,
		IApiProblemTypes.CONSTRUCTOR_CHANGED_DECREASE_ACCESS,
		IApiProblemTypes.CONSTRUCTOR_REMOVED_TYPE_PARAMETER,
		IApiProblemTypes.TYPE_PARAMETER_ADDED_CLASS_BOUND,
		IApiProblemTypes.TYPE_PARAMETER_CHANGED_CLASS_BOUND,
		IApiProblemTypes.TYPE_PARAMETER_REMOVED_CLASS_BOUND,
		IApiProblemTypes.TYPE_PARAMETER_ADDED_INTERFACE_BOUND,
		IApiProblemTypes.TYPE_PARAMETER_CHANGED_INTERFACE_BOUND,
		IApiProblemTypes.TYPE_PARAMETER_REMOVED_INTERFACE_BOUND,
	};
	/**
	 * A set of listeners that want to participate in the saving life-cycle of the workbench
	 * via this plugin
	 */
	private HashSet savelisteners = new HashSet();
	
	/**
	 * Constructor
	 */
	public ApiPlugin() {
		super();
		fgDefault = this;
	}
	
	/**
	 * @return The singleton instance of the plugin
	 */
	public static ApiPlugin getDefault() {
		return fgDefault;
	}
	
	/**
	 * @return the id of this plugin.
	 * Value is <code>org.eclipse.pde.api.tools</code>
	 */
	public static String getPluginIdentifier() {
		return PLUGIN_ID;
	}
	
	/**
	 * Logs the specified status with this plug-in's log.
	 * 
	 * @param status status to log
	 */
	public static void log(IStatus status) {
		if (getDefault() == null) {
			Throwable exception = status.getException();
			if (exception != null) {
				exception.printStackTrace();
			}
		} else {
			getDefault().getLog().log(status);
		}
	}
	
	/**
	 * Logs the specified throwable with this plug-in's log.
	 * 
	 * @param t throwable to log 
	 */
	public static void log(Throwable t) {
		log(newErrorStatus("Error logged from API Tools Core: ", t)); //$NON-NLS-1$
	}
	
	/**
	 * Logs an internal error with the specified message.
	 * 
	 * @param message the error message to log
	 */
	public static void logErrorMessage(String message) {
		// this message is intentionally not internationalized, as an exception may
		// be due to the resource bundle itself
		log(newErrorStatus("Internal message logged from API Tools Core: " + message, null)); //$NON-NLS-1$	
	}
	
	/**
	 * Returns a new error status for this plug-in with the given message
	 * @param message the message to be included in the status
	 * @param exception the exception to be included in the status or <code>null</code> if none
	 * @return a new error status
	 */
	public static IStatus newErrorStatus(String message, Throwable exception) {
		return new Status(IStatus.ERROR, getPluginIdentifier(), INTERNAL_ERROR, message, exception);
	}
	
	/**
	 * Returns whether the API tools bundle is running inside an OSGi framework.
	 * 
	 * @return whether the API tools bundle is running inside an OSGi framework
	 */
	public static boolean isRunningInFramework() {
		return fgDefault != null;
	}

	/**
	 * Returns the {@link IApiProfileManager}, allowing clients to add/remove and search
	 * for {@link IApiBaseline}s stored in the manager.
	 * 
	 * @return the singleton instance of the {@link IApiProfileManager}
	 */
	public IApiProfileManager getApiProfileManager() {
		return ApiBaselineManager.getManager();
	}
	
	/**
	 * @return The singleton instance of the {@link JavadocTagManager}
	 */
	public static JavadocTagManager getJavadocTagManager() {
		if(fgTagManager == null) {
			fgTagManager = new JavadocTagManager();
		}
		return fgTagManager;
	}
	
	/**
	 * Adds the given save participant to the listing of participants to 
	 * be notified when the workbench saving life-cycle occurs. If the specified
	 * participant is <code>null</code> no changes are made.
	 * @param participant
	 */
	public void addSaveParticipant(ISaveParticipant participant) {
		if(participant != null) {
			savelisteners.add(participant);
		}
	}
	
	/**
	 * Removes the given save participant from the current listing.
	 * If the specified participant is <code>null</code> no changes are made.
	 * @param participant
	 */
	public void removeSaveParticipant(ISaveParticipant participant) {
		if(participant != null) {
			savelisteners.remove(participant);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.ISaveParticipant#doneSaving(org.eclipse.core.resources.ISaveContext)
	 */
	public void doneSaving(ISaveContext context) {
		ISaveParticipant sp = null;
		for(Iterator iter = savelisteners.iterator(); iter.hasNext();) {
			sp = (ISaveParticipant) iter.next();
			sp.doneSaving(context);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.ISaveParticipant#prepareToSave(org.eclipse.core.resources.ISaveContext)
	 */
	public void prepareToSave(ISaveContext context) throws CoreException {
		ISaveParticipant sp = null;
		for(Iterator iter = savelisteners.iterator(); iter.hasNext();) {
			sp = (ISaveParticipant) iter.next();
			sp.prepareToSave(context);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.ISaveParticipant#rollback(org.eclipse.core.resources.ISaveContext)
	 */
	public void rollback(ISaveContext context) {
		ISaveParticipant sp = null;
		for(Iterator iter = savelisteners.iterator(); iter.hasNext();) {
			sp = (ISaveParticipant) iter.next();
			sp.rollback(context);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.ISaveParticipant#saving(org.eclipse.core.resources.ISaveContext)
	 */
	public void saving(ISaveContext context) throws CoreException {
		if(context.getKind() == ISaveContext.FULL_SAVE) {
			context.needDelta();
		}
		ISaveParticipant sp = null;
		for(Iterator iter = savelisteners.iterator(); iter.hasNext();) {
			sp = (ISaveParticipant) iter.next();
			sp.saving(context);
		}
		savePluginPreferences();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		try {
			super.start(context);
		} finally {
			ResourcesPlugin.getWorkspace().addSaveParticipant(this, this);
			configurePluginDebugOptions();
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.Plugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		try {
			ApiDescriptionManager.shutdown();
			ResourcesPlugin.getWorkspace().removeSaveParticipant(this);
		}
		finally {
			super.stop(context);
		}
	}

	/**
	 * Returns the severity for the specific key from the given {@link IProject}.
	 * If the project does not have project specific settings, the workspace preference
	 * is returned. If <code>null</code> is passed in as the project the workspace
	 * preferences are consulted.
	 * 
	 * @param prefkey the given preference key
	 * @param project the given project or <code>null</code>
	 * @return the severity level for the given pref key
	 */
	public int getSeverityLevel(String prefkey, IProject project) {
		IPreferencesService service = Platform.getPreferencesService();
		List scopes = new ArrayList();
		scopes.add(new InstanceScope());
		if(project != null) {
			scopes.add(new ProjectScope(project));
		}
		String value = service.getString(ApiPlugin.getPluginIdentifier(), prefkey, null, (IScopeContext[]) scopes.toArray(new IScopeContext[scopes.size()]));
		if(value == null) {
			value = getPluginPreferences().getDefaultString(prefkey);
		}
		if(VALUE_ERROR.equals(value)) {
			return SEVERITY_ERROR;
		}
		if(VALUE_WARNING.equals(value)) {
			return SEVERITY_WARNING;
		}
		return SEVERITY_IGNORE;
	}

	/**
	 * Returns the enable state for the specific key from the given {@link IProject}.
	 * If the project does not have project specific settings, the workspace preference
	 * is returned. If <code>null</code> is passed in as the project the workspace
	 * preferences are consulted.
	 * 
	 * @param prefkey the given preference key
	 * @param project the given project or <code>null</code>
	 * @return the enable state
	 */
	public String getEnableState(String prefkey, IProject project) {
		IPreferencesService service = Platform.getPreferencesService();
		List scopes = new ArrayList();
		scopes.add(new InstanceScope());
		if(project != null) {
			scopes.add(new ProjectScope(project));
		}
		String value = service.getString(ApiPlugin.getPluginIdentifier(), prefkey, null, (IScopeContext[]) scopes.toArray(new IScopeContext[scopes.size()]));
		if(value == null) {
			value = getPluginPreferences().getDefaultString(prefkey);
		}
		return value;
	}
	/**
	 * Method to configure all of the debug options for this plugin
	 */
	public void configurePluginDebugOptions(){
		if(ApiPlugin.getDefault().isDebugging()){
			String option = Platform.getDebugOption(BUILDER_DEBUG);
			if(option != null) {
				boolean debugValue = option.equalsIgnoreCase(TRUE);
				ApiAnalysisBuilder.setDebug(debugValue);
			}
			
			option = Platform.getDebugOption(DELTA_DEBUG);
			if(option != null) {
				boolean debugValue = option.equalsIgnoreCase(TRUE);
				ClassFileComparator.setDebug(debugValue);
				ApiComparator.setDebug(debugValue);
			}

			option = Platform.getDebugOption(CLASSFILE_VISITOR_DEBUG);
			if(option != null) {
				ClassFileVisitor.setDebug(option.equalsIgnoreCase(TRUE));
			}

			option = Platform.getDebugOption(SEARCH_DEBUG);
			if(option != null) {
				SearchEngine.setDebug(option.equalsIgnoreCase(TRUE));
			}

			option = Platform.getDebugOption(DESCRIPTOR_FRAMEWORK_DEBUG);
			if(option != null) {
				ElementDescriptorImpl.setDebug(option.equalsIgnoreCase(TRUE));
			}

			option = Platform.getDebugOption(TAG_SCANNER_DEBUG);
			if(option != null) {
				TagScanner.setDebug(option.equalsIgnoreCase(TRUE));
			}

			option = Platform.getDebugOption(PLUGIN_WORKSPACE_COMPONENT_DEBUG);
			if(option != null) {
				PluginProjectApiComponent.setDebug(option.equalsIgnoreCase(TRUE));
			}
			
			option = Platform.getDebugOption(API_PROFILE_MANAGER_DEBUG);
			if(option != null) {
				ApiBaselineManager.setDebug(option.equalsIgnoreCase(TRUE));
			}
			
			option = Platform.getDebugOption(API_PROFILE_DEBUG);
			if(option != null) {
				ApiBaseline.setDebug(option.equalsIgnoreCase(TRUE));
			}
			option = Platform.getDebugOption(API_FILTER_STORE_DEBUG);
			if(option != null) {
				ApiFilterStore.setDebug(option.equalsIgnoreCase(TRUE));
			}
			option = Platform.getDebugOption(API_ANALYZER_DEBUG);
			if(option != null) {
				ApiFilterStore.setDebug(option.equalsIgnoreCase(TRUE));
			}
		}
	}
}
