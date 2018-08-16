/********************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
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
 ********************************************************************************/
package org.eclipse.winery.compliance.checking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.winery.common.ids.Namespace;
import org.eclipse.winery.common.ids.definitions.ComplianceRuleId;
import org.eclipse.winery.compliance.model.ToscaNode;
import org.eclipse.winery.model.tosca.TComplianceRule;
import org.eclipse.winery.model.tosca.TServiceTemplate;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.repository.backend.filebased.FilebasedRepository;

import com.google.common.collect.Lists;
import org.eclipse.jdt.annotation.NonNull;
import org.jgrapht.GraphMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceTemplateComplianceRuleRuleChecker {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceTemplateComplianceRuleRuleChecker.class);

    private TServiceTemplate serviceTemplate;

    public ServiceTemplateComplianceRuleRuleChecker(@NonNull TServiceTemplate serviceTemplate) {
        this.serviceTemplate = serviceTemplate;
    }

    public List<ComplianceRuleResult> checkComplianceRules() {
        StringBuilder checkingResult = new StringBuilder("Rulechecking result for servicetemplate " + serviceTemplate.getIdFromIdOrNameField() + System.lineSeparator());
        ServiceTemplateCheckingResult result = new ServiceTemplateCheckingResult();
        List<ComplianceRuleId> ruleIds = getRuleIds(serviceTemplate);
        List<ComplianceRuleResult> complianceRuleResults = new ArrayList<>();
        if (ruleIds.isEmpty()) {
            checkingResult.append("No rules defined");
        } else {
            IRepository repository = RepositoryFactory.getRepository();

            for (ComplianceRuleId ruleId : ruleIds) {
                TComplianceRule tComplianceRule = repository.getElement(ruleId);
                ComplianceRuleResult complianceRuleResult = new ComplianceRuleResult(ruleId.getNamespace().toString(), 
                    ruleId.getXmlId().toString());
                ComplianceRuleChecker checker = new ComplianceRuleChecker(tComplianceRule, serviceTemplate.getTopologyTemplate());
                List<GraphMapping> graphMappings;
                try {
                    graphMappings = checker.checkComplianceRule();
                    if (graphMappings.size() > 0) {
                        result.getUnsatisfied().add(ruleId.getQName());
                        checkingResult.append(ruleId.getQName().toString() + " violated:");
                        checkingResult.append(System.lineSeparator());
                        complianceRuleResult.setIsSatisfied(false);
                        for (GraphMapping mapping : graphMappings) {
                            Map<ToscaNode, ToscaNode> resultMap = checker.getSubGraphMappingAsMap(mapping, checker.getIdentifierGraph());
                            List<String> violatingNodeIds = resultMap.values().stream().map(node -> node.getId()).collect(Collectors.toList());
                            complianceRuleResult.getViolatingNodesIds().addAll(violatingNodeIds);
                            checkingResult.append(System.lineSeparator());
                            checkingResult.append(resultMap.values().stream().map(node -> node.getNodeTemplate().getIdFromIdOrNameField()).collect(Collectors.joining(";", "NodeTemplateIds: ", "")));
                        }
                    } else {
                        result.getSatisfied().add(ruleId.getQName());
                        checkingResult.append(ruleId.getQName().toString() + " satisfied");
                        complianceRuleResult.setIsSatisfied(true);
                    }
                    complianceRuleResults.add(complianceRuleResult);
                } catch (ComplianceCheckingException e) {
                    result.getException().add(ruleId.getQName());
                    LOGGER.debug("Could not check compliance at rule " + ruleId.getQName().toString(), e);
                }
            }
        }
        LOGGER.debug(checkingResult.toString());
        return complianceRuleResults;
    }
    
    public List<String> getViolatingNodes(GraphMapping graphMapping){
        List<String> violatingNodes = new ArrayList<>();
        return  violatingNodes;
    }

    public List<ComplianceRuleId> getRuleIds(TServiceTemplate serviceTemplate) {
        List<ComplianceRuleId> complianceRules = Lists.newArrayList();
        Namespace namespace = new Namespace(serviceTemplate.getTargetNamespace(), false);
        Collection<Namespace> componentsNamespaces = RepositoryFactory.getRepository().getComponentsNamespaces(ComplianceRuleId.class);
        List<Namespace> relevantNamespaces = componentsNamespaces.stream().filter(ns -> namespace.getDecoded().startsWith(ns.getDecoded().split("/compliancerules")[0])).collect(Collectors.toList());

        for (Namespace space : relevantNamespaces) {
            complianceRules.addAll((Collection<? extends ComplianceRuleId>) ((FilebasedRepository) RepositoryFactory.getRepository()).getAllIdsInNamespace(ComplianceRuleId.class, space));
        }
        return complianceRules;
    }

    public TServiceTemplate getServiceTemplate() {
        return serviceTemplate;
    }

    public void setServiceTemplate(TServiceTemplate serviceTemplate) {
        this.serviceTemplate = serviceTemplate;
    }
}
