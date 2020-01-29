/*******************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the Apache Software License 2.0
 * which is available at https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
 *******************************************************************************/

package org.eclipse.winery.model.adaptation.qcdeployment;

import javax.xml.namespace.QName;

public class Const {
    static final String PROVIDER_PROPERTY_KEY = "Provider";
    static final String IMPLEMENTS_ALGORITHM_PROPERTY_KEY = "ImplementsAlgorithm";
    static final String API_TYPE_PROPERTY_KEY = "InterfaceType";
    static final String API_IMPLEMENTATION_TYPE_PROPERTY_KEY = "InterfaceImplementationType";
    static final QName QCTYPE = new QName("http://opentosca.org/nodetypes", "QuantumAbstractType_w1-wip1");
}
