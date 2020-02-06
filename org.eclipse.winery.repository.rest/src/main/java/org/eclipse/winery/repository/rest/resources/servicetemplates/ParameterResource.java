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
package org.eclipse.winery.repository.rest.resources.servicetemplates;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.tosca.kvproperties.ParameterDefinitionList;
import org.eclipse.winery.repository.rest.RestUtils;

public class ParameterResource {

    private final ServiceTemplateResource parent;
    private final TTopologyTemplate topologyTemplate;

    public ParameterResource(ServiceTemplateResource parent, TTopologyTemplate topologyTemplate) {
        this.parent = parent;
        this.topologyTemplate = topologyTemplate;
    }

    @GET
    @Path("inputs")
    @Produces(MediaType.APPLICATION_JSON)
    public ParameterDefinitionList getInputParameters() {
        return this.topologyTemplate.getInputs();
    }

    @PUT
    @Path("inputs")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ParameterDefinitionList putInputParameters(ParameterDefinitionList parameters) {
        this.topologyTemplate.setInputs(parameters);
        RestUtils.persist(this.parent);
        return this.topologyTemplate.getInputs();
    }

    @GET
    @Path("outputs")
    @Produces(MediaType.APPLICATION_JSON)
    public ParameterDefinitionList getOutputParameters() {
        return this.topologyTemplate.getOutputs();
    }

    @PUT
    @Path("outputs")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public ParameterDefinitionList putOutputParameters(ParameterDefinitionList parameters) {
        this.topologyTemplate.setOutputs(parameters);
        RestUtils.persist(this.parent);
        return this.topologyTemplate.getOutputs();
    }
}
