/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.trace.internal.utils;

import java.io.PrintStream;

/**
 * A collection of constant values used by the tracing UI
 * 
 * @since 3.6
 */
public class TracingConstants {

	/** The name of the bundle */
	public final static String BUNDLE_ID = "org.eclipse.ui.trace"; //$NON-NLS-1$

	/** The separator for option paths in key=value pairs */
	public final static String DEBUG_OPTION_PATH_SEPARATOR = "="; //$NON-NLS-1$

	/** The separator for option paths in key=value pairs */
	public final static String DEBUG_OPTION_PATH_VALUE_SEPARATOR = "/"; //$NON-NLS-1$

	/** The value for a debug option that is disabled */
	public final static String DEBUG_OPTION_VALUE_FALSE = "false"; //$NON-NLS-1$

	/** The value for a debug option that is enabled **/
	public final static String DEBUG_OPTION_VALUE_TRUE = "true"; //$NON-NLS-1$

	/** Tracing Component extension point name */
	public final static String TRACING_EXTENSION_POINT_NAME = "traceComponent"; //$NON-NLS-1$

	/** The name of the 'id' attribute for a Tracing Component */
	public final static String TRACING_EXTENSION_ID_ATTRIBUTE = "id"; //$NON-NLS-1$

	/** The name of the 'label' attribute for a Tracing Component */
	public final static String TRACING_EXTENSION_LABEL_ATTRIBUTE = "label"; //$NON-NLS-1$

	/** The name of the 'bundle' attribute for a Tracing Component */
	public final static String TRACING_EXTENSION_BUNDLE_ATTRIBUTE = "bundle"; //$NON-NLS-1$

	/** The name of the 'name' attribute for a bundle in a Tracing Component */
	public final static String TRACING_EXTENSION_BUNDLE_NAME_ATTRIBUTE = "name"; //$NON-NLS-1$

	/** The name of the 'consumed' attribute for a bundle in a Tracing Component */
	public final static String TRACING_EXTENSION_BUNDLE_CONSUMED_ATTRIBUTE = "consumed"; //$NON-NLS-1$

	/** The name of the instance preference key */
	public final static String TRACING_PREFERENCE_KEY = "tracingStrings"; //$NON-NLS-1$

	/** An empty {@link String} array **/
	public final static String[] EMPTY_STRING_ARRAY = new String[0];

	/** An empty {@link String} **/
	public final static String EMPTY_STRING = ""; //$NON-NLS-1$

	/** The index of the label column in the tree */
	public final static int LABEL_COLUMN_INDEX = 0;

	/** The index of the value column in the tree */
	public final static int VALUE_COLUMN_INDEX = 1;

	/** The name of the .options file used to store the debug options for a bundle */
	public final static String OPTIONS_FILENAME = ".options"; //$NON-NLS-1$

	/** The name of the file containing text written to the System.out output stream */
	public final static String SYSTEM_OUT_FILENAME = "systemOut.log"; //$NON-NLS-1$

	/** The name of the file containing text written to the System.err output stream */
	public final static String SYSTEM_ERR_FILENAME = "systemErr.log"; //$NON-NLS-1$

	/** The original System.out {@link PrintStream} */
	public final static PrintStream ORIGINAL_SYSTEM_OUT_STREAM = System.out;

	/** The original System.err {@link PrintStream} */
	public final static PrintStream ORIGINAL_SYSTEM_ERR_STREAM = System.err;

	/** The separator character for a debug option represented as a string, i.e. key1=value1;key2=value2;key3=value3; */
	public final static String DEBUG_OPTION_PREFERENCE_SEPARATOR = ";"; //$NON-NLS-1$

	/** The preference identifier for the tracing enablement state */
	public final static String PREFERENCE_ENABLEMENT_IDENTIFIER = "tracingEnabled"; //$NON-NLS-1$

	/** The preference identifier for the list of tracing entries */
	public final static String PREFERENCE_ENTRIES_IDENTIFIER = "tracingEntries"; //$NON-NLS-1$

	/** The preference identifier for the maximum size of the tracing files */
	public final static String PREFERENCE_MAX_FILE_SIZE_IDENTIFIER = "tracingMaxFileSize"; //$NON-NLS-1$

	/** The preference identifier for the maximum number of tracing files */
	public final static String PREFERENCE_MAX_FILE_COUNT_IDENTIFIER = "tracingMaxFileCount"; //$NON-NLS-1$

	/** The preference identifier for the location of tracing files */
	public final static String PREFERENCE_FILE_PATH = "tracingFilePath"; //$NON-NLS-1$

	/** The tracing identifier for generic tracing of this bundle */
	public final static String TRACE_DEBUG_STRING = "/debug"; //$NON-NLS-1$

	/** The tracing identifier for tracing the preference handling of this bundle */
	public final static String TRACE_PREFERENCES_STRING = "/debug/preference"; //$NON-NLS-1$

	/** The tracing identifier for tracing model operations used in this bundle */
	public final static String TRACE_MODEL_STRING = "/debug/model"; //$NON-NLS-1$

	/** The tracing identifier for tracing model operations used in this bundle */
	public final static String TRACE_UI_STRING = "/debug/ui"; //$NON-NLS-1$

	/** The tracing identifier for tracing model operations used in this bundle */
	public final static String TRACE_UI_LISTENERS_STRING = "/debug/ui/listeners"; //$NON-NLS-1$

	/** The tracing identifier for tracing model operations used in this bundle */
	public final static String TRACE_UI_PROVIDERS_STRING = "/debug/ui/providers"; //$NON-NLS-1$
}