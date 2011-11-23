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
package org.eclipse.ui.trace.internal.datamodel;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility handler for storing the options that were changed
 * 
 * @since 8.0
 */
public class ModifiedDebugOptions {

	/**
	 * Construct a new ModifiedDebugOptions object
	 */
	public ModifiedDebugOptions() {

		this.debugOptionsToAdd = new ArrayList<TracingComponentDebugOption>();
		this.debugOptionsToRemove = new ArrayList<TracingComponentDebugOption>();
	}

	/**
	 * Accessor for an array of the {@link TracingComponentDebugOption} items that were selected to be added on the
	 * tracing preference page.
	 * 
	 * @return An array of the {@link TracingComponentDebugOption} items that were selected to be added on the tracing
	 *         preference page
	 */
	public final TracingComponentDebugOption[] getDebugOptionsToAdd() {

		return this.debugOptionsToAdd.toArray(new TracingComponentDebugOption[this.debugOptionsToAdd.size()]);
	}

	/**
	 * Accessor for an array of the {@link TracingComponentDebugOption} items that were selected to be removed on the
	 * tracing preference page.
	 * 
	 * @return An array of the {@link TracingComponentDebugOption} items that were selected to be removed on the tracing
	 *         preference page
	 */
	public final TracingComponentDebugOption[] getDebugOptionsToRemove() {

		return this.debugOptionsToRemove.toArray(new TracingComponentDebugOption[this.debugOptionsToRemove.size()]);
	}

	/**
	 * Adds a new {@link TracingComponentDebugOption} to the list of debug options to add
	 * 
	 * @param option
	 *            The {@link TracingComponentDebugOption} option to add
	 */
	public final void addDebugOption(final TracingComponentDebugOption option) {

		if (option != null) {
			boolean isBeingRemoved = this.debugOptionsToRemove.contains(option);
			if (isBeingRemoved) {
				// remove it from the list of debug options to remove
				this.debugOptionsToRemove.remove(option);
			} else {
				// add it to the list of debug options to add
				this.debugOptionsToAdd.add(option);
			}
		}
	}

	/**
	 * Adds a new {@link TracingComponentDebugOption} to the list of debug options to remove
	 * 
	 * @param option
	 *            The {@link TracingComponentDebugOption} option to add
	 */
	public final void removeDebugOption(final TracingComponentDebugOption option) {

		if (option != null) {
			boolean isBeingAdded = this.debugOptionsToAdd.contains(option);
			if (isBeingAdded) {
				// remove it from the list of debug options to add
				this.debugOptionsToAdd.remove(option);
			} else {
				// add it to the list of debug options to remove
				this.debugOptionsToRemove.add(option);
			}
		}
	}

	/**
	 * Purge the list of bundles to add and remove
	 */
	public final void clear() {

		this.debugOptionsToAdd.clear();
		this.debugOptionsToRemove.clear();
	}

	/**
	 * A list of the {@link TracingComponentDebugOption} instances to be added.
	 */
	private List<TracingComponentDebugOption> debugOptionsToAdd = null;

	/**
	 * A list of the {@link TracingComponentDebugOption} instances to be removed.
	 */
	private List<TracingComponentDebugOption> debugOptionsToRemove = null;
}