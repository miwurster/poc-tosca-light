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

import org.eclipse.winery.model.tosca.TServiceCompositionModel;

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
     * @param serviceCompositionModel the model representing the service composition
     * @param containerURL URL to deploy the services on an OpenTOSCA Container
     * @param odeUrl URL to deploy the workflow on an Apache ODE
     */
    public static void deployServiceComposition(TServiceCompositionModel serviceCompositionModel, URL containerURL,
                                                URL odeUrl) {
        LOGGER.debug("Starting deployment for service composition: {}", serviceCompositionModel.getName());
        LOGGER.debug("Using OpenTOSCA Container endpoint: {} and Apache ODE endpoint: {}", containerURL, odeUrl);

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // TODO
        
        LOGGER.debug("Deployment terminated!");
    }    
}
