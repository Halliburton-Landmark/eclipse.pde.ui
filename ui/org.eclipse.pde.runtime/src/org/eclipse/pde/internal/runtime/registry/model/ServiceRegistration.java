/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Wolfgang Schell <ws@jetztgrad.net> - bug 259348
 *******************************************************************************/
package org.eclipse.pde.internal.runtime.registry.model;

public class ServiceRegistration extends ModelObject {

	private long id;
	private String bundle;
	private long[] usingBundles = new long[0];
	private String[] classes = new String[0];
	private Property[] properties = new Property[0];

	public void setId(long id) {
		this.id = id;
	}

	public void setBundle(String bundle) {
		this.bundle = bundle;
	}

	public void setUsingBundles(long[] usingBundles) {
		if (usingBundles == null)
			throw new IllegalArgumentException();

		this.usingBundles = usingBundles;
	}

	public void setClasses(String[] classes) {
		if (usingBundles == null)
			throw new IllegalArgumentException();

		this.classes = classes;
	}

	public void setProperties(Property[] properties) {
		if (properties == null)
			throw new IllegalArgumentException();

		this.properties = properties;
	}

	public long getId() {
		return id;
	}

	public String[] getClasses() {
		return classes;
	}

	public String getBundle() {
		return bundle;
	}

	public long[] getUsingBundles() {
		return usingBundles;
	}

	public Property[] getProperties() {
		return properties;
	}

	public Property getProperty(String name) {
		for (int p = 0; p < properties.length; p++) {
			Property property = properties[p];
			if (name.equals(property.getName())) {
				return property;
			}
		}
		return null;
	}

	public static String toString(Object value) {
		if (value == null) {
			return ""; //$NON-NLS-1$
		} else if (value instanceof CharSequence) {
			CharSequence charSequence = (CharSequence) value;
			return charSequence.toString();
		} else if (value instanceof Object[]) {
			StringBuffer buff = new StringBuffer();
			appendString(buff, value);

			return buff.toString();
		} else {
			return value.toString();
		}
	}

	public static void appendString(StringBuffer buff, Object value) {
		if (value == null) {
			// ignore
		} else if (value instanceof Object[]) {
			Object[] objects = (Object[]) value;
			buff.append("["); //$NON-NLS-1$
			for (int o = 0; o < objects.length; o++) {
				Object object = objects[o];
				if (o > 0)
					buff.append(", "); //$NON-NLS-1$
				appendString(buff, object);
			}
			buff.append("]"); //$NON-NLS-1$
		} else {
			buff.append(value.toString());
		}
	}

	public boolean equals(Object obj) {
		return (obj instanceof ServiceRegistration) && (id == (((ServiceRegistration) obj).id));
	}

	public int hashCode() {
		return (int) id;
	}
}
