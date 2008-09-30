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
package org.eclipse.pde.api.tools.model.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.Signature;
import org.eclipse.pde.api.tools.internal.ApiSettingsXmlVisitor;
import org.eclipse.pde.api.tools.internal.IApiCoreConstants;
import org.eclipse.pde.api.tools.internal.model.ApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.RestrictionModifiers;
import org.eclipse.pde.api.tools.internal.provisional.VisibilityModifiers;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IPackageDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IReferenceTypeDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiDescription;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.scanner.ApiDescriptionProcessor;
import org.eclipse.pde.api.tools.internal.util.Util;

/**
 * Tests API manifest implementation.
 * 
 * @since 1.0.0
 */
public class ApiDescriptionTests extends TestCase {
	
	private IApiDescription fManifest = buildManifest();
	
	/**
	 * Wraps an element with its API description
	 */
	class ElementDescription {
		
		public IElementDescriptor fElement;
		public int fVis, fRes;
		public String fComponent = null;
		
		public ElementDescription(IElementDescriptor element, int visibility, int restrictions) {
			fElement = element;
			fVis = visibility;
			fRes = restrictions;
		}
		
		public ElementDescription(String componentContext, IElementDescriptor element, int visibility, int restrictions) {
			this(element, visibility, restrictions);
			fComponent = componentContext;
		}
	}

	/**
	 * Creates and returns a container for an element and expected API settings.
	 * 
	 * @param element
	 * @param visibility
	 * @param restrictions
	 * @return
	 */
	public ElementDescription newDescription(IElementDescriptor element, int visibility, int restrictions) {
		return new ElementDescription(element, visibility, restrictions);
	}
	
	/**
	 * Creates a new empty API component description, not owned by any component.
	 * 
	 * @return
	 */
	protected IApiDescription newDescription() {
		return new ApiDescription(null);
	}
	
	/**
	 * Builds a test manifest with the following information:
	 * 
	 * default package: API
	 * 		class A
	 * 		class B 		- @noinstantiate
	 * 			method m1 	- @noextend
	 * 		class C 		- @noinstantiate @noextend
	 * 		class D 		- @noreference
	 * 			field f1 	- @noreference
	 * 		interface IA
	 * 		interface IB 	- @noimplement
	 * package a.b.c: API
	 * 		class A 		- @noinstantiate @noextend
	 * 			method m2 	- @noreference
	 * 		class B
	 * 		class C			- @noextend
	 * 		class D 		- @noinstantiate
	 * 			field f2 	- @noreference
	 * 		interface IC 	- @noimplement
	 * 		interface ID
	 * package a.b.c.spi: API
	 * 		class SpiA
	 * 		class SpiB 		- @noextend
	 * 			method m3
	 * 		class SpiC 		- @noinstantiate
	 * 			field f4	- @noreference
	 * 			method m4	- @noextend
	 * 		class SpiD 		- @noextend @noinstantiate
	 * 		class SpiE 		- @noreference
	 * 			field f3
	 * 		interface ISpiA
	 * 		interface ISpiB - @noimplement
	 * package a.b.c.internal: PRIVATE
	 * 		class PA
	 * 		class PB
	 * 		class PC
	 * 		class PD
	 * 
	 * 
	 * @return
	 */
	protected IApiDescription buildManifest() {
		IApiDescription manifest = newDescription();
		// add packages to the manifest with default rules - public API
		manifest.setVisibility(Factory.packageDescriptor(""), VisibilityModifiers.API);
		manifest.setVisibility(Factory.packageDescriptor("a.b.c"), VisibilityModifiers.API);
		manifest.setVisibility(Factory.packageDescriptor("a.b.c.spi"), VisibilityModifiers.SPI);
		manifest.setVisibility(Factory.packageDescriptor("a.b.c.internal"), VisibilityModifiers.PRIVATE);
		
		// add type specific settings
		manifest.setRestrictions(Factory.typeDescriptor("B"), RestrictionModifiers.NO_INSTANTIATE);
		manifest.setRestrictions(Factory.typeDescriptor("C"), RestrictionModifiers.NO_EXTEND | RestrictionModifiers.NO_INSTANTIATE);
		manifest.setRestrictions(Factory.typeDescriptor("D"), RestrictionModifiers.NO_REFERENCE);
		manifest.setRestrictions(Factory.typeDescriptor("IB"), RestrictionModifiers.NO_IMPLEMENT);
		
		manifest.setRestrictions(Factory.typeDescriptor("a.b.c.A"), RestrictionModifiers.NO_EXTEND | RestrictionModifiers.NO_INSTANTIATE);
		manifest.setRestrictions(Factory.typeDescriptor("a.b.c.C"), RestrictionModifiers.NO_EXTEND);
		manifest.setRestrictions(Factory.typeDescriptor("a.b.c.D"), RestrictionModifiers.NO_INSTANTIATE);
		manifest.setRestrictions(Factory.typeDescriptor("a.b.c.IC"), RestrictionModifiers.NO_IMPLEMENT);
		
		manifest.setRestrictions(Factory.typeDescriptor("a.b.c.spi.SpiB"), RestrictionModifiers.NO_EXTEND);
		manifest.setRestrictions(Factory.typeDescriptor("a.b.c.spi.SpiC"), RestrictionModifiers.NO_INSTANTIATE);
		manifest.setRestrictions(Factory.typeDescriptor("a.b.c.spi.SpiD"), RestrictionModifiers.NO_EXTEND | RestrictionModifiers.NO_INSTANTIATE);
		manifest.setRestrictions(Factory.typeDescriptor("a.b.c.spi.SpiE"), RestrictionModifiers.NO_REFERENCE);
		manifest.setRestrictions(Factory.typeDescriptor("a.b.c.spi.ISpiB"), RestrictionModifiers.NO_IMPLEMENT);		
		
		//add method specific settings
		manifest.setRestrictions(Factory.methodDescriptor("B", "m1", Signature.createMethodSignature(new String[0], Signature.SIG_VOID)), RestrictionModifiers.NO_OVERRIDE);
		manifest.setRestrictions(Factory.methodDescriptor("a.b.c.A","m2", Signature.createMethodSignature(new String[0], Signature.SIG_VOID)), RestrictionModifiers.NO_REFERENCE);
		manifest.setRestrictions(Factory.methodDescriptor("a.b.c.spi.SpiB","m3", Signature.createMethodSignature(new String[0], Signature.SIG_VOID)), RestrictionModifiers.NO_RESTRICTIONS);
		manifest.setRestrictions(Factory.methodDescriptor("a.b.c.spi.SpiC", "m4", Signature.createMethodSignature(new String[0], Signature.SIG_VOID)), RestrictionModifiers.NO_OVERRIDE);
		
		//add field specific settings
		manifest.setRestrictions(Factory.fieldDescriptor("D", "f1"), RestrictionModifiers.NO_REFERENCE);
		manifest.setRestrictions(Factory.fieldDescriptor("a.b.c.D","f2"), RestrictionModifiers.NO_REFERENCE);
		manifest.setRestrictions(Factory.fieldDescriptor("a.b.c.spi.SpiD","f3"), RestrictionModifiers.NO_RESTRICTIONS);
		manifest.setRestrictions(Factory.fieldDescriptor("a.b.c.spi.SpiC", "f4"), RestrictionModifiers.NO_REFERENCE);
		
		return manifest;
	}

