/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.ui.wizards.toc;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.pde.internal.ua.ui.PDEUserAssistanceUIPlugin;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.wizards.newresource.BasicNewFileResourceWizard;

public class TocHTMLWizard extends BasicNewFileResourceWizard {
	protected IFile fNewFile;

	@Override
	public void addPages() {
		IWizardPage mainPage = new TocHTMLWizardPage("newHTMLPage1", getSelection());//$NON-NLS-1$
		mainPage.setTitle(TocWizardMessages.TocHTMLWizard_title);
		mainPage.setDescription(TocWizardMessages.TocHTMLWizard_description);
		addPage(mainPage);
	}

	@Override
	public boolean performFinish() {
		IWizardPage mainPage = getPage("newHTMLPage1"); //$NON-NLS-1$
		if (!(mainPage instanceof TocHTMLWizardPage)) {
			return false;
		}

		fNewFile = ((TocHTMLWizardPage) mainPage).createNewFile();
		if (fNewFile == null) {
			return false;
		}

		try {
			getContainer().run(false, true, getOperation());
			selectAndReveal(fNewFile);
		} catch (InvocationTargetException e) {
			PDEUserAssistanceUIPlugin.logException(e);
			fNewFile = null;
			return false;
		} catch (InterruptedException e) {
			fNewFile = null;
			return false;
		}

		return true;
	}

	private WorkspaceModifyOperation getOperation() {
		return new TocHTMLOperation(fNewFile);
	}

	public IFile getNewResource() {
		return fNewFile;
	}
}
