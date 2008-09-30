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
package org.eclipse.pde.api.tools.ui.internal.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfileManager;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiToolsLabelProvider;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.pde.api.tools.ui.internal.wizards.ApiProfileWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

/**
 * This preference page allows {@link IApiBaseline}s to be created/removed/edited
 * @since 1.0.0
 */
public class ApiProfilesPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	/**
	 * Override to tell the label provider about uncommitted {@link IApiBaseline}s that might have been set to 
	 * be the new default
	 */
	class ProfileLabelProvider extends ApiToolsLabelProvider {
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.ui.internal.ApiToolsLabelProvider#isDefaultProfile(java.lang.Object)
		 */
		protected boolean isDefaultProfile(Object element) {
			return isDefault(element);
		}
	}
	
	private IApiProfileManager manager = ApiPlugin.getDefault().getApiProfileManager();

	private static HashSet removed = new HashSet(8);
	private CheckboxTableViewer tableviewer = null;
	private ArrayList backingcollection = new ArrayList(8);
	private String newdefault = null;
	private Button newbutton = null, 
				   removebutton = null, 
				   editbutton = null;
	protected static int rebuildcount = 0;
	private String origdefault = null;
	private boolean dirty = false;
	private boolean needsbuild = false;
	
	/**
	 * The main configuration block for the page
	 */
	private ApiProfilesConfigurationBlock block = null;
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH, 0, 0);
		SWTFactory.createWrapLabel(comp, PreferenceMessages.ApiProfilesPreferencePage_0, 2, 200);
		SWTFactory.createVerticalSpacer(comp, 1);
		
		Composite lcomp = SWTFactory.createComposite(comp, 2, 1, GridData.FILL_BOTH, 0, 0);
		SWTFactory.createWrapLabel(lcomp, PreferenceMessages.ApiProfilesPreferencePage_1, 2);
		Table table = new Table(lcomp, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER | SWT.CHECK);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		tableviewer = new CheckboxTableViewer(table);
		tableviewer.setLabelProvider(new ProfileLabelProvider());
		tableviewer.setContentProvider(new ArrayContentProvider());
		tableviewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection ss = (IStructuredSelection) event.getSelection();
				doEdit((IApiBaseline) ss.getFirstElement());
			}
		});
		tableviewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				IApiBaseline profile = (IApiBaseline) event.getElement();
				if(event.getChecked()) {
					tableviewer.setCheckedElements(new Object[] {profile});
					newdefault = profile.getName();
				}
				else {
					newdefault = null;
					manager.setDefaultApiProfile(null);
				}
				rebuildcount = 0;
				tableviewer.refresh(true);
				dirty = true;
			}
		});
		tableviewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IApiBaseline[] state = getCurrentSelection();
				removebutton.setEnabled(state.length > 0);
				editbutton.setEnabled(state.length == 1);
			}
		});
		tableviewer.setComparator(new ViewerComparator() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				return ((IApiBaseline)e1).getName().compareTo(((IApiBaseline)e2).getName());
			}
		});
		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			public void run() {	
				backingcollection.addAll(Arrays.asList(manager.getApiProfiles()));
				tableviewer.setInput(backingcollection);
			}
		});
		Composite bcomp = SWTFactory.createComposite(lcomp, 1, 1, GridData.FILL_VERTICAL | GridData.VERTICAL_ALIGN_BEGINNING, 0, 0);
		newbutton = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiProfilesPreferencePage_2, null);
		newbutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ApiProfileWizard wizard = new ApiProfileWizard(null);
				WizardDialog dialog = new WizardDialog(ApiUIPlugin.getShell(), wizard);
				if(dialog.open() == IDialogConstants.OK_ID) {
					IApiBaseline profile = wizard.getProfile();
					if(profile != null) {
						backingcollection.add(profile);
						tableviewer.refresh();
						tableviewer.setSelection(new StructuredSelection(profile), true);
						if(backingcollection.size() == 1) {
							newdefault = profile.getName();
							tableviewer.setCheckedElements(new Object[] {profile});
							tableviewer.refresh(profile);
						}
						
						dirty = true;
					}
				}
			}
		});
		editbutton = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiProfilesPreferencePage_4, null);
		editbutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doEdit((IApiBaseline)getCurrentSelection()[0]);
			}
		});
		editbutton.setEnabled(false);
		removebutton = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiProfilesPreferencePage_3, null);
		removebutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IApiBaseline[] states = getCurrentSelection();
				for(int i = 0; i < states.length; i++) {
					if(isDefault(states[i])) {
						newdefault = null;
						rebuildcount = 0;
					}
					removed.add(states[i].getName());
				}
				if(backingcollection.removeAll(Arrays.asList(states))) {
					dirty = true;
				}
				tableviewer.refresh();
			}
		});
		removebutton.setEnabled(false);
		IApiBaseline profile = manager.getDefaultApiProfile();
		origdefault = newdefault = (profile == null ? null : profile.getName());
		initialize();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(comp, IApiToolsHelpContextIds.APIPROFILES_PREF_PAGE);

		block = new ApiProfilesConfigurationBlock((IWorkbenchPreferenceContainer)getContainer());
		block.createControl(comp, this);
		SWTFactory.createVerticalSpacer(parent, 1);
		Dialog.applyDialogFont(comp);
		return comp;
	}
	
	/**
	 * Returns if the {@link IApiBaseline} with the given name has been removed, but not yet committed back to the manager
	 * @param name the name of the {@link IApiBaseline}
	 * @return true if the {@link IApiBaseline} has been removed from the page, false otherwise
	 */
	public static boolean isRemovedBaseline(String name) {
		return removed.contains(name);
	}
	
	/**
	 * Performs the edit operation for the edit button and the double click listener for the table
	 * @param profile
	 */
	protected void doEdit(final IApiBaseline profile) {
		ApiProfileWizard wizard = new ApiProfileWizard(profile);
		WizardDialog dialog = new WizardDialog(ApiUIPlugin.getShell(), wizard);
		if(dialog.open() == IDialogConstants.OK_ID) {
			IApiBaseline newprofile = wizard.getProfile();
			if(newprofile != null) {
				//clear any pending edit updates
				removed.add(profile.getName());
				backingcollection.remove(profile);
				backingcollection.add(newprofile);
				tableviewer.refresh();
				if(isDefault(profile)) {
					tableviewer.setCheckedElements(new Object[] {newprofile});
					tableviewer.setSelection(new StructuredSelection(newprofile), true);
					newdefault = newprofile.getName();
					rebuildcount = 0;
					needsbuild = true;
					tableviewer.refresh(true);
				}
				dirty = true;
			}
		}
	}
	
	/**
	 * updates the buttons on the page
	 */
	protected void initialize() {
		IApiBaseline def = ApiPlugin.getDefault().getApiProfileManager().getDefaultApiProfile();
		if(def != null) {
			tableviewer.setCheckedElements(new Object[] {def});
		}
	}
	
	/**
	 * Returns if the specified {@link IApiBaseline} is the default profile or not
	 * @param element
	 * @return if the profile is the default or not
	 */
	protected boolean isDefault(Object element) {
		if(element instanceof IApiBaseline) {
			IApiBaseline profile = (IApiBaseline) element;
			if(newdefault == null) {
				IApiBaseline def = ApiPlugin.getDefault().getApiProfileManager().getDefaultApiProfile();
				if(def != null) {
					return profile.getName().equals(def.getName());
				}
			}
			else {
				return profile.getName().equals(newdefault);
			}
		}
		return false;
	}
	
	/**
	 * @return the current selection from the table viewer
	 */
	protected IApiBaseline[] getCurrentSelection() {
		IStructuredSelection ss = (IStructuredSelection) tableviewer.getSelection();
		if(ss.isEmpty()) {
			return new IApiBaseline[0];
		}
		return (IApiBaseline[]) ((IStructuredSelection) tableviewer.getSelection()).toList().toArray(new IApiBaseline[ss.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performCancel()
	 */
	public boolean performCancel() {
		manager.setDefaultApiProfile(origdefault);
		backingcollection.clear();
		removed.clear();
		if(this.block != null) {
			this.block.performCancel();
		}
		return super.performCancel();
	}

	/**
	 * Applies the changes from the current change set to the {@link ApiBaselineManager}. When done
	 * the current change set is cleared.
	 */
	protected void applyChanges() {
		if(!dirty) {
			return;
		}
		//remove 
		for(Iterator iter = removed.iterator(); iter.hasNext();) {
			manager.removeApiProfile((String) iter.next());
		}
		//add the new / changed ones
		for(Iterator iter = backingcollection.iterator(); iter.hasNext();) {
			manager.addApiProfile((IApiBaseline) iter.next());
		}
		IApiBaseline def = ApiPlugin.getDefault().getApiProfileManager().getDefaultApiProfile();
		if(def != null && !def.getName().equals(newdefault)) {
			manager.setDefaultApiProfile(newdefault);
			needsbuild = true;
		}
		else if(def == null) {
			manager.setDefaultApiProfile(newdefault);
			needsbuild = true;
		}
		if(needsbuild) {
			if(rebuildcount < 1) {
				rebuildcount++;
				IProject[] projects = Util.getApiProjects();
				//do not even ask if there are no projects to build
				if (projects != null) {
					if(MessageDialog.openQuestion(getShell(), PreferenceMessages.ApiProfilesPreferencePage_6, PreferenceMessages.ApiProfilesPreferencePage_7)) {
						Util.getBuildJob(projects).schedule();
					}
				}
			}
		}
		origdefault = newdefault;
		dirty = false;
		removed.clear();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	public boolean performOk() {
		this.block.performOK();
		applyChanges();
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performApply()
	 */
	protected void performApply() {
		this.block.performApply();
		applyChanges();
	}
}