	/**
	 * used to build a manifest that can be changed
	 */
	protected IApiDescription buildModifiableManifest() {
		IApiDescription desc = newDescription();
		desc.setVisibility(Factory.packageDescriptor("a.b.c"), VisibilityModifiers.API);
		desc.setVisibility(Factory.packageDescriptor(""), VisibilityModifiers.SPI);
		IElementDescriptor element = Factory.typeDescriptor("C"); 
		desc.setRestrictions(element, RestrictionModifiers.NO_EXTEND | RestrictionModifiers.NO_INSTANTIATE);
		desc.setVisibility(element, VisibilityModifiers.PRIVATE);
		
		element = Factory.typeDescriptor("a.b.c.D");
		desc.setRestrictions(element, RestrictionModifiers.NO_REFERENCE);
		desc.setVisibility(element, VisibilityModifiers.PRIVATE);
		
		element = Factory.methodDescriptor("C", "m1", Signature.createMethodSignature(new String[0], Signature.SIG_VOID));
		desc.setRestrictions(element, RestrictionModifiers.NO_OVERRIDE);
		desc.setVisibility(element, VisibilityModifiers.PRIVATE);
		
		element = Factory.methodDescriptor("a.b.c.A","m2", Signature.createMethodSignature(new String[0], Signature.SIG_VOID));
		desc.setRestrictions(element, RestrictionModifiers.NO_REFERENCE);
		desc.setVisibility(element, VisibilityModifiers.PRIVATE);
		
		element = Factory.fieldDescriptor("D", "f1");
		desc.setRestrictions(element, RestrictionModifiers.NO_REFERENCE);
		desc.setVisibility(element, VisibilityModifiers.PRIVATE);
		
		element = Factory.fieldDescriptor("a.b.c","f2");
		desc.setRestrictions(element, RestrictionModifiers.NO_REFERENCE);
		desc.setVisibility(element, VisibilityModifiers.PRIVATE);
		return desc;
	}
	
	/**
	 * Tests visiting types in the manually created manifest
	 */
	public void testVisitTypes() {
		IApiDescription manifest = buildManifest();
		doVisitTypes(manifest);
	}
	
