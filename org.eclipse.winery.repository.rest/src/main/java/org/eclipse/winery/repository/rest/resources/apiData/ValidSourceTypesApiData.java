/*******************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.repository.rest.resources.apiData;

import java.util.List;

import javax.xml.namespace.QName;

import org.eclipse.winery.model.tosca.TCapabilityType;

public class ValidSourceTypesApiData {
    private List<QName> nodes;

    public ValidSourceTypesApiData() {

    }

    public ValidSourceTypesApiData(TCapabilityType capabilityType) {
        this.nodes = capabilityType.getValidNodeTypes();
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ValidSourceTypesApiDataJson: { nodes: ");

        if (nodes != null) {
            nodes.forEach(node -> builder.append(String.format("\"%s\",", node)));
            builder.replace(builder.lastIndexOf(","), builder.lastIndexOf(",") + 1, "}");
        } else {
            builder.append("null }");
        }

        return builder.toString();
    }
}
