/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.feature;

import java.io.File;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.pde.internal.core.feature.FeatureChild;
import org.eclipse.pde.internal.core.feature.FeaturePlugin;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeatureData;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.SystemFileEditorInput;
import org.eclipse.pde.internal.ui.editor.plugin.ManifestEditor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.actions.SelectionProviderAction;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;

public class OpenReferenceAction extends SelectionProviderAction {
	public OpenReferenceAction(ISelectionProvider provider) {
	super(provider, PDEUIMessages.Actions_open_label);
}

	public void run() {
		IStructuredSelection sel = (IStructuredSelection) getSelection();
		Object obj = sel.getFirstElement();
		
		if (obj instanceof FeaturePlugin) {
			FeaturePlugin reference = (FeaturePlugin) obj;
			String pluginId = reference.getId();
			ManifestEditor.openPluginEditor(pluginId);
		} else if (obj instanceof IFeatureData) {
			IFeatureData data = (IFeatureData) obj;
			String id = data.getId();
			IResource resource = data.getModel().getUnderlyingResource();
			if (resource != null) {
				IProject project = resource.getProject();
				IFile file = project.getFile(id);
				if (file != null && file.exists()) {
					IWorkbenchPage page = PDEPlugin.getActivePage();
					try {
						IDE.openEditor(page, file, true);
					} catch (PartInitException e) {
					}
				}
			}
		} else if (obj instanceof IFeatureChild) {
			IFeatureChild included = (IFeatureChild) obj;
			IFeature feature = ((FeatureChild) included).getReferencedFeature();
			if (feature != null) {
				IEditorInput input = null;
				IResource resource = feature.getModel().getUnderlyingResource();
				if (resource != null)
					input = new FileEditorInput((IFile) resource);
				else
					input = new SystemFileEditorInput(new File(feature
							.getModel().getInstallLocation(), "feature.xml")); //$NON-NLS-1$
				try {
					IDE.openEditor(PDEPlugin.getActivePage(), input,
							PDEPlugin.FEATURE_EDITOR_ID, true);
				} catch (PartInitException e) {
				}
			}
		}
	}

	public void selectionChanged(IStructuredSelection selection) {
	setEnabled(!selection.isEmpty());
}
}