	/**
	 * Tests restoring API settings from component XML. These settings are not quite
	 * as rich as we have in the usual baseline (no notion of SPI package, etc).
	 * 
	 * We expect a component with the following information:
	 * 
	 * default package: API
	 * 		class A
	 * 		class B 		- @noinstantiate
	 * 			method m1 	- @noextend
	 * 		class C 		- @noinstantiate @noextend
	 * 		class D 		- @noreference
	 * 			field f1 	- @noreference
	 * 		interface IA
	 * 		interface IB 	- @noimplement
	 * package a.b.c: API
	 * 		class A 		- @noinstantiate @noextend
	 * 			method m2 	- @noreference
	 * 		class B
	 * 		class C			- @noextend
	 * 		class D 		- @noinstantiate
	 * 			field f2 	- @noreference
	 * 		interface IC 	- @noimplement
	 * 		interface ID
	 * package a.b.c.spi: API
	 * 		class SpiA
	 * 		class SpiB 		- @noextend
	 * 			method m3
	 * 		class SpiC 		- @noinstantiate
	 * 			field f4	- @noreference
	 * 			method m4	- @noextend
	 * 		class SpiD 		- @noextend @noinstantiate
	 * 		class SpiE 		- @noreference
	 * 			field f3
	 * 		interface ISpiA
	 * 		interface ISpiB - @noimplement
	 * package a.b.c.internal: PRIVATE
	 * 		class PA
	 * 		class PB
	 * 		class PC
	 * 		class PD
	 * 
	 * package a.b.c.internal has API visibility for component "a.friend"
	 * class D has SPI visibility for component "a.friend"
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 * @throws CoreException
	 */
	public void testRestoreFromXML() throws FileNotFoundException, IOException, CoreException {
		IPath path = TestSuiteHelper.getPluginDirectoryPath();
		path = path.append("test-xml");
		File file = path.toFile();
		assertTrue("Missing xml directory", file.exists());
		IApiBaseline baseline = TestSuiteHelper.newApiProfile("test", TestSuiteHelper.getEEDescriptionFile());
		IApiComponent component = baseline.newApiComponent(file.getAbsolutePath());
		baseline.addApiComponents(new IApiComponent[] { component });
		
		IPackageDescriptor defPkgDesc = Factory.packageDescriptor("");
		ElementDescription defPkg = new ElementDescription(defPkgDesc, VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS);
			ElementDescription B = new ElementDescription(defPkgDesc.getType("B"), VisibilityModifiers.API, RestrictionModifiers.NO_INSTANTIATE);
				ElementDescription m1 = new ElementDescription(defPkgDesc.getType("B").getMethod("m1", Signature.createMethodSignature(new String[0],Signature.SIG_VOID)), VisibilityModifiers.API, RestrictionModifiers.NO_OVERRIDE);
			ElementDescription C = new ElementDescription(defPkgDesc.getType("C"), VisibilityModifiers.API, RestrictionModifiers.NO_EXTEND | RestrictionModifiers.NO_INSTANTIATE);
			ElementDescription D = new ElementDescription(defPkgDesc.getType("D"), VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS);
				ElementDescription f1 = new ElementDescription(defPkgDesc.getType("D").getField("f1"), VisibilityModifiers.API, RestrictionModifiers.NO_REFERENCE);
			ElementDescription IB = new ElementDescription(defPkgDesc.getType("IB"), VisibilityModifiers.API, RestrictionModifiers.NO_IMPLEMENT);
		IPackageDescriptor abcPkgDesc = Factory.packageDescriptor("a.b.c");
		ElementDescription abcPkg = new ElementDescription(abcPkgDesc, VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS);
			ElementDescription abcA = new ElementDescription(abcPkgDesc.getType("A"), VisibilityModifiers.API, RestrictionModifiers.NO_EXTEND | RestrictionModifiers.NO_INSTANTIATE);
				ElementDescription abcAm2 = new ElementDescription(abcPkgDesc.getType("A").getMethod("m2", Signature.createMethodSignature(new String[0], Signature.SIG_VOID)), VisibilityModifiers.API, RestrictionModifiers.NO_REFERENCE);
			ElementDescription abcC = new ElementDescription(abcPkgDesc.getType("C"), VisibilityModifiers.API, RestrictionModifiers.NO_EXTEND);
			ElementDescription abcD = new ElementDescription(abcPkgDesc.getType("D"), VisibilityModifiers.API, RestrictionModifiers.NO_INSTANTIATE);
				ElementDescription abcDf2 = new ElementDescription(abcPkgDesc.getType("D").getField("f2"), VisibilityModifiers.API, RestrictionModifiers.NO_REFERENCE);
			ElementDescription abcIC = new ElementDescription(abcPkgDesc.getType("IC"), VisibilityModifiers.API, RestrictionModifiers.NO_IMPLEMENT);
		IPackageDescriptor spiPkgDesc = Factory.packageDescriptor("a.b.c.spi");
		ElementDescription spiPkg = new ElementDescription(spiPkgDesc, VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS);
			ElementDescription spiB = new ElementDescription(spiPkgDesc.getType("SpiB"), VisibilityModifiers.API, RestrictionModifiers.NO_EXTEND);
			ElementDescription spiC = new ElementDescription(spiPkgDesc.getType("SpiC"), VisibilityModifiers.API, RestrictionModifiers.NO_INSTANTIATE);
				ElementDescription spiCf4 = new ElementDescription(spiPkgDesc.getType("SpiC").getField("f4"), VisibilityModifiers.API, RestrictionModifiers.NO_REFERENCE);
				ElementDescription spiCm4 = new ElementDescription(spiPkgDesc.getType("SpiC").getMethod("m4", Signature.createMethodSignature(new String[0], Signature.SIG_VOID)), VisibilityModifiers.API, RestrictionModifiers.NO_OVERRIDE);
			ElementDescription spiD = new ElementDescription(spiPkgDesc.getType("SpiD"), VisibilityModifiers.API, RestrictionModifiers.NO_EXTEND | RestrictionModifiers.NO_INSTANTIATE);
			ElementDescription spiE = new ElementDescription(spiPkgDesc.getType("SpiE"), VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS);
			ElementDescription IspiB = new ElementDescription(spiPkgDesc.getType("ISpiB"), VisibilityModifiers.API, RestrictionModifiers.NO_IMPLEMENT);
		IPackageDescriptor intPkgDesc = Factory.packageDescriptor("a.b.c.internal");
		ElementDescription intPkg = new ElementDescription(intPkgDesc, VisibilityModifiers.PRIVATE, RestrictionModifiers.NO_RESTRICTIONS);
		
		final List<ElementDescription> visitOrder = new ArrayList<ElementDescription>();
		visitOrder.add(defPkg); // start def
			visitOrder.add(B); //start B
				visitOrder.add(m1); visitOrder.add(m1); //start / end m1
			visitOrder.add(B); // end B
			visitOrder.add(C); visitOrder.add(C); // start/end C
			visitOrder.add(D); // start D
				visitOrder.add(f1); visitOrder.add(f1); //start / end f1
			visitOrder.add(D); // end D
			visitOrder.add(IB); visitOrder.add(IB); // start/end IB
		visitOrder.add(defPkg); // end def
		visitOrder.add(abcPkg); // start a.b.c
			visitOrder.add(abcA); //a.b.c.start A 
				visitOrder.add(abcAm2); visitOrder.add(abcAm2); //start / end m2
			visitOrder.add(abcA); // end a.b.c.A
			visitOrder.add(abcC); visitOrder.add(abcC); // start/end a.b.c.C
			visitOrder.add(abcD); //start a.b.c.D
				visitOrder.add(abcDf2); visitOrder.add(abcDf2); //start /end f2
			visitOrder.add(abcD); // end a.b.c.D
			visitOrder.add(abcIC); visitOrder.add(abcIC); // start/end a.b.c.IC
		visitOrder.add(abcPkg); // end a.b.c
		visitOrder.add(intPkg); // start a.b.c.internal
		visitOrder.add(intPkg); // end a.b.c.internal
		visitOrder.add(spiPkg); // start a.b.c.spi
			visitOrder.add(IspiB); visitOrder.add(IspiB); // start/end ISpiB
			visitOrder.add(spiB); //start spiB
			visitOrder.add(spiB); // end SpiB
			visitOrder.add(spiC); //start SpiC
				visitOrder.add(spiCf4); visitOrder.add(spiCf4); //start/ end f4
				visitOrder.add(spiCm4); visitOrder.add(spiCm4); //start / end m4
			visitOrder.add(spiC); // end SpiC
			visitOrder.add(spiD); visitOrder.add(spiD); // start / end SpiD
			visitOrder.add(spiE); 
			visitOrder.add(spiE); // start/end SpiE
		visitOrder.add(spiPkg); // end a.b.c.spi
				
		ApiDescriptionVisitor visitor = new ApiDescriptionVisitor() {
			public boolean visitElement(IElementDescriptor element, IApiAnnotations description) {
				ElementDescription expected = visitOrder.remove(0);
				assertEquals("Wrong begin visit element", expected.fElement, element);
				assertEquals("Wrong begin visit visibility", expected.fVis, description.getVisibility());
				assertEquals("Wrong begin visit restrictions for ", expected.fRes, description.getRestrictions());
				return true;
			}
		
			public void endVisitElement(IElementDescriptor element, IApiAnnotations description) {
				ElementDescription expected = visitOrder.remove(0);
				assertEquals("Wrong end visit element", expected.fElement, element);
				assertEquals("Wrong end visit visibility", expected.fVis, description.getVisibility());
				assertEquals("Wrong end visit restrictions", expected.fRes, description.getRestrictions());
			}
		};
		
		component.getApiDescription().accept(visitor);
		
		assertEquals("Visit incomplete", 0, visitOrder.size());
		baseline.dispose();
	}
	
