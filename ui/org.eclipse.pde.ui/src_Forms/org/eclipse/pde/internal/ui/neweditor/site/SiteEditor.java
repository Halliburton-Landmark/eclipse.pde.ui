/*
 * Created on Jan 27, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.eclipse.pde.internal.ui.neweditor.site;
import java.io.File;

import org.eclipse.core.resources.*;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.SystemFileEditorInput;
import org.eclipse.pde.internal.ui.neweditor.*;
import org.eclipse.pde.internal.ui.neweditor.context.*;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.*;
import org.eclipse.ui.*;
import org.eclipse.ui.forms.editor.IFormPage;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * @author dejan
 * 
 * To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Generation - Code and Comments
 */
public class SiteEditor extends MultiSourceEditor {
	protected void createResourceContexts(InputContextManager manager,
			IFileEditorInput input) {
		IFile file = input.getFile();
		IProject project = file.getProject();
		IFile siteFile = null;
		String name = file.getName().toLowerCase();
		if (name.equals("site.xml")) {
			siteFile = file;
		}
		if (siteFile.exists()) {
			IEditorInput in = new FileEditorInput(siteFile);
			manager.putContext(in, new SiteInputContext(this, in, file==siteFile));
		}
		manager.monitorFile(siteFile);
	}
	
	protected InputContextManager createInputContextManager() {
		SiteInputContextManager contextManager = new SiteInputContextManager();
		contextManager.setUndoManager(new SiteUndoManager(this));
		return contextManager;
	}
	
	public boolean canCopy(ISelection selection) {
		return true;
	}	
	
	protected boolean hasKnownTypes() {
		try {
			TransferData[] types = getClipboard().getAvailableTypes();
			Transfer[] transfers =
				new Transfer[] { TextTransfer.getInstance(), RTFTransfer.getInstance()};
			for (int i = 0; i < types.length; i++) {
				for (int j = 0; j < transfers.length; j++) {
					if (transfers[j].isSupportedType(types[i]))
						return true;
				}
			}
		} catch (SWTError e) {
		}
		return false;
	}
	
	protected IModelUndoManager createModelUndoManager() {
		return new SiteUndoManager(this);
	}
	
	public void monitoredFileAdded(IFile file) {
		String name = file.getName();
		if (name.equalsIgnoreCase("site.xml")) {
			IEditorInput in = new FileEditorInput(file);
			inputContextManager.putContext(in, new SiteInputContext(this, in, false));
		}
	}

	public boolean monitoredFileRemoved(IFile file) {
		//TODO may need to check with the user if there
		//are unsaved changes in the model for the
		//file that just got removed under us.
		return true;
	}
	public void contextAdded(InputContext context) {
		addSourcePage(context.getId());
	}
	public void contextRemoved(InputContext context) {
		IFormPage page = findPage(context.getId());
		if (page!=null)
			removePage(context.getId());
	}

	protected void createSystemFileContexts(InputContextManager manager,
			SystemFileEditorInput input) {
		File file = (File) input.getAdapter(File.class);
		File siteFile = null;

		String name = file.getName().toLowerCase();
		if (name.equals("site.xml")) {
			siteFile = file;
		}
		if (siteFile.exists()) {
			IEditorInput in = new SystemFileEditorInput(siteFile);
			manager.putContext(in, new SiteInputContext(this, in,
					file == siteFile));
		}
	}

	protected void createStorageContexts(InputContextManager manager,
			IStorageEditorInput input) {
		String name = input.getName().toLowerCase();
		if (name.equals("site.xml")) {
			manager.putContext(input,
							new SiteInputContext(this, input, true));
		}
	}
	
	protected void contextMenuAboutToShow(IMenuManager manager) {
		super.contextMenuAboutToShow(manager);
	}

	protected void addPages() {
		try {
			addPage(new FeaturesPage(this));
			addPage(new ArchivePage(this));
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
		addSourcePage(SiteInputContext.CONTEXT_ID);
	}


	protected String computeInitialPageId() {
		String firstPageId = super.computeInitialPageId();
		if (firstPageId == null) {
			InputContext primary = inputContextManager.getPrimaryContext();
			if (primary.getId().equals(SiteInputContext.CONTEXT_ID))
				firstPageId = FeaturesPage.PAGE_ID;
			if (firstPageId == null)
				firstPageId = FeaturesPage.PAGE_ID;
		}
		return firstPageId;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.neweditor.MultiSourceEditor#createXMLSourcePage(org.eclipse.pde.internal.ui.neweditor.PDEFormEditor, java.lang.String, java.lang.String)
	 */
	protected PDESourcePage createXMLSourcePage(PDEFormEditor editor, String title, String name) {
		return new SiteSourcePage(editor, title, name);
	}
	
	protected IContentOutlinePage createContentOutline() {
		return null;//return new SiteOutlinePage(this);
	}
	public void doRevert() {
		/*
		try {
			((ISiteModel)getModel()).getBuildModel().load();
			super.doRevert();
		} catch (CoreException e) {
		}
		*/
	}
}