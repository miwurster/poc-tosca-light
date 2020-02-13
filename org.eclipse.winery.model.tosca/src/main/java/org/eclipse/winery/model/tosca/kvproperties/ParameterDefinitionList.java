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
package org.eclipse.winery.model.tosca.kvproperties;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "ParameterDefinitions")
public class ParameterDefinitionList extends ArrayList<ParameterDefinition> implements Serializable {

    public ParameterDefinitionList() {
    }

    public ParameterDefinitionList(Collection<? extends ParameterDefinition> c) {
        super(c);
    }

    @XmlElement(name = "ParameterDefinition")
    public List<ParameterDefinition> getParameterDefinitions() {
        return this;
    }
}