	/**
	 * Returns XML for the component's current API description.
	 * 
	 * @param apiComponent API component
	 * @return XML for the API description
	 * @throws CoreException if something goes terribly wrong 
	 */
	private String getApiDescriptionXML(IApiComponent apiComponent) throws CoreException {
		ApiSettingsXmlVisitor xmlVisitor = new ApiSettingsXmlVisitor(apiComponent);
		apiComponent.getApiDescription().accept(xmlVisitor);
		return xmlVisitor.getXML();
	}
	
	/**
	 * Reads XML from disk, annotates settings, then persists and re-creates settings
	 * to ensure we read/write equivalent XML.
	 *
	 * @throws CoreException
	 * @throws IOException
	 */
	public void testPersistRestoreXML() throws CoreException, IOException {
		// read XML into API settings
		IPath path = TestSuiteHelper.getPluginDirectoryPath();
		path = path.append("test-xml");
		File file = path.toFile();
		assertTrue("Missing xml directory", file.exists());
		File descfile = new File(file, IApiCoreConstants.API_DESCRIPTION_XML_NAME);
		String readXML = null;
		if (descfile.exists()) {
			FileInputStream stream = null;
			try {
				 stream = new FileInputStream(descfile);
				 char[] charArray = Util.getInputStreamAsCharArray(stream, -1, IApiCoreConstants.UTF_8);
				 readXML = new String(charArray);
			}
			finally {
				if(stream != null) {
					stream.close();
				}
			}
		}
		IApiDescription settings = new ApiDescription(null);
		ApiDescriptionProcessor.annotateApiSettings(null, settings, readXML);
		
		// write back to XML and then re-create
		IApiComponent component = TestSuiteHelper.createTestingApiComponent("test", "test", settings);
		String writeXML = getApiDescriptionXML(component);
		
		IApiDescription restored = new ApiDescription(null); 
		ApiDescriptionProcessor.annotateApiSettings(null, restored, writeXML);
		
		// compare the original and restore settings
		
		// build expected visit order from original
		final List<ElementDescription> visitOrder = new ArrayList<ElementDescription>();
		ApiDescriptionVisitor visitor = new ApiDescriptionVisitor() {
			/* (non-Javadoc)
			 * @see org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor#visitElement(org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor, org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations)
			 */
			public boolean visitElement(IElementDescriptor element, IApiAnnotations description) {
				visitOrder.add(new ElementDescription(null, element, description.getVisibility(), description.getRestrictions()));
				return super.visitElement(element, description);
			}
			/* (non-Javadoc)
			 * @see org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor#endVisitElement(org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor, org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations)
			 */
			public void endVisitElement(IElementDescriptor element, IApiAnnotations description) {
				visitOrder.add(new ElementDescription(null, element, description.getVisibility(), description.getRestrictions()));
				super.endVisitElement(element, description);
			}
		};
		settings.accept(visitor);
		
		// now visit the restored version and compare order
		visitor = new ApiDescriptionVisitor() {
			/* (non-Javadoc)
			 * @see org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor#visitElement(org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor, org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations)
			 */
			public boolean visitElement(IElementDescriptor element, IApiAnnotations description) {
				ElementDescription expected = visitOrder.remove(0);
				assertEquals("Wrong begin visit element", expected.fElement, element);
				assertEquals("Wrong begin visit component", expected.fComponent, null);
				assertEquals("Wrong begin visit visibility", expected.fVis, description.getVisibility());
				assertEquals("Wrong begin visit restrictions", expected.fRes, description.getRestrictions());
				return true;
			}

			/* (non-Javadoc)
			 * @see org.eclipse.pde.api.tools.internal.provisional.ApiDescriptionVisitor#endVisitElement(org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor, org.eclipse.pde.api.tools.internal.provisional.IApiAnnotations)
			 */
			public void endVisitElement(IElementDescriptor element, IApiAnnotations description) {
				ElementDescription expected = visitOrder.remove(0);
				assertEquals("Wrong end visit element", expected.fElement, element);
				assertEquals("Wrong end visit component", expected.fComponent, null);
				assertEquals("Wrong end visit visibility", expected.fVis, description.getVisibility());
				assertEquals("Wrong end visit restrictions", expected.fRes, description.getRestrictions());
			}
		};
		
		restored.accept(visitor);
		
		assertEquals("Visit incomplete", 0, visitOrder.size());		
	}
	
