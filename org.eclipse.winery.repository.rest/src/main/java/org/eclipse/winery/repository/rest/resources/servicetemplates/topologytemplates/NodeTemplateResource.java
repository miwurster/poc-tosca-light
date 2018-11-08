/*******************************************************************************
 * Copyright (c) 2012-2014 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.repository.rest.resources.servicetemplates.topologytemplates;

import io.swagger.annotations.ApiOperation;
import org.eclipse.winery.common.ids.Namespace;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.constants.Namespaces;
import org.eclipse.winery.repository.rest.RestUtils;
import org.eclipse.winery.repository.rest.resources._support.INodeTemplateResourceOrNodeTypeImplementationResource;
import org.eclipse.winery.repository.rest.resources._support.IPersistable;
import org.eclipse.winery.repository.rest.resources._support.collections.IIdDetermination;
import org.eclipse.winery.repository.rest.resources.artifacts.DeploymentArtifactsResource;
import org.eclipse.winery.repository.rest.resources.entitytemplates.TEntityTemplateResource;
import org.eclipse.winery.repository.rest.resources.servicetemplates.ServiceTemplateResource;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;
import javax.xml.namespace.QName;
import java.util.List;
import java.util.Map;

public class NodeTemplateResource extends TEntityTemplateResource<TNodeTemplate> implements INodeTemplateResourceOrNodeTypeImplementationResource {

    private final QName qnameX = new QName(Namespaces.TOSCA_WINERY_EXTENSIONS_NAMESPACE, "x");
    private final QName qnameY = new QName(Namespaces.TOSCA_WINERY_EXTENSIONS_NAMESPACE, "y");

    public NodeTemplateResource(IIdDetermination<TNodeTemplate> idDetermination, TNodeTemplate o, int idx, List<TNodeTemplate> list, IPersistable res) {
        super(idDetermination, o, idx, list, res);
    }

    @Path("deploymentartifacts/")
    public DeploymentArtifactsResource getDeploymentArtifacts() {
        return new DeploymentArtifactsResource(this.o, this);
    }

    // The following methods are currently *not* used by the topology modeler. The modeler is using the repository client to interact with the repository

    @GET
    @Path("minInstances")
    public String getMinInstances() {
        return Integer.toString(this.o.getMinInstances());
    }

    @PUT
    @Path("minInstances")
    public Response setMinInstances(@FormParam(value = "minInstances") String minInstances) {
        int min = Integer.parseInt(minInstances);
        this.o.setMinInstances(min);
        return RestUtils.persist(this.res);
    }

    @GET
    @Path("maxInstances")
    public String getMaxInstances() {
        return this.o.getMaxInstances();
    }

    @PUT
    @Path("maxInstances")
    public Response setMaxInstances(@FormParam(value = "maxInstances") String maxInstances) {
        // TODO: check for valid integer | "unbound"
        this.o.setMaxInstances(maxInstances);
        return RestUtils.persist(this.res);
    }


    /* * *
     * The visual appearance
     *
     * We do not use a subresource "visualappearance" here to avoid generation of more objects
     * * */

    @Path("x")
    @GET
    @ApiOperation(value = "@return the x coordinate of the node template")
    public String getX() {
        Map<QName, String> otherAttributes = this.o.getOtherAttributes();
        return otherAttributes.get(this.qnameX);
    }

    @Path("x")
    @PUT
    public Response setX(String x) {
        this.o.getOtherAttributes().put(this.qnameX, x);
        return RestUtils.persist(this.res);
    }

    @Path("y")
    @GET
    @ApiOperation(value = "@return the y coordinate of the node template")
    public String getY() {
        Map<QName, String> otherAttributes = this.o.getOtherAttributes();
        return otherAttributes.get(this.qnameY);
    }

    @Path("y")
    @PUT
    public Response setY(String y) {
        this.o.getOtherAttributes().put(this.qnameY, y);
        return RestUtils.persist(this.res);
    }

    @Override
    public Namespace getNamespace() {
        // TODO Auto-generated method stub
        throw new IllegalStateException("Not yet implemented.");
    }

    /**
     * Required for persistence after a change of the deployment artifact. Required by DeploymentArtifactResource to be
     * able to persist
     *
     * @return the service template this node template belongs to
     */
    public ServiceTemplateResource getServiceTemplateResource() {
        return (ServiceTemplateResource) this.res;
    }

    /**
     * required for topology modeler to check for existence of a node template at the server
     *
     * @return empty response
     */
    @HEAD
    public Response getHEAD() {
        return Response.noContent().build();
    }

    
    // TODO : decrypt node template if security policies are specified and the respective key is present 
    @Path("decrypt")
    @POST
    public Response decrypt() {
        return Response.noContent().build();
    }
}
