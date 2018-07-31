/*******************************************************************************
 * Copyright (c) 2013-2014 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.repository.rest.resources.servicetemplates.boundarydefinitions;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.eclipse.winery.model.tosca.TBoundaryDefinitions;
import org.eclipse.winery.model.tosca.TBoundaryDefinitions.Capabilities;
import org.eclipse.winery.model.tosca.TBoundaryDefinitions.Interfaces;
import org.eclipse.winery.model.tosca.TBoundaryDefinitions.Policies;
import org.eclipse.winery.model.tosca.TBoundaryDefinitions.Properties;
import org.eclipse.winery.model.tosca.TBoundaryDefinitions.Properties.PropertyMappings;
import org.eclipse.winery.model.tosca.TBoundaryDefinitions.Requirements;
import org.eclipse.winery.model.tosca.TCapabilityRef;
import org.eclipse.winery.model.tosca.TRequirementRef;
import org.eclipse.winery.model.tosca.kvproperties.PropertyKV;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.repository.rest.RestUtils;
import org.eclipse.winery.repository.rest.resources.servicetemplates.ServiceTemplateResource;
import org.eclipse.winery.repository.rest.resources.servicetemplates.boundarydefinitions.interfaces.InterfacesResource;
import org.eclipse.winery.repository.rest.resources.servicetemplates.boundarydefinitions.policies.PoliciesResource;
import org.eclipse.winery.repository.rest.resources.servicetemplates.boundarydefinitions.reqscaps.CapabilitiesResource;
import org.eclipse.winery.repository.rest.resources.servicetemplates.boundarydefinitions.reqscaps.RequirementsResource;

import io.swagger.annotations.ApiOperation;

public class BoundaryDefinitionsResource {

    private final ServiceTemplateResource serviceTemplateResource;
    private final TBoundaryDefinitions boundaryDefinitions;

    public BoundaryDefinitionsResource(ServiceTemplateResource serviceTemplateResource, TBoundaryDefinitions boundaryDefinitions) {
        this.serviceTemplateResource = serviceTemplateResource;
        this.boundaryDefinitions = boundaryDefinitions;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public TBoundaryDefinitions getJSON(@Context UriInfo uriInfo) {
        return new BoundaryDefinitionsJSPData(this.serviceTemplateResource.getServiceTemplate(), uriInfo.getBaseUri()).getDefs();
    }

    @Path("xml/")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public String getXML(@Context UriInfo uriInfo) {
        return new BoundaryDefinitionsJSPData(this.serviceTemplateResource.getServiceTemplate(), uriInfo.getBaseUri()).getBoundaryDefinitionsAsXMLString();
    }

    @PUT
    @ApiOperation(value = "Replaces the boundary definitions by the information given in the XML")
    @Consumes( {MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public Response setModel(TBoundaryDefinitions boundaryDefinitions) {
        this.serviceTemplateResource.getServiceTemplate().setBoundaryDefinitions(boundaryDefinitions);
        return RestUtils.persist(this.serviceTemplateResource);
    }

    @Path("properties/")
    @GET
    @Produces(MediaType.APPLICATION_XML)
    public String getProperties(@Context UriInfo uriInfo) {
        return new BoundaryDefinitionsJSPData(this.serviceTemplateResource.getServiceTemplate(), uriInfo.getBaseUri()).getPropertiesAsXMLString();
    }

    @Path("properties/")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getKVProperties(@Context UriInfo uriInfo) {
        Set<PropertyKV> result = new BoundaryDefinitionsJSPData(this.serviceTemplateResource.getServiceTemplate(), uriInfo.getBaseUri()).getKVProperties();
        return Response.ok()
            .entity(Objects.isNull(result) ? "[]" : result)
            .type(MediaType.APPLICATION_JSON)
            .build();
    }

    @Path("properties/")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "saves KV properties of boundary definitions")
    public Response putKVProperties(Set<PropertyKV> kv) {
        org.eclipse.winery.model.tosca.TBoundaryDefinitions.Properties properties = ModelUtilities.getProperties(this.boundaryDefinitions);
        properties.getKeyValueProperties().setKeyValueProperty(kv);
        return RestUtils.persist(this.serviceTemplateResource);
    }

    @Path("properties/")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "creates KV property of boundary definitions")
    public Response createKVProperty(PropertyKV kv) {
        if (kv.isValid()) {
            org.eclipse.winery.model.tosca.TBoundaryDefinitions.Properties properties = ModelUtilities.getProperties(this.boundaryDefinitions);
            properties.getKeyValueProperties().getKeyValueProperty().add(kv);
            return RestUtils.persist(this.serviceTemplateResource);
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("Received KV property is invalid").build();
        }
    }

    @Path("properties/{key}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "modifies KV property of boundary definitions")
    public Response editKVProperty(PropertyKV kv) {
        if (kv.isValid()) {
            org.eclipse.winery.model.tosca.TBoundaryDefinitions.Properties properties = ModelUtilities.getProperties(this.boundaryDefinitions);
            properties.getKeyValueProperties().editKeyValueProperty(kv);
            return RestUtils.persist(this.serviceTemplateResource);
        } else {
            return Response.status(Response.Status.BAD_REQUEST).entity("Received KV property is invalid").build();
        }
    }

    @Path("properties/{key}")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "deletes KV property of boundary definitions")
    public Response deleteKVProperty(@PathParam("key") String key) {
        org.eclipse.winery.model.tosca.TBoundaryDefinitions.Properties properties = ModelUtilities.getProperties(this.boundaryDefinitions);
        properties.getKeyValueProperties().removeKeyValueProperty(key);
        if (properties.getKeyValueProperties().getKeyValueProperty().isEmpty()) {
            properties.setKeyValueProperties(null);
        }
        return RestUtils.persist(this.serviceTemplateResource);
    }

    @Path("requirements/")
    public RequirementsResource getRequiremensResource() {
        Requirements requirements = this.boundaryDefinitions.getRequirements();
        if (requirements == null) {
            requirements = new Requirements();
            this.boundaryDefinitions.setRequirements(requirements);
        }
        List<TRequirementRef> refs = requirements.getRequirement();
        return new RequirementsResource(this.serviceTemplateResource, refs);
    }

    @Path("capabilities/")
    public CapabilitiesResource getCapabilitiesResource() {
        Capabilities caps = this.boundaryDefinitions.getCapabilities();
        if (caps == null) {
            caps = new Capabilities();
            this.boundaryDefinitions.setCapabilities(caps);
        }
        List<TCapabilityRef> refs = caps.getCapability();
        return new CapabilitiesResource(this.serviceTemplateResource, refs);
    }

    @Path("policies/")
    public PoliciesResource getPoliciesResource() {
        Policies policies = this.boundaryDefinitions.getPolicies();
        if (policies == null) {
            policies = new Policies();
            this.boundaryDefinitions.setPolicies(policies);
        }
        return new PoliciesResource(policies.getPolicy(), this.serviceTemplateResource);
    }

    /**
     * This path is below "boundary definitions" to ease implementation If it
     * was modeled following the XSD, it would have been nested below
     * "properties". We did not do that
     */
    @Path("propertymappings/")
    public PropertyMappingsResource getPropertyMappings() {
        Properties properties = this.boundaryDefinitions.getProperties();
        if (properties == null) {
            properties = new Properties();
            this.boundaryDefinitions.setProperties(properties);
        }
        PropertyMappings propertyMappings = properties.getPropertyMappings();
        if (propertyMappings == null) {
            propertyMappings = new PropertyMappings();
            properties.setPropertyMappings(propertyMappings);
        }
        return new PropertyMappingsResource(propertyMappings, this.serviceTemplateResource);
    }

    @Path("propertyconstraints")
    public PropertyConstraintsResource getPropertyConstraints() {
        TBoundaryDefinitions.PropertyConstraints constraints = this.boundaryDefinitions.getPropertyConstraints();
        if (constraints == null) {
            constraints = new TBoundaryDefinitions.PropertyConstraints();
            this.boundaryDefinitions.setPropertyConstraints(constraints);
        }
        return new PropertyConstraintsResource(constraints, this.serviceTemplateResource);
    }

    @Path("interfaces/")
    public InterfacesResource getInterfacesResource() {
        Interfaces interfaces = this.boundaryDefinitions.getInterfaces();
        if (interfaces == null) {
            interfaces = new Interfaces();
            this.boundaryDefinitions.setInterfaces(interfaces);
        }
        return new InterfacesResource(interfaces.getInterface(), this.serviceTemplateResource);
    }
}