	/**
	 * Test visiting types
	 */
	protected void doVisitTypes(IApiDescription manifest) {
		IPackageDescriptor defPkgDesc= Factory.packageDescriptor("");
		ElementDescription defPkg = new ElementDescription(defPkgDesc, VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS);
			ElementDescription B = new ElementDescription(defPkgDesc.getType("B"), VisibilityModifiers.API, RestrictionModifiers.NO_INSTANTIATE);
			ElementDescription m1 = new ElementDescription(defPkgDesc.getType("B").getMethod("m1", Signature.createMethodSignature(new String[0], Signature.SIG_VOID)), VisibilityModifiers.API, RestrictionModifiers.NO_OVERRIDE);
			ElementDescription C = new ElementDescription(defPkgDesc.getType("C"), VisibilityModifiers.API, RestrictionModifiers.NO_EXTEND | RestrictionModifiers.NO_INSTANTIATE);
			ElementDescription D = new ElementDescription(defPkgDesc.getType("D"), VisibilityModifiers.API, RestrictionModifiers.NO_REFERENCE);
				ElementDescription f1 = new ElementDescription(defPkgDesc.getType("D").getField("f1"), VisibilityModifiers.API, RestrictionModifiers.NO_REFERENCE);
			ElementDescription IB = new ElementDescription(defPkgDesc.getType("IB"), VisibilityModifiers.API, RestrictionModifiers.NO_IMPLEMENT);
		IPackageDescriptor abcPkgDesc = Factory.packageDescriptor("a.b.c");
		ElementDescription abcPkg = new ElementDescription(abcPkgDesc, VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS);
			ElementDescription abcA = new ElementDescription(abcPkgDesc.getType("A"), VisibilityModifiers.API, RestrictionModifiers.NO_EXTEND | RestrictionModifiers.NO_INSTANTIATE);
			ElementDescription abcAm2 = new ElementDescription(abcPkgDesc.getType("A").getMethod("m2", Signature.createMethodSignature(new String[0], Signature.SIG_VOID)), VisibilityModifiers.API, RestrictionModifiers.NO_REFERENCE);
			ElementDescription abcC = new ElementDescription(abcPkgDesc.getType("C"), VisibilityModifiers.API, RestrictionModifiers.NO_EXTEND);
			ElementDescription abcD = new ElementDescription(abcPkgDesc.getType("D"), VisibilityModifiers.API, RestrictionModifiers.NO_INSTANTIATE);
				ElementDescription abcDf2 = new ElementDescription(abcPkgDesc.getType("D").getField("f2"), VisibilityModifiers.API, RestrictionModifiers.NO_REFERENCE);
			ElementDescription abcIC = new ElementDescription(abcPkgDesc.getType("IC"), VisibilityModifiers.API, RestrictionModifiers.NO_IMPLEMENT);
		IPackageDescriptor spiPkgDesc = Factory.packageDescriptor("a.b.c.spi");
		ElementDescription spiPkg = new ElementDescription(spiPkgDesc, VisibilityModifiers.SPI, RestrictionModifiers.NO_RESTRICTIONS);
			ElementDescription spiB = new ElementDescription(spiPkgDesc.getType("SpiB"), VisibilityModifiers.SPI, RestrictionModifiers.NO_EXTEND);
			ElementDescription spiBm3 = new ElementDescription(spiPkgDesc.getType("SpiB").getMethod("m3", Signature.createMethodSignature(new String[0], Signature.SIG_VOID)), VisibilityModifiers.SPI, RestrictionModifiers.NO_RESTRICTIONS);
			ElementDescription spiC = new ElementDescription(spiPkgDesc.getType("SpiC"), VisibilityModifiers.SPI, RestrictionModifiers.NO_INSTANTIATE);
				ElementDescription spiCf4 = new ElementDescription(spiPkgDesc.getType("SpiC").getField("f4"), VisibilityModifiers.SPI, RestrictionModifiers.NO_REFERENCE);
				ElementDescription spiCm4 = new ElementDescription(spiPkgDesc.getType("SpiC").getMethod("m4", Signature.createMethodSignature(new String[0], Signature.SIG_VOID)), VisibilityModifiers.SPI, RestrictionModifiers.NO_OVERRIDE);
			ElementDescription spiD = new ElementDescription(spiPkgDesc.getType("SpiD"), VisibilityModifiers.SPI, RestrictionModifiers.NO_EXTEND | RestrictionModifiers.NO_INSTANTIATE);
				ElementDescription spiDf3 = new ElementDescription(spiPkgDesc.getType("SpiD").getField("f3"), VisibilityModifiers.SPI, RestrictionModifiers.NO_RESTRICTIONS);
			ElementDescription spiE = new ElementDescription(spiPkgDesc.getType("SpiE"), VisibilityModifiers.SPI, RestrictionModifiers.NO_REFERENCE);
			ElementDescription IspiB = new ElementDescription(spiPkgDesc.getType("ISpiB"), VisibilityModifiers.SPI, RestrictionModifiers.NO_IMPLEMENT);
		IPackageDescriptor intPkgDesc = Factory.packageDescriptor("a.b.c.internal");
		ElementDescription intPkg = new ElementDescription(intPkgDesc, VisibilityModifiers.PRIVATE, RestrictionModifiers.NO_RESTRICTIONS);
		
		final List<ElementDescription> visitOrder = new ArrayList<ElementDescription>();
		visitOrder.add(defPkg); // start def
			visitOrder.add(B); //start B
				visitOrder.add(m1); visitOrder.add(m1); //start / end m1
			visitOrder.add(B); // end B
			visitOrder.add(C); visitOrder.add(C); // start/end C
			visitOrder.add(D); // start D
				visitOrder.add(f1); visitOrder.add(f1); //start / end f1
			visitOrder.add(D); // end D
			visitOrder.add(IB); visitOrder.add(IB); // start/end IB
		visitOrder.add(defPkg); // end def
		visitOrder.add(abcPkg); // start a.b.c
			visitOrder.add(abcA); //a.b.c.start A 
				visitOrder.add(abcAm2); visitOrder.add(abcAm2); //start /end m2
			visitOrder.add(abcA); // end a.b.c.A
			visitOrder.add(abcC); visitOrder.add(abcC); // start/end a.b.c.C
			visitOrder.add(abcD); //start a.b.c.D
				visitOrder.add(abcDf2); visitOrder.add(abcDf2); //start / end f2
			visitOrder.add(abcD); // end a.b.c.D
			visitOrder.add(abcIC); visitOrder.add(abcIC); // start/end a.b.c.IC
		visitOrder.add(abcPkg); // end a.b.c
		visitOrder.add(intPkg); // start a.b.c.internal
		visitOrder.add(intPkg); // end a.b.c.internal
		visitOrder.add(spiPkg); // start a.b.c.spi
			visitOrder.add(IspiB); visitOrder.add(IspiB); // start/end ISpiB
			visitOrder.add(spiB); //start spiB
				visitOrder.add(spiBm3); visitOrder.add(spiBm3); //start / end m3
			visitOrder.add(spiB); // end SpiB
			visitOrder.add(spiC); //start SpiC
				visitOrder.add(spiCf4); visitOrder.add(spiCf4); //start / end f4
				visitOrder.add(spiCm4); visitOrder.add(spiCm4); //start / end f4
			visitOrder.add(spiC); // end SpiC
			visitOrder.add(spiD); //start SpiD
				visitOrder.add(spiDf3); visitOrder.add(spiDf3); //start / end f3
			visitOrder.add(spiD); // end SpiD
			visitOrder.add(spiE); visitOrder.add(spiE); // start/end SpiE
		visitOrder.add(spiPkg); // end a.b.c.spi
				
		ApiDescriptionVisitor visitor = new ApiDescriptionVisitor() {
			public boolean visitElement(IElementDescriptor element, IApiAnnotations description) {
				ElementDescription expected = visitOrder.remove(0);
				assertEquals("Wrong begin visit element", expected.fElement, element);
				assertEquals("Wrong begin visit visibility", expected.fVis, description.getVisibility());
				assertEquals("Wrong begin visit restrictions", expected.fRes, description.getRestrictions());
				return true;
			}
		
			public void endVisitElement(IElementDescriptor element, IApiAnnotations description) {
				ElementDescription expected = visitOrder.remove(0);
				assertEquals("Wrong end visit element", expected.fElement, element);
				assertEquals("Wrong end visit visibility", expected.fVis, description.getVisibility());
				assertEquals("Wrong end visit restrictions", expected.fRes, description.getRestrictions());
			}
		};
		
		manifest.accept(visitor);
		
		assertEquals("Visit incomplete", 0, visitOrder.size());
	}

