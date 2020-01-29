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
import java.util.LinkedHashMap;
import java.util.List;

import javax.xml.namespace.QName;

import org.eclipse.winery.model.tosca.TEntityTemplate;
import org.eclipse.winery.model.tosca.TEntityType;
import org.eclipse.winery.model.tosca.TInterface;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TNodeType;
import org.eclipse.winery.model.tosca.TOperation;
import org.eclipse.winery.model.tosca.TParameter;
import org.eclipse.winery.model.tosca.TRelationshipTemplate;
import org.eclipse.winery.model.tosca.TServiceTemplate;
import org.eclipse.winery.model.tosca.constants.ToscaBaseTypes;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Extractor {
    private static final IRepository repo = RepositoryFactory.getRepository();
    private static final Logger LOGGER = LoggerFactory.getLogger(Extractor.class);

    private TServiceTemplate serviceTemplate;

    public Extractor(TServiceTemplate serviceTemplate) {
        this.serviceTemplate = serviceTemplate;
    }

    public TServiceTemplate getServiceTemplate() {
        return serviceTemplate;
    }

    public String getHostName(TNodeTemplate node) {
        TNodeTemplate host = getHost(node);
        if (host != null) {
            return host.getId();
        }
        return "";
    }

    public String getAlgorithmName(TNodeTemplate node) {
        List<TNodeTemplate> allQCNodesOfService = getAllQCNodes();
        if (allQCNodesOfService.contains(node)) {
            return node.getId();
        }
        return "";
    }

    public ApiType getApiType(TNodeTemplate node) {
        LOGGER.debug("Try to get API type for " + node.getId());
        for (TRelationshipTemplate relation : serviceTemplate.getTopologyTemplate().getRelationshipTemplates()) {
            if (nodeIsTargetOfRelation(node, relation) && isOfConnectToType(relation)) {
                TEntityTemplate.Properties properties = relation.getProperties();
                if (properties == null) {
                    break;
                }
                LinkedHashMap<String, String> kvProperties = properties.getKVProperties();
                if (kvProperties == null) {
                    break;
                }
                String channelType = kvProperties.get(Const.API_TYPE_PROPERTY_KEY);
                String implementationType = kvProperties.get(Const.API_IMPLEMENTATION_TYPE_PROPERTY_KEY);
                return new ApiType(channelType, implementationType);
            }
        }
        LOGGER.debug("Haven't found API type for {}", node.getId());
        return null;
    }

    public @NonNull List<TParameter> getAllInputParameters(TNodeTemplate node) {
        TNodeType typeForTemplate = (TNodeType) repo.getTypeForTemplate(node);
        for (TInterface tInterface : typeForTemplate.getInterfaces().getInterface()) {
            if (tInterface.getName().equals("API")) {
                for (TOperation operation : tInterface.getOperation()) {
                    if (operation.getName().equals("invoke")) {
                        TOperation.@Nullable InputParameters inputParameters = operation.getInputParameters();
                        if (inputParameters != null) {
                            return inputParameters.getInputParameter();
                        }
                    }
                }
            }
        }
        return new ArrayList<>();
    }

    public @NonNull List<TParameter> getAllOutputParameters(TNodeTemplate node) {
        TNodeType typeForTemplate = (TNodeType) repo.getTypeForTemplate(node);
        for (TInterface tInterface : typeForTemplate.getInterfaces().getInterface()) {
            if (tInterface.getName().equals("API")) {
                for (TOperation operation : tInterface.getOperation()) {
                    if (operation.getName().equals("invoke")) {
                        TOperation.@Nullable OutputParameters outputParameters = operation.getOutputParameters();
                        if (outputParameters != null) {
                            return outputParameters.getOutputParameter();
                        }
                    }
                }
            }
        }
        return new ArrayList<>();
    }

    public @NonNull List<TNodeTemplate> getAllQCNodes() {
        List<TNodeTemplate> result = new ArrayList<>();
        @NonNull List<TNodeTemplate> allNodesOfService = serviceTemplate.getTopologyTemplate().getNodeTemplates();
        for (TNodeTemplate node : allNodesOfService) {
            if (isOfQCType(node)) {
                result.add(node);
            }
        }
        return result;
    }

    private boolean isOfQCType(TNodeTemplate node) {
        TEntityType nodeType = repo.getTypeForTemplate(node);
        // TODO not just parent but either all ancestors or the root
        TEntityType.DerivedFrom parentNodeType = nodeType.getDerivedFrom();
        if (parentNodeType != null) {
            return (parentNodeType.getType().equals(Const.QCTYPE));
        }
        return false;
    }

    private @Nullable TNodeTemplate getHost(TNodeTemplate node) {
        LOGGER.debug("Try to find host of {}", node.getId());
        for (TRelationshipTemplate relation : serviceTemplate.getTopologyTemplate().getRelationshipTemplates()) {
            if (nodeIsSourceOfRelation(node, relation) && isOfHostedOnType(relation)) {
                TRelationshipTemplate.SourceOrTargetElement targetElement = relation.getTargetElement();
                TNodeTemplate hostNode = serviceTemplate.getTopologyTemplate().getNodeTemplate(targetElement.getRef().getId());
                if (hostNode != null) {
                    LOGGER.debug("Host of {} is {}", node.getId(), hostNode.getId());
                    return hostNode;
                }
            }
        }
        LOGGER.debug("Didn't find host for {}", node.getId());
        return null;
    }

    private boolean nodeIsSourceOfRelation(TNodeTemplate node, TRelationshipTemplate relation) {
        TRelationshipTemplate.SourceOrTargetElement sourceElement = relation.getSourceElement();
        return sourceElement.getRef().getId().equals(node.getId());
    }

    private boolean nodeIsTargetOfRelation(TNodeTemplate node, TRelationshipTemplate relation) {
        TRelationshipTemplate.SourceOrTargetElement targetElement = relation.getTargetElement();
        return targetElement.getRef().getId().equals(node.getId());
    }

    private boolean isOfHostedOnType(TRelationshipTemplate relation) {
        QName typeOfRelation = relation.getType();
        return typeOfRelation != null && typeOfRelation.equals(ToscaBaseTypes.hostedOnRelationshipType);
    }

    private boolean isOfConnectToType(TRelationshipTemplate relation) {
        QName typeOfRelation = relation.getType();
        return typeOfRelation != null && typeOfRelation.equals(ToscaBaseTypes.connectsToRelationshipType);
    }
}
