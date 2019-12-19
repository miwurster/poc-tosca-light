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

package org.eclipse.winery.repository.rest.resources.servicecompositionmodels;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.namespace.QName;

import org.eclipse.winery.common.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.common.ids.definitions.ServiceCompositionModelId;
import org.eclipse.winery.model.adaptation.servicecomposition.DeploymentUtils;
import org.eclipse.winery.model.tosca.TExtensibleElements;
import org.eclipse.winery.model.tosca.TParameter;
import org.eclipse.winery.model.tosca.TServiceCompositionModel;
import org.eclipse.winery.repository.backend.BackendUtils;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.repository.rest.RestUtils;
import org.eclipse.winery.repository.rest.resources._support.AbstractComponentInstanceResource;
import org.eclipse.winery.repository.rest.resources._support.IHasName;
import org.eclipse.winery.repository.rest.resources.apiData.ServiceCompositionEndpointData;
import org.eclipse.winery.repository.rest.resources.apiData.ServiceCompositionParametersData;

public class ServiceCompositionModelResource extends AbstractComponentInstanceResource implements IHasName {
    
    /**
     * Instantiates the resource. Assumes that the resource should exist (assured by the caller)
     * <p>
     * The caller should <em>not</em> create the resource by other ways. E.g., by instantiating this resource and then
     * adding data.
     *
     * @param id
     */
    public ServiceCompositionModelResource(DefinitionsChildId id) { super(id); }

    @Path("parameters/")
    @GET
    public ServiceCompositionParametersData getParameters() {
        List<TParameter> inputParams = new ArrayList<>();
        if (Objects.nonNull(this.getServiceCompositionModel().getInputParameters())) {
            inputParams = this.getServiceCompositionModel().getInputParameters().getInputParameter();
        }

        List<TParameter> outputParams = new ArrayList<>();
        if (Objects.nonNull(this.getServiceCompositionModel().getOutputParameters())) {
            outputParams = this.getServiceCompositionModel().getOutputParameters().getOutputParameter();
        }

        ServiceCompositionParametersData data = new ServiceCompositionParametersData();
        data.setInputParameters(inputParams);
        data.setOutputParameters(outputParams);
        return data;
    }

    @Path("parameters/")
    @POST
    @Consumes( {MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON} )
    public Response updateParameters(ServiceCompositionParametersData parameters) {

        TServiceCompositionModel serviceComposition = this.getServiceCompositionModel();

        QName serviceCompositionName = new QName(serviceComposition.getTargetNamespace(), serviceComposition.getId());
        ServiceCompositionModelId serviceCompositionId =
            BackendUtils.getDefinitionsChildId(ServiceCompositionModelId.class, serviceCompositionName);

        if (Objects.nonNull(parameters.getInputParameters())) {
            if (Objects.isNull(serviceComposition.getInputParameters())) {
                serviceComposition.setInputParameters(new TServiceCompositionModel.InputParameters());
            }
            serviceComposition.getInputParameters().setInputParameter(parameters.getInputParameters());
        }

        if (Objects.nonNull(parameters.getOutputParameters())) {
            if (Objects.isNull(serviceComposition.getOutputParameters())) {
                serviceComposition.setOutputParameters(new TServiceCompositionModel.OutputParameters());
            }
            serviceComposition.getOutputParameters().setOutputParameter(parameters.getOutputParameters());
        }

        try {
            BackendUtils.persist(RepositoryFactory.getRepository(), serviceCompositionId, serviceComposition);
            return Response.status(Response.Status.OK).entity("Parameters updated successfully!").build();
        } catch (IOException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error while saving updated service " +
                "composition: " + e.getMessage()).build();
        }
    }

    @Path("deployment/")
    @POST
    @Consumes( {MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON} )
    public Response deployServiceComposition(ServiceCompositionEndpointData endpoints) {
        
        if (Objects.isNull(endpoints.getContainerURL()) || Objects.isNull(endpoints.getOdeURL())) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Deployment not possible with missing " +
                "endpoint!").build();
        }

        try {
            URL containerURL = new URL(endpoints.getContainerURL());
            URL odeURL = new URL(endpoints.getOdeURL());
            
            // start deployment in separate thread and return response as it takes some minutes
            new Thread(() -> {
                DeploymentUtils.deployServiceComposition(this.getServiceCompositionModel(), containerURL, odeURL);
            }).start();
            return Response.status(Response.Status.OK).entity("Deployment triggered successfully").build();
        } catch (MalformedURLException e) {
            return Response.status(Response.Status.BAD_REQUEST).entity("Unable to parse given endpoint to URL: " + e.getMessage()).build();
        }
    }

    @Override
    protected TExtensibleElements createNewElement() { return new TServiceCompositionModel(); }

    @Override
    public String getName() {
        String name = this.getServiceCompositionModel().getName();
        if (name == null) {
            // place default
            name = this.getId().getXmlId().getDecoded();
        }
        return name;
    }

    @Override
    public Response setName(String name) {
        this.getServiceCompositionModel().setName(name);
        return RestUtils.persist(this);
    }

    public TServiceCompositionModel getServiceCompositionModel() { return (TServiceCompositionModel) this.getElement(); }
}
