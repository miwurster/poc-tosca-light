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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.winery.common.ids.definitions.NodeTypeId;
import org.eclipse.winery.common.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.model.tosca.TDocumentation;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TNodeType;
import org.eclipse.winery.model.tosca.TRelationshipTemplate;
import org.eclipse.winery.model.tosca.TServiceTemplate;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.repository.backend.BackendUtils;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.repository.splitting.groupprovisioninggraphmodel.Group;
import org.eclipse.winery.repository.splitting.groupprovisioninggraphmodel.GroupProvisioningOrderGraph;
import org.eclipse.winery.repository.splitting.groupprovisioninggraphmodel.OrderRelation;

import org.jgrapht.alg.CycleDetector;
import org.jgrapht.alg.TransitiveReduction;
import org.slf4j.LoggerFactory;

public class SplittingServiceTemplate {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SplittingServiceTemplate.class);
	private static SplittingTopology splittingTopology = new SplittingTopology();

	/**
	 * 
	 * @param id
	 * @return
	 * @throws SplittingException
	 * @throws IOException
	 */
	public static List<ServiceTemplateId> splitServiceTemplate(ServiceTemplateId id) throws SplittingException, IOException {
		List<ServiceTemplateId> splitServiceTemplateIDs = new ArrayList<>();
		IRepository repository = RepositoryFactory.getRepository();
		TServiceTemplate serviceTemplate = repository.getElement(id);
		/*
		TTopologyTemplate splitTopologyTemplate = 
			splittingTopology.hostMatchingWithDefaultHostSelection(splittingTopology.split(serviceTemplate.getTopologyTemplate()));
		*/
		TTopologyTemplate splitTopologyTemplate = serviceTemplate.getTopologyTemplate();
		List<Group> provisioningOrder = determineProvisiongingOrder(splitTopologyTemplate);
		
		for (Group group : provisioningOrder) {
			// create wrapper service template
			ServiceTemplateId splitServiceTemplateId =
				new ServiceTemplateId(
					id.getNamespace().getDecoded(),
					id.getXmlId().getDecoded() + "-" + provisioningOrder.indexOf(group),
					false);
			repository.forceDelete(splitServiceTemplateId);
			repository.flagAsExisting(splitServiceTemplateId);
			
			Queue<TNodeTemplate> queue = new LinkedList<>();
			TServiceTemplate splitServiceTemplate = new TServiceTemplate();
			TDocumentation orderID = new TDocumentation();
			orderID.getContent().add("This Service Template is number " + provisioningOrder.indexOf(group)
				+ " in the provisioning order amd has to be deployed by" + group.getLabel());
			splitServiceTemplate.getDocumentation().add(orderID);
			TTopologyTemplate newTopologyTemplate = new TTopologyTemplate();
			splitServiceTemplate.setTopologyTemplate(newTopologyTemplate);
			newTopologyTemplate.getNodeTemplateOrRelationshipTemplate().addAll(group.getGroupComponents());
			queue.addAll(group.getGroupComponents());
			// Handle host stacks for each contained node in the group
			while (!queue.isEmpty()) {
				TNodeTemplate sourceElement = queue.poll();
				List<TRelationshipTemplate> hostedOnOutgoingRel = 
					SplittingUtilities.getHostedOnOutgoingRelationshipTemplates(splitTopologyTemplate, sourceElement);

				for (TRelationshipTemplate rel : hostedOnOutgoingRel) {
					TRelationshipTemplate clonedRelationshipTemplate = BackendUtils.clone(rel);
					newTopologyTemplate.getNodeTemplateOrRelationshipTemplate().add(clonedRelationshipTemplate);
					TNodeTemplate newSourceElement = newTopologyTemplate.getNodeTemplates().stream()
						.filter(n -> n.getId().equals(sourceElement.getId())).findFirst().get();
					clonedRelationshipTemplate.setSourceNodeTemplate(newSourceElement);
					TNodeTemplate targetElement = ModelUtilities.getTargetNodeTemplateOfRelationshipTemplate(splitTopologyTemplate, clonedRelationshipTemplate);
					TNodeTemplate newTargetElement;
					if (!newTopologyTemplate.getNodeTemplates().stream().anyMatch(n -> n.getId().equals(targetElement.getId()))) {
						newTargetElement = BackendUtils.clone(targetElement);
						newTopologyTemplate.getNodeTemplateOrRelationshipTemplate().add(newTargetElement);
						if (ModelUtilities.getPropertiesKV(newSourceElement) != null &&
							ModelUtilities.getPropertiesKV(newSourceElement).getProperty("State").equalsIgnoreCase("running")) {
							setStatePropertyToRunning(newTargetElement);
						} else {
							List<TNodeTemplate> appSpecificHostedOnPredecessors =
								getApplicationSpecificHostedOnPredecessors(splitTopologyTemplate, targetElement);
							appSpecificHostedOnPredecessors.removeIf(nt -> group.groupComponents.contains(nt));
							if (!appSpecificHostedOnPredecessors.isEmpty()) {
								for (TNodeTemplate nodeTemplate : appSpecificHostedOnPredecessors) {
									int order = provisioningOrder
										.indexOf(provisioningOrder.stream().filter(g -> g.groupComponents.contains(nodeTemplate)).findFirst().get());
									if (order < provisioningOrder.indexOf(group)) {
										setStatePropertyToRunning(newTargetElement);
										TDocumentation input = new TDocumentation();
										input.getContent().add("This Service Template requires input from ServiceTemplate number " + order
											+ " from the Participant " + ModelUtilities.getTargetLabel(nodeTemplate));
										splitServiceTemplate.getDocumentation().add(input);
									}
								}
							}
						}
						if (!SplittingUtilities.getHostedOnOutgoingRelationshipTemplates(splitTopologyTemplate, targetElement).isEmpty()) {
							queue.add(targetElement);
						}
					} else {
						newTargetElement = newTopologyTemplate.getNodeTemplate(targetElement.getId());
					}
					clonedRelationshipTemplate.setTargetNodeTemplate(newTargetElement);
				}
			}
			// Handel outgoing connectsTo relationships
			for (TNodeTemplate appSpecificNode : group.getGroupComponents()) {
				List<TRelationshipTemplate> connectsToOutgoingRels = 
					SplittingUtilities.getConnectsToOutgoingRelationshipTemplates(splitTopologyTemplate, appSpecificNode);
				for (TRelationshipTemplate connectsToRel : connectsToOutgoingRels) {
					TRelationshipTemplate clonedRelationshipTemplate = BackendUtils.clone(connectsToRel);
					TNodeTemplate newSourceElement = newTopologyTemplate.getNodeTemplates().stream()
						.filter(n -> n.getId().equals(appSpecificNode.getId())).findFirst().get();
					clonedRelationshipTemplate.setSourceNodeTemplate(newSourceElement);
					newTopologyTemplate.getNodeTemplateOrRelationshipTemplate().add(clonedRelationshipTemplate);
					TNodeTemplate targetElement = ModelUtilities.getTargetNodeTemplateOfRelationshipTemplate(splitTopologyTemplate, clonedRelationshipTemplate);
					TNodeTemplate newTargetElement;
					if (!newTopologyTemplate.getNodeTemplates().stream().anyMatch(n -> n.getId().equals(targetElement.getId()))) {
						int order = provisioningOrder
							.indexOf(provisioningOrder.stream().filter(g -> g.groupComponents.contains(targetElement)).findFirst().get());
						TDocumentation input = new TDocumentation();
						input.getContent().add("This Service Template requires input from ServiceTemplate number " + order
							+ " from the Participant " + ModelUtilities.getTargetLabel(targetElement));
						splitServiceTemplate.getDocumentation().add(input);
						newTargetElement = BackendUtils.clone(targetElement);
						newTopologyTemplate.getNodeTemplateOrRelationshipTemplate().add(newTargetElement);
						setStatePropertyToRunning(newTargetElement);
						queue.clear();
						queue.add(targetElement);
						while (!queue.isEmpty()) {
							TNodeTemplate sourceElement = queue.poll();
							List<TRelationshipTemplate> outgoinghostingRels = SplittingUtilities.getHostedOnOutgoingRelationshipTemplates(splitTopologyTemplate, sourceElement);
							for (TRelationshipTemplate hostingRel : outgoinghostingRels) {
								TRelationshipTemplate clonedhostingRel = BackendUtils.clone(hostingRel);
								newTopologyTemplate.getNodeTemplateOrRelationshipTemplate().add(clonedhostingRel);
								TNodeTemplate existingSourceElement = newTopologyTemplate.getNodeTemplate(sourceElement.getId());
								TNodeTemplate oldTargetElement = ModelUtilities.getTargetNodeTemplateOfRelationshipTemplate(splitTopologyTemplate, hostingRel);
								if (newTopologyTemplate.getNodeTemplates().stream().anyMatch(n -> n.getId().equals(oldTargetElement.getId()))) {
									TNodeTemplate existingTargetElement = newTopologyTemplate.getNodeTemplate(oldTargetElement.getId());
									clonedhostingRel.setSourceNodeTemplate(existingSourceElement);
									clonedhostingRel.setTargetNodeTemplate(existingTargetElement);
								} else {
									TNodeTemplate clonedTargetElement = BackendUtils.clone(oldTargetElement);
									newTopologyTemplate.getNodeTemplateOrRelationshipTemplate().add(clonedTargetElement);
									clonedhostingRel.setTargetNodeTemplate(clonedTargetElement);
									setStatePropertyToRunning(clonedTargetElement);
									queue.add(oldTargetElement);
								}
							}
						}
						
					} else {
						newTargetElement = newTopologyTemplate.getNodeTemplate(targetElement.getId());
					}
					clonedRelationshipTemplate.setTargetNodeTemplate(newTargetElement);
				}
				
			}
			LOGGER.debug("Persisting...");
			repository.setElement(splitServiceTemplateId, splitServiceTemplate);
			splitServiceTemplateIDs.add(splitServiceTemplateId);
			LOGGER.debug("Persisted.");
		}
		return splitServiceTemplateIDs;
	}
	
	public static List<TNodeTemplate> getApplicationSpecificHostedOnPredecessors (TTopologyTemplate topologyTemplate, TNodeTemplate nodeTemplate) {
		List<TNodeTemplate> appSpecificHostedOnPredecessors = new ArrayList<>();
		
		List<TNodeTemplate> predecessors = SplittingUtilities.getHostedOnPredecessorsOfNodeTemplate(topologyTemplate, nodeTemplate);
		for (TNodeTemplate predecessor : predecessors) {
			if (!SplittingUtilities.getHostedOnPredecessorsOfNodeTemplate(topologyTemplate, predecessor).isEmpty()) {
				return getApplicationSpecificHostedOnPredecessors(topologyTemplate, predecessor);
			} else {
				appSpecificHostedOnPredecessors.add(predecessor);
			}
		}
		return appSpecificHostedOnPredecessors;
	}

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
		TTopologyTemplate cloneTopologyTempplate = BackendUtils.clone(topologyTemplate);
		List<TNodeTemplate> hostingNodeTemplates = cloneTopologyTempplate.getNodeTemplates().stream()
			.filter(nt -> !SplittingUtilities.getNodeTemplatesWithoutIncomingHostedOnRelationships(cloneTopologyTempplate).contains(nt))
			.collect(Collectors.toList());
		
		cloneTopologyTempplate.getNodeTemplateOrRelationshipTemplate().removeAll(hostingNodeTemplates);
		
		List<TRelationshipTemplate> hostingRelationshipTemplates = cloneTopologyTempplate.getRelationshipTemplates().stream()
			.filter(rt -> !SplittingUtilities.getBasisRelationshipType(rt.getType()).getName().equalsIgnoreCase("connectsTo"))
			.collect(Collectors.toList());
		
		cloneTopologyTempplate.getNodeTemplateOrRelationshipTemplate().removeAll(hostingRelationshipTemplates);
		
		return cloneTopologyTempplate;
	}
	
	private static void setStatePropertyToRunning (TNodeTemplate nodeTemplate) {
		Properties nodeTemplateProperties = ModelUtilities.getPropertiesKV(nodeTemplate);
		nodeTemplateProperties.setProperty("State", "running");
		NodeTypeId nodeTypeId = new NodeTypeId(nodeTemplate.getType());
		TNodeType nodeType = RepositoryFactory.getRepository().getElement(nodeTypeId);
		ModelUtilities.setPropertiesKV(ModelUtilities.getWinerysPropertiesDefinition(nodeType), nodeTemplate, nodeTemplateProperties);
	}
}