	/**
	 * Tests visiting packages
	 */
	public void testVisitPackages() {
		ElementDescription defPkg = new ElementDescription(Factory.packageDescriptor(""), VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS);
		ElementDescription abcPkg = new ElementDescription(Factory.packageDescriptor("a.b.c"), VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS);
		ElementDescription spiPkg = new ElementDescription(Factory.packageDescriptor("a.b.c.spi"), VisibilityModifiers.SPI, RestrictionModifiers.NO_RESTRICTIONS);
		//ElementDescription spiPkgForNoFriend = new ElementDescription("no.friend", Factory.packageDescriptor("a.b.c.spi"), VisibilityModifiers.PRIVATE, RestrictionModifiers.NO_RESTRICTIONS);
		ElementDescription intPkg = new ElementDescription(Factory.packageDescriptor("a.b.c.internal"), VisibilityModifiers.PRIVATE, RestrictionModifiers.NO_RESTRICTIONS);
		//ElementDescription intPkgForFriend = new ElementDescription("a.friend", Factory.packageDescriptor("a.b.c.internal"), VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS);
		
		final List<ElementDescription> visitOrder = new ArrayList<ElementDescription>();
		visitOrder.add(defPkg); // start def
		visitOrder.add(defPkg); // end def
		visitOrder.add(abcPkg); // start a.b.c
		visitOrder.add(abcPkg); // end a.b.c
		visitOrder.add(intPkg); // start a.b.c.internal
		visitOrder.add(intPkg); // end a.b.c.internal
		visitOrder.add(spiPkg); // start a.b.c.spi
		visitOrder.add(spiPkg); // end a.b.c.spi
				
		ApiDescriptionVisitor visitor = new ApiDescriptionVisitor() {
			public boolean visitElement(IElementDescriptor element, IApiAnnotations description) {
				ElementDescription expected = visitOrder.remove(0);
				assertEquals("Wrong begin visit element", expected.fElement, element);
				assertEquals("Wrong begin visit visibility", expected.fVis, description.getVisibility());
				assertEquals("Wrong begin visit restrictions", expected.fRes, description.getRestrictions());
				return false;
			}
		
			public void endVisitElement(IElementDescriptor element, IApiAnnotations description) {
				ElementDescription expected = visitOrder.remove(0);
				assertEquals("Wrong end visit element", expected.fElement, element);
				assertEquals("Wrong end visit visibility", expected.fVis, description.getVisibility());
				assertEquals("Wrong end visit restrictions", expected.fRes, description.getRestrictions());
			}
		};
		
		IApiDescription manifest = buildManifest();
		manifest.accept(visitor);
		
		assertEquals("Visit incomplete", 0, visitOrder.size());
	}	
	
