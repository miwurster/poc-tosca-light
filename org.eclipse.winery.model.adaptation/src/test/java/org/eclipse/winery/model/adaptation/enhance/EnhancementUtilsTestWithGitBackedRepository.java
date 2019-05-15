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

package org.eclipse.winery.model.adaptation.enhance;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.eclipse.winery.common.ids.definitions.NodeTypeId;
import org.eclipse.winery.common.ids.definitions.NodeTypeImplementationId;
import org.eclipse.winery.common.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.model.tosca.TExtensibleElements;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TNodeType;
import org.eclipse.winery.model.tosca.TNodeTypeImplementation;
import org.eclipse.winery.model.tosca.TPolicy;
import org.eclipse.winery.model.tosca.TServiceTemplate;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.tosca.constants.OpenToscaBaseTypes;
import org.eclipse.winery.repository.TestWithGitBackedRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnhancementUtilsTestWithGitBackedRepository extends TestWithGitBackedRepository {

    @Test
    void determineStatefulComponentsTest() throws Exception {
        this.setRevisionTo("origin/plain");

        TServiceTemplate element = RepositoryFactory.getRepository()
            .getElement(new ServiceTemplateId(
                    QName.valueOf("{http://opentosca.org/examples/servicetemplates}TopologyWithStatefulComponent_w1-wip1")
                )
            );

        TTopologyTemplate topologyTemplate = EnhancementUtils.determineStatefulComponents(element.getTopologyTemplate());

        TNodeTemplate statefulComponent = topologyTemplate.getNodeTemplate("statefulComponent");
        assertNotNull(statefulComponent);
        TPolicy stateful = statefulComponent.getPolicies().getPolicy().get(0);
        assertEquals(OpenToscaBaseTypes.statefulComponentPolicyType, stateful.getPolicyType());

        TNodeTemplate stateless = topologyTemplate.getNodeTemplate("stateless");
        assertNotNull(stateless);
        assertNull(stateless.getPolicies());
    }

    @Test
    void determineFreezableComponents() throws Exception {
        this.setRevisionTo("origin/plain");

        TServiceTemplate element = RepositoryFactory.getRepository()
            .getElement(new ServiceTemplateId(
                    QName.valueOf("{http://opentosca.org/examples/servicetemplates}TopologyWithStatefulComponent_w1-wip2")
                )
            );
        TPolicy expectedPolicy = new TPolicy();
        expectedPolicy.setPolicyType(OpenToscaBaseTypes.freezableComponentPolicyType);
        expectedPolicy.setName("freezable");

        TopologyAndErrorList result = EnhancementUtils.determineFreezableComponents(element.getTopologyTemplate());

        assertEquals(0, result.errorList.size());

        TTopologyTemplate topologyTemplate = result.topologyTemplate;
        assertNull(topologyTemplate.getNodeTemplate("VM_2").getPolicies());

        List<TPolicy> statefulFreezableComponentPolicies = topologyTemplate.getNodeTemplate("statefulFreezableComponent")
            .getPolicies().getPolicy();
        assertEquals(2, statefulFreezableComponentPolicies.size());
        assertTrue(statefulFreezableComponentPolicies.contains(expectedPolicy));

        List<TPolicy> statefulNotFreezableComponentPolicies = topologyTemplate.getNodeTemplate("statefulNotFreezableComponent")
            .getPolicies().getPolicy();
        assertEquals(1, statefulNotFreezableComponentPolicies.size());
        assertFalse(statefulNotFreezableComponentPolicies.contains(expectedPolicy));

        List<TPolicy> statelessFreezableComponentPolicies = topologyTemplate.getNodeTemplate("statelessFreezableComponent")
            .getPolicies().getPolicy();
        assertEquals(1, statelessFreezableComponentPolicies.size());
        assertTrue(statelessFreezableComponentPolicies.contains(expectedPolicy));

        assertNull(topologyTemplate.getNodeTemplate("AbstractNodeTypeWithProperties_1-w1-wip1").getPolicies());
        assertNull(topologyTemplate.getNodeTemplate("Infrastructure-As-A-Service-Implementation_1-w1-wip1").getPolicies());
    }

    @Test
    void cleanFreezableComponents() throws Exception {
        this.setRevisionTo("origin/plain");

        TServiceTemplate element = RepositoryFactory.getRepository()
            .getElement(new ServiceTemplateId(
                    QName.valueOf("{http://opentosca.org/examples/servicetemplates}TopologyWithStatefulComponent_w1-wip3")
                )
            );

        TPolicy freezablePolicy = new TPolicy();
        freezablePolicy.setPolicyType(OpenToscaBaseTypes.freezableComponentPolicyType);
        freezablePolicy.setName("freezable");

        TTopologyTemplate topologyTemplate = EnhancementUtils.cleanFreezableComponents(element.getTopologyTemplate());
        assertTrue(topologyTemplate.getNodeTemplate("statefulFreezableComponent").getPolicies().getPolicy().contains(freezablePolicy));

        assertFalse(topologyTemplate.getNodeTemplate("statefulNotFreezableComponent").getPolicies().getPolicy().contains(freezablePolicy));
        assertFalse(topologyTemplate.getNodeTemplate("AbstractNodeTypeWithProperties_1-w1-wip1").getPolicies().getPolicy().contains(freezablePolicy));
        assertTrue(topologyTemplate.getNodeTemplate("statelessFreezableComponent").getPolicies().getPolicy().contains(freezablePolicy));

        assertFalse(topologyTemplate.getNodeTemplate("statefulFreezableImplicitlyProvisioned").getPolicies().getPolicy().contains(freezablePolicy));
        assertTrue(topologyTemplate.getNodeTemplate("VM_3").getPolicies().getPolicy().contains(freezablePolicy));
    }

    @Test
    void getAvailableFeatures() throws Exception {
        this.setRevisionTo("origin/plain");

        TServiceTemplate serviceTemplate = RepositoryFactory.getRepository()
            .getElement(
                new ServiceTemplateId(
                    QName.valueOf("{http://opentosca.org/add/management/to/instances/servicetemplates}STWithBasicManagementOnly_w1-wip1")
                )
            );

        Map<QName, Map<QName, String>> availableFeaturesForTopology =
            EnhancementUtils.getAvailableFeaturesForTopology(serviceTemplate.getTopologyTemplate());

        assertEquals(2, availableFeaturesForTopology.size());
        assertEquals(1, availableFeaturesForTopology.get(
            QName.valueOf("{http://opentosca.org/add/management/to/instances/nodetypes}MySQL-Database_w1")).size()
        );
        assertEquals(2, availableFeaturesForTopology.get(
            QName.valueOf("{http://opentosca.org/add/management/to/instances/nodetypes}Ubuntu_16.04-w1")).size()
        );
    }

    @Test
    void mergeFeatureNodeTypes() throws Exception {
        this.setRevisionTo("origin/plain");

        // region preparation
        Map<QName, TExtensibleElements> previousListOfNodeTypes = this.repository.getQNameToElementMapping(NodeTypeId.class);

        Map<QName, String> mySqlFeatures = new HashMap<>();
        mySqlFeatures.put(QName.valueOf("{http://opentosca.org/add/management/to/instances/nodetypes}MySQL-Database_freezable-w1"), "");

        Map<QName, String> ubuntuFeatures = new HashMap<>();
        ubuntuFeatures.put(QName.valueOf("{http://opentosca.org/add/management/to/instances/nodetypes}Ubuntu_16.04-testable-w1"), "");
        ubuntuFeatures.put(QName.valueOf("{http://opentosca.org/add/management/to/instances/nodetypes}Ubuntu_16.04-freezable-w1"), "");

        QName mySql = QName.valueOf("{http://opentosca.org/add/management/to/instances/nodetypes}MySQL-Database_w1");
        QName ubuntu = QName.valueOf("{http://opentosca.org/add/management/to/instances/nodetypes}Ubuntu_16.04-w1");

        Map<QName, Map<QName, String>> availableFeaturesForTopology = new HashMap<>();
        availableFeaturesForTopology.put(mySql, mySqlFeatures);
        availableFeaturesForTopology.put(ubuntu, ubuntuFeatures);
        // endregion

        Map<QName, TNodeType> oldNodeTypeToMergedNodeTypeMapping = EnhancementUtils.createFeatureNodeTypes(availableFeaturesForTopology);

        Map<QName, TExtensibleElements> listOfNodeTypes = this.repository.getQNameToElementMapping(NodeTypeId.class);
        QName expectedMergedMySqlQName = QName.valueOf("{http://opentosca.org/add/management/to/instances/nodetypes" + EnhancementUtils.GENERATED_NS_SUFFIX + "}MySQL-Database_w1");
        QName expectedMergedUbuntuQName = QName.valueOf("{http://opentosca.org/add/management/to/instances/nodetypes" + EnhancementUtils.GENERATED_NS_SUFFIX + "}Ubuntu_16.04-w1");
        TNodeType generatedMySql = oldNodeTypeToMergedNodeTypeMapping.get(mySql);
        TNodeType generatedUbuntu = oldNodeTypeToMergedNodeTypeMapping.get(ubuntu);

        assertTrue(this.repository.getNamespaceManager()
            .isGeneratedNamespace("http://opentosca.org/add/management/to/instances/nodetypes" + EnhancementUtils.GENERATED_NS_SUFFIX)
        );

        assertEquals(2, listOfNodeTypes.size() - previousListOfNodeTypes.size());
        assertEquals(2, oldNodeTypeToMergedNodeTypeMapping.size());
        assertEquals(expectedMergedMySqlQName, generatedMySql.getQName());
        assertEquals(expectedMergedUbuntuQName, generatedUbuntu.getQName());

        TNodeTypeImplementation generatedMySqlImpl = this.repository.getElement(
            new ArrayList<>(this.repository.getAllElementsReferencingGivenType(NodeTypeImplementationId.class, expectedMergedMySqlQName))
                .get(0)
        );
        TNodeTypeImplementation generatedUbuntuImpl = this.repository.getElement(
            new ArrayList<>(this.repository.getAllElementsReferencingGivenType(NodeTypeImplementationId.class, expectedMergedUbuntuQName))
                .get(0)
        );

        assertNotNull(generatedMySqlImpl);
        assertNotNull(generatedMySqlImpl.getImplementationArtifacts());
        assertEquals(4, generatedMySqlImpl.getImplementationArtifacts().getImplementationArtifact().size());
        assertNotNull(generatedUbuntuImpl);
        assertNotNull(generatedUbuntuImpl.getImplementationArtifacts());
        assertEquals(3, generatedUbuntuImpl.getImplementationArtifacts().getImplementationArtifact().size());
    }
}
