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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TRelationshipTemplate;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.repository.splitting.groupprovisioninggraphmodel.Group;
import org.eclipse.winery.repository.splitting.groupprovisioninggraphmodel.GroupProvisioningOrderGraph;
import org.eclipse.winery.repository.splitting.groupprovisioninggraphmodel.OrderRelation;

import org.jgrapht.alg.CycleDetector;
import org.jgrapht.alg.TransitiveReduction;
import org.slf4j.LoggerFactory;

public class SplittingServiceTemplate {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SplittingServiceTemplate.class);

	/**
	 *  topological sorting based on Kahn's algorithm
	 * @param topologyTemplate
	 * @return
	 */
	public static List<Group> determineProvisiongingOrder (TTopologyTemplate topologyTemplate) {
		Queue<Group> queue = new LinkedList<>();
		List<Group> visitNodes = new ArrayList<>();
		List<Group> topologicialSorting = new ArrayList<>();
		GroupProvisioningOrderGraph gPOG = initializeGPOG(topologyTemplate);
		GroupProvisioningOrderGraph compressedGPOG = compressGPOG(gPOG);
		GroupProvisioningOrderGraph workingCopyGPOG = new GroupProvisioningOrderGraph();
		compressedGPOG.vertexSet().forEach(v -> workingCopyGPOG.addVertex(v));
		compressedGPOG.edgeSet().forEach(e -> workingCopyGPOG.addEdge(e.getSource(), e.getTarget()));
		
		queue.addAll(workingCopyGPOG.vertexSet().stream().filter(v -> workingCopyGPOG.incomingEdgesOf(v).isEmpty()).collect(Collectors.toSet()));
		int i = 0;

		while (!queue.isEmpty()) {
			Group group = queue.poll();
			visitNodes.add(group);
			topologicialSorting.add(i, group);
			Set<OrderRelation> outgoingRelations = workingCopyGPOG.outgoingEdgesOf(group).stream().collect(Collectors.toSet());
			workingCopyGPOG.removeAllEdges(outgoingRelations);
			workingCopyGPOG.removeVertex(group);
			queue.addAll(workingCopyGPOG.vertexSet().stream().filter(v -> workingCopyGPOG.incomingEdgesOf(v).isEmpty())
				.filter(v -> !visitNodes.contains(v)).collect(Collectors.toSet()));
			i++;
		}

		return topologicialSorting;
	}


	/**
	 * 
	 * @param gPoG
	 * @return compressed GPOG with a reduced number of Provisioning Groups
	 */
	public static GroupProvisioningOrderGraph compressGPOG (GroupProvisioningOrderGraph gPoG) {
		TransitiveReduction TRANSITIVEREDUCTION = TransitiveReduction.INSTANCE;
		TRANSITIVEREDUCTION.reduce(gPoG);
		List<OrderRelation> queue = gPoG.edgeSet().stream()
			.filter(e -> e.getSource().getLabel().equals(e.getTarget().getLabel())).collect(Collectors.toList());
		
		while (!queue.isEmpty()) {
			OrderRelation relation = queue.get(0);
			GroupProvisioningOrderGraph tempGPOG = contractRelationInGPOG(gPoG, relation);
				if (tempGPOG != null) {
					Group compressedGroup = tempGPOG.vertexSet().stream()
						.filter(v -> v.getGroupComponents().containsAll(relation.getSource().getGroupComponents())).findFirst().get();
					
					queue.remove(relation);
					List<OrderRelation> replacedOutgoingRelations = queue.stream()
						.filter(r -> compressedGroup.getGroupComponents().containsAll(r.getSource().getGroupComponents())).collect(Collectors.toList());
					List<OrderRelation> replacingOutgoingRelations = tempGPOG.outgoingEdgesOf(compressedGroup).stream()
						.filter(r -> replacedOutgoingRelations.stream().anyMatch(or -> or.getTarget().equals(r.getTarget())))
						.collect(Collectors.toList());
					queue.removeAll(replacedOutgoingRelations);
					queue.addAll(replacingOutgoingRelations);

					List<OrderRelation> replacedIncomingRelations = queue.stream()
						.filter(r -> r.getTarget().equals(relation.getTarget())).collect(Collectors.toList());
					List<OrderRelation> replacingIncomingRelations = tempGPOG.incomingEdgesOf(compressedGroup).stream()
						.filter(r -> replacedIncomingRelations.stream().anyMatch(or -> or.getSource().equals(r.getSource())))
						.collect(Collectors.toList());
					queue.removeAll(replacedIncomingRelations);
					queue.addAll(replacingIncomingRelations);
					
					gPoG.clearGraph();
					tempGPOG.vertexSet().forEach(v -> gPoG.addVertex(v));
					tempGPOG.edgeSet().forEach(e -> gPoG.addEdge(e.getSource(), e.getTarget()));
				} 
				queue.remove(relation);
				
		}
		return gPoG;
	}

	/**
	 * 
	 * @param gPoG
	 * @param relation
	 * @return either the compressed gPoG or null if the compression results in a loop
	 */
	public static GroupProvisioningOrderGraph contractRelationInGPOG(GroupProvisioningOrderGraph gPoG, OrderRelation relation) {
		GroupProvisioningOrderGraph graph = new GroupProvisioningOrderGraph();
		gPoG.vertexSet().forEach(v -> graph.addVertex(v));
		gPoG.edgeSet().forEach(e -> graph.addEdge(e.getSource(), e.getTarget()));
		CycleDetector cycleDetector = new CycleDetector(graph);
		// Former target element is added to the source element group
		Group mergedGroup = relation.getSource();
		mergedGroup.addAllNodeTemplatesToGroupComponents(relation.getTarget().getGroupComponents());
		
		// For each edge related to the former target element a new edge has to be added
		Set<OrderRelation> outgoingRelations = graph.outgoingEdgesOf(relation.getTarget()).stream().collect(Collectors.toSet());
		Set<OrderRelation> incomingRelations = graph.incomingEdgesOf(relation.getTarget()).stream()
			.filter(ir -> !ir.getSource().equals(relation.getSource())).collect(Collectors.toSet());
		outgoingRelations.forEach(or -> graph.addEdge(relation.getSource(), or.getTarget()));
		incomingRelations.forEach(ir -> graph.addEdge(ir.getSource(), relation.getSource()));
		
		// Old edges and the former target element are removed
		graph.removeAllEdges(outgoingRelations);
		graph.removeAllEdges(incomingRelations);
		Set<TNodeTemplate> movedGroupSet = relation.getTarget().getGroupComponents();
		graph.removeVertex(relation.getTarget());
		graph.removeEdge(relation);
			
		if (cycleDetector.detectCycles()) {
				mergedGroup.getGroupComponents().removeAll(movedGroupSet);
				return null;
		}
		return graph;		
	}

	/**
	 * Creates a Provisioning Order Graph from a given Topology Template. Each source and target of the edges
	 * in the GPOG are the other way around as the relationships in the Topology Template. Each Node Template is
	 * contained in a separated Group
	 * 
	 * @param topologyTemplate
	 * @return Group Provisioning Order Graph with edges for the provisioning dependencies
	 */
	public static GroupProvisioningOrderGraph initializeGPOG (TTopologyTemplate topologyTemplate) {
		GroupProvisioningOrderGraph gPOG = new GroupProvisioningOrderGraph();
		TTopologyTemplate connectionTopology = createConnectionTopology(topologyTemplate);
		Set<TNodeTemplate> queue = new HashSet<>();

		List<TNodeTemplate> nodesWithoutIncomingRel = connectionTopology.getNodeTemplates().stream()
			.filter(nt -> ModelUtilities.getIncomingRelationshipTemplates(connectionTopology, nt).isEmpty())
			.collect(Collectors.toList());

		nodesWithoutIncomingRel.forEach(nt -> {
			Group group = new Group();
			group.addToGroupComponents(nt);
			group.setLabel(ModelUtilities.getTargetLabel(nt).get());
			gPOG.addVertex(group);
			queue.add(nt);
		});

		while (!connectionTopology.getNodeTemplates().isEmpty()) {
			Set<TNodeTemplate> tempQueue = new HashSet<>();	
			for (TNodeTemplate node : queue) {
				ModelUtilities.getOutgoingRelationshipTemplates(connectionTopology, node).stream()
					.forEach(rt -> {
						TNodeTemplate targetElement = ModelUtilities.getTargetNodeTemplateOfRelationshipTemplate(connectionTopology, rt);
						Group targetGroup;
						if (!gPOG.vertexSet().stream().
							filter(v -> (v.getGroupComponents().stream().findFirst().get()).getId().equals(targetElement.getId())).findAny().isPresent()) {
								targetGroup = new Group();
								targetGroup.addToGroupComponents(targetElement);
								targetGroup.setLabel(ModelUtilities.getTargetLabel(targetElement).get());
								gPOG.addVertex(targetGroup);
						} else {
							targetGroup = gPOG.vertexSet().stream()
								.filter(v -> (v.getGroupComponents().stream().findFirst().get()).getId().equals(targetElement.getId())).findAny().get();
						}
						Group sourceGroup = gPOG.vertexSet().stream().filter(g -> (g.getGroupComponents().stream().findFirst().get())
							.getId().equals(node.getId())).findFirst().get();
						gPOG.addEdge(targetGroup, sourceGroup);
						connectionTopology.getNodeTemplateOrRelationshipTemplate().remove(rt);
						if (ModelUtilities.getIncomingRelationshipTemplates(connectionTopology, targetElement).isEmpty()) {
							tempQueue.add(targetElement);
						} 
					});				
			}
			connectionTopology.getNodeTemplateOrRelationshipTemplate().removeAll(queue);
			queue.clear();
			queue.addAll(tempQueue);
			tempQueue.clear();
		}
		return gPOG;
	}

	/**
	 * Removes all nodes which are not application-specific nodes and all not connectsTo relationships
	 * 
	 * @param topologyTemplate
	 * @return a topology with just the application-specific components and connectsTo relationships
	 */
	public static TTopologyTemplate createConnectionTopology (TTopologyTemplate topologyTemplate) {
		List<TNodeTemplate> hostingNodeTemplates = topologyTemplate.getNodeTemplates().stream()
			.filter(nt -> !SplittingUtilities.getNodeTemplatesWithoutIncomingHostedOnRelationships(topologyTemplate).contains(nt))
			.collect(Collectors.toList());
		
		topologyTemplate.getNodeTemplateOrRelationshipTemplate().removeAll(hostingNodeTemplates);
		
		List<TRelationshipTemplate> hostingRelationshipTemplates = topologyTemplate.getRelationshipTemplates().stream()
			.filter(rt -> !SplittingUtilities.getBasisRelationshipType(rt.getType()).getName().equalsIgnoreCase("connectsTo"))
			.collect(Collectors.toList());
		
		topologyTemplate.getNodeTemplateOrRelationshipTemplate().removeAll(hostingRelationshipTemplates);
		
		return topologyTemplate;
	}
	
}