	/**
	 * Test for bug 209335, where an element is not in the component map for an {@link ApiDescription},
	 * and not performing an insertion for missing elements throws an NPE
	 */
	public void test209335() {
		String typename = "x.y.z.209335";
		String packageName = Util.getPackageName(typename);
		String tName = Util.getTypeName(typename);
		IReferenceTypeDescriptor type = Factory.packageDescriptor(packageName).getType(tName);
		IApiAnnotations description = fManifest.resolveAnnotations(type);
		assertTrue("The description must be null", description == null);
	}
	
	/**
	 * Resolves API description for a type with the given name.
	 *  
	 * @param typeName fully qualified name of referenced type
	 * @param expectedVisibility expected visibility modifiers
	 * @param expectedRestrictions expected visibility restriction modifiers
	 */
	protected void resolveType(String typeName, int expectedVisibility, int expectedRestrictions) {
		String packageName = Util.getPackageName(typeName);
		String tName = Util.getTypeName(typeName);
		IReferenceTypeDescriptor type = Factory.packageDescriptor(packageName).getType(tName);
		IApiAnnotations description = fManifest.resolveAnnotations(type);
		assertEquals("Wrong visibility", expectedVisibility, description.getVisibility());
		assertEquals("Wrong restrictions", expectedRestrictions, description.getRestrictions());
	}
		
	/**
	 * Tests API description: A = API with no restrictions.
	 * Note that 'A' has not been added to the manifest
	 */
	public void testADefPkg() {
		resolveType("A", VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS);
	}
	
	/**
	 * Tests API description: B = API with no instantiate.
	 */
	public void testBDefPkg() {
		resolveType("B", VisibilityModifiers.API, RestrictionModifiers.NO_INSTANTIATE);
	}
	
	/**
	 * Tests API description: C = API with no instantiate, no subclass.
	 */
	public void testCDefPkg() {
		resolveType("C", VisibilityModifiers.API, RestrictionModifiers.NO_EXTEND | RestrictionModifiers.NO_INSTANTIATE);
	}	
	
	/**
	 * Tests API description: D = API with no reference.
	 */
	public void testDDefPkg() {
		resolveType("D", VisibilityModifiers.API, RestrictionModifiers.NO_REFERENCE);
	}		
		
	/**
	 * Tests API description: IA = API with no restrictions.
	 * Note that this type is not explicitly in the manifest.
	 */
	public void testIADefPkg() {
		resolveType("IA", VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS);
	}
	
	/**
	 * Tests API description: IB = API with no implement.
	 */
	public void testIBDefPkg() {
		resolveType("IB", VisibilityModifiers.API, RestrictionModifiers.NO_IMPLEMENT);
	}
	
