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

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import javax.xml.namespace.QName;

import org.eclipse.winery.model.tosca.TServiceCompositionModel;
import org.eclipse.winery.model.tosca.TServiceTemplate;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class exposes functionality to generate a BPEL plan corresponding to a TServiceCompositionModel and updating 
 * the endpoints for the service invocations with the provisioned service instances.
 */
public class ServiceCompositionGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceCompositionGenerator.class);

    /**
     * Generate the BPEL workflow representing the given service composition.
     * 
     * @param serviceCompositionModel the model of the service composition
     * @param serviceTemplates the utilized ServiceTemplates to retrieve the information about the used operations
     * @param endpointsMap the Map containing the endpoints for all deployed service instances
     * @param odeURL the Url to the ODE to update the endpoint reference of the workflow properly
     * @return the byte array containing the generated workflow
     */
    public byte[] generateServiceComposition(TServiceCompositionModel serviceCompositionModel,
                                           List<TServiceTemplate> serviceTemplates, HashMap<QName, URI> endpointsMap,
                                            URL odeURL) {
        LOGGER.debug("Generating workflow for service composition with Id: {}", serviceCompositionModel.getId());

        try {
            // TODO: generate workflow and adapt endpoint to 'odeURL + "/processes/" + serviceCompositionModel.getId()'
            InputStream in = this.getClass().getClassLoader().getResourceAsStream("servicecomposition/test.zip");
            return IOUtils.toByteArray(in);
        } catch (Exception e) {
            LOGGER.error("Error while generating workflow: {}", e.getMessage());
            return null;
        }
    }
}
