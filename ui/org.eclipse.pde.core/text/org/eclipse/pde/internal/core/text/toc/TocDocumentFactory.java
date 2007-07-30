/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.core.text.toc;

import org.eclipse.pde.internal.core.itoc.ITocConstants;
import org.eclipse.pde.internal.core.text.DocumentNodeFactory;
import org.eclipse.pde.internal.core.text.IDocumentNode;
import org.eclipse.pde.internal.core.text.IDocumentNodeFactory;

/**
 * TocDocumentFactory
 *
 */
public class TocDocumentFactory extends DocumentNodeFactory implements IDocumentNodeFactory
{	private TocModel fModel;
	
	/**
	 * @param model
	 */
	public TocDocumentFactory(TocModel model) {
		super();
		fModel = model;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.core.text.IDocumentNodeFactory#createDocumentNode(java.lang.String, org.eclipse.pde.internal.core.text.IDocumentNode)
	 */
	public IDocumentNode createDocumentNode(String name, IDocumentNode parent) {

		// Semantics:
		// org.eclipse.platform.doc.isv/reference/extension-points/org_eclipse_help_toc.html
		
		// TODO: MP: TEO: Parent is not needed as it is set in the DocumentHandler
		// TODO: MP: TEO: Could delegate to model classes to do creation?
		// TODO: MP: TEO: Enforce model validity rules? Do not read in extraneous elements?
		// Note: Cannot return null
		// TODO: MP: TEO:  Change to interfaces for checking instance of and cast

		if (isToc(name))
		{	// Root
			return (IDocumentNode)createToc();
		}

		if (isTopic(name))
		{	// Topic
			return (IDocumentNode)createTocTopic();
		}

		if (isLink(name))
		{	// Link
			return (IDocumentNode)createTocLink();
		}			

		if (isAnchor(name))
		{	// Anchor
			return (IDocumentNode)createTocAnchor();
		}

		if(isEnablement(name))
		{	// Enablement
			return (IDocumentNode)createTocEnablement();
		}

		return super.createDocumentNode(name, parent);
	}

	/**
	 * @param name
	 * @param elementName
	 * @return
	 */
	private boolean isTocElement(String name, String elementName) {
		if (name.equals(elementName)) {
			return true;
		}
		return false;
	}
	
	/**
	 * @param name
	 * @return
	 */
	private boolean isToc(String name) {
		return isTocElement(name, ITocConstants.ELEMENT_TOC);
	}
	
	/**
	 * @param name
	 * @return
	 */
	private boolean isAnchor(String name) {
		return isTocElement(name, ITocConstants.ELEMENT_ANCHOR);
	}
	
	/**
	 * @param name
	 * @return
	 */
	private boolean isTopic(String name) {
		return isTocElement(name, ITocConstants.ELEMENT_TOPIC);
	}	
	
	/**
	 * @param name
	 * @return
	 */
	private boolean isLink(String name) {
		return isTocElement(name, ITocConstants.ELEMENT_LINK);
	}	
	
	/**
	 * @param name
	 * @return
	 */
	private boolean isEnablement(String name) {
		return isTocElement(name, ITocConstants.ELEMENT_ENABLEMENT);
	}		
	
	/**
	 * @return
	 */
	public Toc createToc() {
		return new Toc(fModel);
	}

	/**
	 * @param parent
	 * @return
	 */
	public TocTopic createTocTopic() {
		return new TocTopic(fModel);
	}

	/**
	 * @param parent
	 * @return
	 */
	public TocLink createTocLink() {
		return new TocLink(fModel);
	}

	/**
	 * @return
	 */
	public TocAnchor createTocAnchor() {
		return new TocAnchor(fModel);
	}

	/**
	 * @return
	 */
	public TocEnablement createTocEnablement() {
		return new TocEnablement(fModel);
	}
}
