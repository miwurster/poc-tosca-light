/*******************************************************************************
 * Copyright (c) 2017 University of Stuttgart.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and the Apache License 2.0 which both accompany this distribution,
 * and are available at http://www.eclipse.org/legal/epl-v10.html
 * and http://www.apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *     Oliver Kopp - initial API and implementation
 *******************************************************************************/
package org.eclipse.winery.model.tosca;

/**
 * Ensures that all inheritance things are available. Furthermore, it ensures that getDerivedFrom() returns HasType
 */
public interface HasInheritance {

    public TBoolean getAbstract();
    public TBoolean getFinal();
    public HasType getDerivedFrom();
    
}