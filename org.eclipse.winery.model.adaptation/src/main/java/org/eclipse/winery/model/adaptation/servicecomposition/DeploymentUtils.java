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

package org.eclipse.winery.model.adaptation.servicecomposition;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import javax.xml.namespace.QName;

import org.eclipse.winery.common.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.model.tosca.TServiceCompositionModel;
import org.eclipse.winery.model.tosca.TServiceTemplate;
import org.eclipse.winery.repository.backend.BackendUtils;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class exposes utility functions to deploy service compositions. Therefore, it is in charge to deploy the 
 * contained ServiceTemplates and create corresponding service instances and to deploy the workflow model into the 
 * workflow engine.
 */
public class DeploymentUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentUtils.class);

    /**
     * Deploys the given service composition with the workflow and all  required services.
     * 
     * @param serviceCompositionModel the model representing the service composition
     * @param containerURL URL to deploy the services on an OpenTOSCA Container
     * @param odeUrl URL to deploy the workflow on an Apache ODE
     */
    public static void deployServiceComposition(TServiceCompositionModel serviceCompositionModel, URL containerURL,
                                                URL odeUrl) {
        LOGGER.debug("Starting deployment for service composition: {}", serviceCompositionModel.getName());
        LOGGER.debug("Using OpenTOSCA Container endpoint: {} and Apache ODE endpoint: {}", containerURL, odeUrl);
        
        if (Objects.isNull(serviceCompositionModel.getServices()) || serviceCompositionModel.getServices().getServices().isEmpty()) {
            LOGGER.error("No services defined for the service composition!");
            return;
        }

        IRepository repo = RepositoryFactory.getRepository();
        
        // retrieve the ServiceTemplates corresponding to the services of the composition
        List<TServiceTemplate> serviceTemplates = new ArrayList<>();
        List<TServiceCompositionModel.Service> services = serviceCompositionModel.getServices().getServices();
        LOGGER.debug("Trying to retrieve ServiceTemplates for {} services.", services.size());
        for (TServiceCompositionModel.Service service : services) {
            LOGGER.debug("Retrieving ServiceTemplate for QName: {}", service.getId());
            
            // abort in case the required ServiceTemplate is not available in the repository
            ServiceTemplateId serviceId = BackendUtils.getDefinitionsChildId(ServiceTemplateId.class, service.getId());
            if (!repo.exists(serviceId)) {
                LOGGER.error("ServiceTemplate for QName {} is missing in the repository!", service.getId());
                return;
            }

            serviceTemplates.add(repo.getElement(serviceId));
        }

        LOGGER.debug("Successfully retrieved all required ServiceTemplates!");
        
        // upload the CSARs to the OpenTOSCA Container and create an instance of each service
        HashMap<QName, URL> endpointsMap = new HashMap<QName, URL>();
        for (TServiceTemplate serviceTemplate : serviceTemplates) {
            LOGGER.debug("Creating instance for ServiceTemplate {}...", serviceTemplate.getId());
            URL serviceEndpoint = deployServiceTemplate(serviceTemplate, containerURL);
            if (Objects.isNull(serviceEndpoint)) {
                LOGGER.error("Error while provisioning service instance for ServiceTemplate: {}", serviceTemplate.getId());
                return;
            }
            endpointsMap.put(new QName(serviceTemplate.getTargetNamespace(), serviceTemplate.getId()), serviceEndpoint);
        }
        
        URL workflowEndpoint = deployWorkflow(serviceCompositionModel, serviceTemplates, endpointsMap, odeUrl);
        if (Objects.nonNull(workflowEndpoint)) {
            LOGGER.debug("Successfully deployed workflow. It can be accessed at the following endpoint: {}", workflowEndpoint);
        } else {
            LOGGER.error("Deployment of the workflow failed!");
        }
    }

    /**
     * Deploy the CSAR of the given ServiceTemplate, create an instance, and return the endpoint of the service 
     * instance.
     * 
     * @param serviceTemplate the ServiceTemplate to deploy
     * @param containerURL the URL of the OpenTOSCA Container for the deployment
     * @return the URL of the deployed service or <code>null</code> if the deployment fails
     */
    private static URL deployServiceTemplate(TServiceTemplate serviceTemplate, URL containerURL) {
        // TODO
        return null;
    }

    /**
     * Create the workflow implementing the service composition, add the corresponding endpoints to the service 
     * calls, and deploy the workflow to Apache ODE.
     * 
     * @param serviceCompositionModel the service composition model
     * @param serviceTemplates the ServiceTemplates that are contained in the service composition
     * @param endpointsMap the endpoints of the deployed service instances
     * @param odeURL the URL of the Apache ODE for the deployment
     * @return the URL of the deployed workflow or <code>null</code> if the deployment fails
     */
    private static URL deployWorkflow(TServiceCompositionModel serviceCompositionModel,
                                      List<TServiceTemplate> serviceTemplates, HashMap<QName, URL> endpointsMap, URL odeURL) {
        // TODO
        return null;
    }
}


