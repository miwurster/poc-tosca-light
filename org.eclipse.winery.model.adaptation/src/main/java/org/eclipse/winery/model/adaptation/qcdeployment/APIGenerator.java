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

package org.eclipse.winery.model.adaptation.qcdeployment;

import java.io.OutputStream;
import java.util.List;

import javax.xml.namespace.QName;

import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TServiceTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class APIGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(APIGenerator.class);
    private TServiceTemplate serviceTemplate;

    public APIGenerator(TServiceTemplate serviceTemplate) {
        this.serviceTemplate = serviceTemplate;
    }

    public void generate(OutputStream output) {
        Extractor extractor = new Extractor(serviceTemplate);

        if (!isValid(extractor)) {
            return;
        }

        // TODO iterate trough all and not just the first one
        TNodeTemplate firstNode = extractor.getAllQCNodes().get(0);

        QName selectedArtifact = Selector.select(extractor.getHostName(firstNode), extractor.getAlgorithmName(firstNode));

        Generator generator = new FlaskPythonAPIGenerator(extractor);
        generator.generateApi(firstNode, selectedArtifact, output);
    }

    private boolean isValid(Extractor extractor) {
        List<TNodeTemplate> allQCNodesOfService = extractor.getAllQCNodes();
        if (allQCNodesOfService.size() == 0) {
            LOGGER.warn("No nodes of QC type found. Cancelling.");
            return false;
        }
        return true;
    }
}
