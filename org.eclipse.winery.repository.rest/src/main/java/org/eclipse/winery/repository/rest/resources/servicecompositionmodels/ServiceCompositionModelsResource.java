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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.xml.namespace.QName;

import org.eclipse.winery.common.ids.definitions.NodeTypeId;
import org.eclipse.winery.common.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.model.tosca.TExportedOperation;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TNodeType;
import org.eclipse.winery.model.tosca.TOperation;
import org.eclipse.winery.model.tosca.TServiceTemplate;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.repository.rest.resources._support.AbstractComponentsWithoutTypeReferenceResource;
import org.eclipse.winery.repository.rest.resources.apiData.ServiceCompositionServiceTemplateData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceCompositionModelsResource extends AbstractComponentsWithoutTypeReferenceResource<ServiceCompositionModelResource> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceCompositionModelsResource.class);

    @Path("servicetemplates/")
    @GET
    public ServiceCompositionServiceTemplateData getUsableServiceTemplates() {
        IRepository repo = RepositoryFactory.getRepository();
        SortedSet<ServiceTemplateId> serviceTemplateIds = repo.getAllDefinitionsChildIds(ServiceTemplateId.class);
        
        List<ServiceCompositionServiceTemplateData.ServiceTemplateData> serviceTemplateData = new ArrayList<>();
        
        // filter the ServiceTemplates that provide the required interface in the boundary definition
        for (ServiceTemplateId serviceTemplateId : serviceTemplateIds) {
            TServiceTemplate template = repo.getElement(serviceTemplateId);
            if (Objects.isNull(template)) {
                LOGGER.error("Unable to retrieve TServiceTemplate for ID: {}", serviceTemplateId);
                continue;
            }
            
            if (Objects.isNull(template.getBoundaryDefinitions()) || Objects.isNull(template.getBoundaryDefinitions().getInterfaces())) {
                LOGGER.trace("ServiceTemplate with ID {} has no boundary definition or no interfaces in the boundary " +
                    "definition defined. Skipping it!", template.getId());
                continue;
            }

            // search for the 'ServiceComposition' interface and the 'invokeService' operation
            TExportedOperation serviceCompositionOperation =
                template.getBoundaryDefinitions().getInterfaces().getInterface().stream()
                .filter(exportedInterface -> exportedInterface.getName().equals("ServiceComposition"))
                .flatMap(exportedInterface -> exportedInterface.getOperation().stream())
                .filter(exportedOperation -> exportedOperation.getName().equals("invokeService"))
                .findAny().orElse(null);
            
            if (Objects.isNull(serviceCompositionOperation)) {
                LOGGER.trace("ServiceTemplate with ID {} does not contain the required interface.", template.getId());
                continue;
            }
            
            LOGGER.debug("Found required interface to use ServiceTemplate with ID {} in a service composition!", template.getId());
            if (Objects.isNull(serviceCompositionOperation.getNodeOperation())) {
                LOGGER.error("Currently only node operations are supported as implementation of the service " +
                    "composition operation!");
                continue;
            }
            
            // get the NodeType of the referenced NodeTemplate
            TNodeTemplate nodeTemplate = (TNodeTemplate) serviceCompositionOperation.getNodeOperation().getNodeRef();
            QName nodeTypeQName = nodeTemplate.getType();
            TNodeType nodeType = repo.getElement(new NodeTypeId(nodeTypeQName));

            if (Objects.isNull(nodeType)) {
                LOGGER.error("Unable to retrieve NodeType of NodeTemplate with ID: {}", nodeTemplate.getId());
                continue;
            }

            // get the referenced operation from the NodeType
            LOGGER.debug("Boundary definition references NodeTemplate of NodeType: {}", nodeType.getQName());
            TOperation operation = nodeType.getInterfaces().getInterface().stream()
                .filter(i -> i.getName().equals("ServiceComposition"))
                .flatMap(i -> i.getOperation().stream())
                .filter(op -> op.getName().equals(serviceCompositionOperation.getName()))
                .findFirst().orElse(null);
            if (Objects.isNull(operation)) {
                LOGGER.error("Unable to retrieve referenced operation from NodeType: {}", nodeType.getQName());
                continue;
            }
            
            // ServiceTemplate is valid and has to be returned
            ServiceCompositionServiceTemplateData.ServiceTemplateData templateData =
                new ServiceCompositionServiceTemplateData.ServiceTemplateData(new QName(template.getTargetNamespace()
                    , template.getId()));
            
            // get the input parameters
            if (Objects.nonNull(operation.getInputParameters()) && !operation.getInputParameters().getInputParameter().isEmpty()) {
                templateData.setInputParameters(operation.getInputParameters().getInputParameter());
            }

            // get the output parameters
            if (Objects.nonNull(operation.getOutputParameters()) && !operation.getOutputParameters().getOutputParameter().isEmpty()) {
                templateData.setOutputParameters(operation.getOutputParameters().getOutputParameter());
            }
            
            // add service template to the http response object
            serviceTemplateData.add(templateData);
        }

        ServiceCompositionServiceTemplateData data = new ServiceCompositionServiceTemplateData();
        data.setServiceTemplates(serviceTemplateData);
        return data;
    }
    
    @Path("{namespace}/{id}/")
    public ServiceCompositionModelResource getComponentInstanceResource(@PathParam("namespace") String namespace, @PathParam("id") String id) {
        return this.getComponentInstanceResource(namespace, id, true);
    }
}
