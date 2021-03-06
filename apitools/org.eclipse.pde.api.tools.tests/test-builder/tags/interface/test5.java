/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
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
package a.b.c;

/**
 * Tests invalid @noinstantiate tags on nested inner interfaces
 * @noinstantiate
 */
public interface test5 {

	/**
	 * @noinstantiate
	 */
	interface inner {
		
	}
	
	interface inner1 {
		/**
		 * @noinstantiate
		 */
		interface inner2 {
			
		}
	}
	
	interface inner2 {
		
	}
}

interface outer {
	
	/**
	 * @noinstantiate
	 */
	interface inner {
		
	}
}