	/**
	 * Tests API description: a.b.c.A = API with no instantiate, no subclass.
	 */
	public void testAApiPkg() {
		resolveType("a.b.c.A", VisibilityModifiers.API, RestrictionModifiers.NO_INSTANTIATE | RestrictionModifiers.NO_EXTEND);
	}
	
	/**
	 * Tests API description: a.b.c.B = API with no restrictions.
	 * Note that this type is not explicitly in the manifest.
	 */
	public void testBApiPkg() {
		resolveType("a.b.c.B", VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS);
	}		
	
	/**
	 * Tests API description: a.b.c.C = API with no subclass.
	 */
	public void testCApiPkg() {
		resolveType("a.b.c.C", VisibilityModifiers.API, RestrictionModifiers.NO_EXTEND);
	}
	
	/**
	 * Tests API description: a.b.c.D = API with no instantiate.
	 */
	public void testDApiPkg() {
		resolveType("a.b.c.D", VisibilityModifiers.API, RestrictionModifiers.NO_INSTANTIATE);
	}		
	
	/**
	 * Tests API description: a.b.c.IC = API with no implement.
	 */
	public void testICApiPkg() {
		resolveType("a.b.c.IC", VisibilityModifiers.API, RestrictionModifiers.NO_IMPLEMENT);
	}
	
	/**
	 * Tests API description: a.b.c.ID = API with no restrictions.
	 * Note that this type is not explicitly in the manifest.
	 */
	public void testIDApiPkg() {
		resolveType("a.b.c.ID", VisibilityModifiers.API, RestrictionModifiers.NO_RESTRICTIONS);
	}
	
	/**
	 * Tests API description: a.b.c.spi.SpiA = SPI with no restrictions.
	 * Note that this type is not explicitly in the manifest.
	 */
	public void testASpiPkg() {
		resolveType("a.b.c.spi.SpiA", VisibilityModifiers.SPI, RestrictionModifiers.NO_RESTRICTIONS);
	}
	
	/**
	 * Tests API description: a.b.c.spi.SpiB = SPI with no subclass.
	 */
	public void testBSpiPkg() {
		resolveType("a.b.c.spi.SpiB", VisibilityModifiers.SPI, RestrictionModifiers.NO_EXTEND);
	}
	
	/**
	 * Tests API description: a.b.c.spi.SpiC = SPI with no instantiate.
	 */
	public void testCSpiPkg() {
		resolveType("a.b.c.spi.SpiC", VisibilityModifiers.SPI, RestrictionModifiers.NO_INSTANTIATE);
	}	
	
	/**
	 * Tests API description: a.b.c.spi.SpiD = SPI with no instantiate, no subclass.
	 */
	public void testDSpiPkg() {
		resolveType("a.b.c.spi.SpiD", VisibilityModifiers.SPI, RestrictionModifiers.NO_INSTANTIATE | RestrictionModifiers.NO_EXTEND);
	}
	
	/**
	 * Tests API description: a.b.c.spi.SpiD = SPI with no reference.
	 */
	public void testESpiPkg() {
		resolveType("a.b.c.spi.SpiE", VisibilityModifiers.SPI, RestrictionModifiers.NO_REFERENCE);
	}
	
	/**
	 * Tests API description: a.b.c.spi.ISpiA = SPI with no restrictions.
	 * Note this type is not explicitly in the manifest.
	 */
	public void testIASpiPkg() {
		resolveType("a.b.c.spi.ISpiA", VisibilityModifiers.SPI, RestrictionModifiers.NO_RESTRICTIONS);
	}
	
	/**
	 * Tests API description: a.b.c.spi.ISpiB = SPI with no implement.
	 */
	public void testIBSpiPkg() {
		resolveType("a.b.c.spi.ISpiB", VisibilityModifiers.SPI, RestrictionModifiers.NO_IMPLEMENT);
	}
	
	/**
	 * Tests API description: a.b.c.internal.A = Private with no restrictions.
	 * Note this type is not in the manifest explicitly.
	 */
	public void testAInternalPkg() {
		resolveType("a.b.c.internal.PA", VisibilityModifiers.PRIVATE, RestrictionModifiers.NO_RESTRICTIONS);
	}
	
	/**
	 * Tests API description: a.b.c.internal.B = Private with no restrictions.
	 * Note this type is not in the manifest explicitly.
	 */
	public void testBInternalPkg() {
		resolveType("a.b.c.internal.PB", VisibilityModifiers.PRIVATE, RestrictionModifiers.NO_RESTRICTIONS);
	}
		
	/**
	 * tests that a binary bundle with no .api_description file has no API description
	 */
	public void testBinaryHasNoApiDescription() throws CoreException {
		IApiBaseline profile = TestSuiteHelper.createTestingProfile("test-plugins");
		IApiComponent componentA = profile.getApiComponent("component.a");
		assertFalse("Should have no .api_description file", componentA.hasApiDescription());
	}
	
	/**
	 * tests that a binary bundle with an .api_description file has an API description
	 */
	public void testBinaryHasApiDescription() throws CoreException {
		IApiBaseline profile = TestSuiteHelper.createTestingProfile("test-plugins-with-desc");
		IApiComponent componentA = profile.getApiComponent("component.a");
		assertTrue("Should have an .api_description file", componentA.hasApiDescription());
	}	
}
