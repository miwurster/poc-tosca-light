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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SortedSet;

import javax.xml.namespace.QName;

import org.eclipse.winery.common.ids.definitions.ArtifactTemplateId;
import org.eclipse.winery.common.ids.definitions.DefinitionsChildId;
import org.eclipse.winery.common.ids.definitions.NodeTypeId;
import org.eclipse.winery.common.interfaces.QNameAlreadyExistsException;
import org.eclipse.winery.model.tosca.TArtifactTemplate;
import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;

import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.jvm.hotspot.oops.ObjArrayKlass;

public class Selector {

    private static final IRepository repo = RepositoryFactory.getRepository();
    private static final Logger LOGGER = LoggerFactory.getLogger(Selector.class);
    private static final SelectionStrategy selectionStrategy = new SelectFirstStrategy();

    private final String algorithm;
    private final String provider;



    public Selector(String provider, String algorithm) {
        this.provider = provider;
        this.algorithm = algorithm;
    }

    @Nullable QName select() {
        return Selector.select(provider, algorithm);
    }

    static @Nullable QName select(String hostName, String algorithmName) {
        Objects.requireNonNull(hostName);
        Objects.requireNonNull(algorithmName);
        List<QName> filteredExecutables = new ArrayList<>();

        HashMap<QName, TArtifactTemplate> artifactTemplates = new HashMap<>(repo.getQNameToElementMapping(ArtifactTemplateId.class));
        artifactTemplates.entrySet().stream()
            .filter(entry -> Objects.nonNull(entry.getValue().getProperties()))
            .filter(entry -> Objects.nonNull(entry.getValue().getProperties().getKVProperties()))
            .filter(entry -> entry.getValue().getProperties().getKVProperties().containsKey(Const.PROVIDER_PROPERTY_KEY)
                && entry.getValue().getProperties().getKVProperties().containsKey(Const.IMPLEMENTS_ALGORITHM_PROPERTY_KEY))
            .filter(entry -> hostName.equals(entry.getValue().getProperties().getKVProperties().get(Const.PROVIDER_PROPERTY_KEY))
                && algorithmName.equals(entry.getValue().getProperties().getKVProperties().get(Const.IMPLEMENTS_ALGORITHM_PROPERTY_KEY)))
            .forEach(entry -> filteredExecutables.add(entry.getKey()));
        
        return selectionStrategy.selectOneOf(filteredExecutables);
            
//        if (filteredExecutables.size() == 0) {
//            LOGGER.error("No artifact, that implements '{}' for provider {} found", algorithmName, hostName);
//            return null;
//        } else if (filteredExecutables.size() > 1) {
//            // TODO implement Selection Strategy
//        }
//        TArtifactTemplate selectedArtifact = selectionStrategy.selectOneOf(filteredExecutables);
//        LOGGER.debug("Artifact '{}' that implements '{}' for provider '{}' has been chosen", selectedArtifact.getId(), algorithmName, hostName);
//        return selectedArtifact;
    }
}
