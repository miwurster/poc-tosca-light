/*******************************************************************************
 * Copyright (c) 2017 University of Stuttgart.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and the Apache License 2.0 which both accompany this distribution,
 * and are available at http://www.eclipse.org/legal/epl-v10.html
 * and http://www.apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *    Karoline Saatkamp - initial API and implementation and/or initial documentation
 *******************************************************************************/

package org.eclipse.winery.repository.splitting;

import java.io.IOException;
import java.util.List;

import org.eclipse.winery.common.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.repository.TestWithGitBackedRepository;
import org.eclipse.winery.repository.splitting.groupprovisioninggraphmodel.Group;
import org.eclipse.winery.repository.splitting.groupprovisioninggraphmodel.GroupProvisioningOrderGraph;
import org.eclipse.winery.repository.splitting.groupprovisioninggraphmodel.OrderRelation;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class SplittingServiceTemplateTest extends TestWithGitBackedRepository {

	@Test
	public void createConnectionTopology() throws GitAPIException {
		this.setRevisionTo("a71490a6d5dc6f7a0739520d6b968d765f75249e");
		ServiceTemplateId id = new ServiceTemplateId("http://winery.opentosca.org/test/servicetemplates/ponyuniverse/splittingservicetemplate", "SplittingServiceTemplateTest", false);
		TTopologyTemplate topologyTemplate = this.repository.getElement(id).getTopologyTemplate();
		TTopologyTemplate connectionTopology = SplittingServiceTemplate.createConnectionTopology(topologyTemplate);
		
		assertEquals(5, connectionTopology.getNodeTemplates().size());
		assertEquals(5, connectionTopology.getRelationshipTemplates().size());
	}

	@Test
	public void createGPOG() throws GitAPIException {
		this.setRevisionTo("a71490a6d5dc6f7a0739520d6b968d765f75249e");
		ServiceTemplateId id = new ServiceTemplateId("http://winery.opentosca.org/test/servicetemplates/ponyuniverse/splittingservicetemplate", "SplittingServiceTemplateTest", false);
		TTopologyTemplate topologyTemplate = this.repository.getElement(id).getTopologyTemplate();
		GroupProvisioningOrderGraph gPOG = SplittingServiceTemplate.initializeGPOG(topologyTemplate);

		assertEquals(5, gPOG.vertexSet().size());
		assertEquals(5, gPOG.edgeSet().size());
	}

	@Test
	public void compressRelationInGPOG() throws GitAPIException {
		this.setRevisionTo("a71490a6d5dc6f7a0739520d6b968d765f75249e");
		ServiceTemplateId id = new ServiceTemplateId("http://winery.opentosca.org/test/servicetemplates/ponyuniverse/splittingservicetemplate", "SplittingServiceTemplateTest", false);
		TTopologyTemplate topologyTemplate = this.repository.getElement(id).getTopologyTemplate();
		GroupProvisioningOrderGraph gPOG = SplittingServiceTemplate.initializeGPOG(topologyTemplate);
		GroupProvisioningOrderGraph newGPOG = new GroupProvisioningOrderGraph();
		gPOG.vertexSet().forEach(v -> newGPOG.addVertex(v));
		gPOG.edgeSet().forEach(e -> newGPOG.addEdge(e.getSource(), e.getTarget()));
		OrderRelation relation = gPOG.edgeSet().stream()
			.filter(r -> r.getSource().getGroupComponents().stream().anyMatch(nt -> nt.getId().equalsIgnoreCase("ponycompetition")))
			.filter(r -> r.getTarget().getLabel().equals(r.getSource().getLabel())).findAny().get();
		GroupProvisioningOrderGraph compressGPOG = SplittingServiceTemplate.contractGroupsInGPOG(newGPOG, relation.getSource(), relation.getTarget());

		assertEquals(4, compressGPOG.vertexSet().size());
		assertEquals(4, compressGPOG.edgeSet().size());
	}

	@Test
	public void compressRelationInGPOGProducingCycle() throws GitAPIException {
		this.setRevisionTo("a71490a6d5dc6f7a0739520d6b968d765f75249e");
		ServiceTemplateId id = new ServiceTemplateId("http://winery.opentosca.org/test/servicetemplates/ponyuniverse/splittingservicetemplate", "SplittingServiceTemplateTest", false);
		TTopologyTemplate topologyTemplate = this.repository.getElement(id).getTopologyTemplate();
		GroupProvisioningOrderGraph gPOG = SplittingServiceTemplate.initializeGPOG(topologyTemplate);
		
		GroupProvisioningOrderGraph newGPOG = new GroupProvisioningOrderGraph();
		gPOG.vertexSet().forEach(v -> newGPOG.addVertex(v));
		gPOG.edgeSet().forEach(e -> newGPOG.addEdge(e.getSource(), e.getTarget()));
		OrderRelation relation1 = newGPOG.edgeSet().stream()
			.filter(r -> r.getSource().getGroupComponents().stream().anyMatch(nt -> nt.getId().equalsIgnoreCase("ponycompetition")))
			.filter(r -> r.getTarget().getLabel().equals(r.getSource().getLabel())).findAny().get();
		GroupProvisioningOrderGraph compressGPOGNull;
		compressGPOGNull = SplittingServiceTemplate.contractGroupsInGPOG(newGPOG, relation1.getSource(), relation1.getTarget());

		OrderRelation relation2 = compressGPOGNull.edgeSet().stream()
			.filter(r -> r.getSource().getGroupComponents().stream().anyMatch(nt -> nt.getId().equalsIgnoreCase("ponycompetition_3")))
			.filter(r -> r.getTarget().getGroupComponents().stream().anyMatch(nt -> nt.getId().equalsIgnoreCase("unicorn"))).findAny().get();
		compressGPOGNull = SplittingServiceTemplate.contractGroupsInGPOG(compressGPOGNull, relation2.getSource(), relation2.getTarget());

		assertEquals(null, compressGPOGNull);
	}

	@Test
	public void compressGPOG() throws GitAPIException {
		this.setRevisionTo("a71490a6d5dc6f7a0739520d6b968d765f75249e");
		ServiceTemplateId id = new ServiceTemplateId("http://winery.opentosca.org/test/servicetemplates/ponyuniverse/splittingservicetemplate", "SplittingServiceTemplateTest", false);
		TTopologyTemplate topologyTemplate = this.repository.getElement(id).getTopologyTemplate();
		GroupProvisioningOrderGraph gPOG = SplittingServiceTemplate.initializeGPOG(topologyTemplate);
		GroupProvisioningOrderGraph newGPOG = new GroupProvisioningOrderGraph();
		gPOG.vertexSet().forEach(v -> newGPOG.addVertex(v));
		gPOG.edgeSet().forEach(e -> newGPOG.addEdge(e.getSource(), e.getTarget()));
		
		GroupProvisioningOrderGraph compressedGPOG = SplittingServiceTemplate.compressGPOG(newGPOG);
		
		assertEquals(3, compressedGPOG.vertexSet().size());
	}

	@Test
	public void determineProvisiongingOrder() throws GitAPIException {
		this.setRevisionTo("a71490a6d5dc6f7a0739520d6b968d765f75249e");
		ServiceTemplateId id = new ServiceTemplateId("http://winery.opentosca.org/test/servicetemplates/ponyuniverse/splittingservicetemplate", "SplittingServiceTemplateTest", false);
		TTopologyTemplate topologyTemplate = this.repository.getElement(id).getTopologyTemplate();
		List<Group> topologicalSorting = SplittingServiceTemplate.determineProvisiongingOrder(topologyTemplate);

		assertEquals(3, topologicalSorting.size());
	}

	@Test
	public void splitServiceTemplate() throws GitAPIException, IOException, SplittingException {
		this.setRevisionTo("a71490a6d5dc6f7a0739520d6b968d765f75249e");
		ServiceTemplateId id = new ServiceTemplateId("http://winery.opentosca.org/test/servicetemplates/ponyuniverse/splittingservicetemplate", "SplittingServiceTemplateTest", false);
		List<ServiceTemplateId> serviceTemplateIds = SplittingServiceTemplate.splitServiceTemplate(id);

		assertEquals(3, serviceTemplateIds.size());
	}
}
