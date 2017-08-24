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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TRelationshipTemplate;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.repository.splitting.groupprovisioninggraphmodel.Group;
import org.eclipse.winery.repository.splitting.groupprovisioninggraphmodel.GroupProvisioningOrderGraph;

import org.slf4j.LoggerFactory;

public class SplittingServiceTemplate {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SplittingServiceTemplate.class);
	
	public static GroupProvisioningOrderGraph determineProvisiongingOrder (TTopologyTemplate topologyTemplate) {
		
		GroupProvisioningOrderGraph gPOG = new GroupProvisioningOrderGraph();
		Set<TNodeTemplate> queue = new HashSet<>();
		TTopologyTemplate connectionTopology = createConnectionTopology(topologyTemplate);
		
		List<TNodeTemplate> nodesWithoutIncomingRel = connectionTopology.getNodeTemplates().stream()
			.filter(nt -> ModelUtilities.getOutgoingRelationshipTemplates(connectionTopology, nt).isEmpty())
			.collect(Collectors.toList());
		queue.addAll(nodesWithoutIncomingRel);
		
		nodesWithoutIncomingRel.forEach(nt -> {
			Group group = new Group();
			group.addToGroupComponents(nt);
			group.setLabel(ModelUtilities.getTargetLabel(nt).get());
			gPOG.addVertex(group);
		});
		
		while (!queue.isEmpty()) {
			//TODO: To be finished
		}
		
		return gPOG;
	}
	
	private static TTopologyTemplate createConnectionTopology (TTopologyTemplate topologyTemplate) {
		List<TNodeTemplate> hostingNodesTemplates = topologyTemplate.getNodeTemplates().stream()
			.filter(nt -> !SplittingUtilities.getNodeTemplatesWithoutIncomingHostedOnRelationships(topologyTemplate).contains(nt))
			.collect(Collectors.toList());
		
		topologyTemplate.getNodeTemplates().removeAll(hostingNodesTemplates);
		
		List<TRelationshipTemplate> hostingRelationshipTemplates = topologyTemplate.getRelationshipTemplates().stream()
			.filter(rt -> SplittingUtilities.getBasisRelationshipType(rt.getType()).getName().equalsIgnoreCase("hostedOn"))
			.collect(Collectors.toList());
		
		topologyTemplate.getRelationshipTemplates().removeAll(hostingNodesTemplates);
		
		return topologyTemplate;
	}
	
}
