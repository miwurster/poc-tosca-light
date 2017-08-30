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
import org.jgrapht.alg.TransitiveClosure;
import org.jgrapht.alg.TransitiveReduction;
import org.jgrapht.experimental.dag.DirectedAcyclicGraph;
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
			TDocumentation orderDocumentation = new TDocumentation();
			orderDocumentation.getContent().add("This Service Template is number " + provisioningOrder.indexOf(group)
				+ " in the provisioning order amd has to be deployed by " + group.getLabel());
			splitServiceTemplate.getDocumentation().add(orderDocumentation);
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
										addInputDocumentation(splitServiceTemplate, order, ModelUtilities.getTargetLabel(nodeTemplate).get());
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
						addInputDocumentation(splitServiceTemplate, order, ModelUtilities.getTargetLabel(targetElement).get());
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
		TransitiveClosure TRANSITIVECLOSURE = TransitiveClosure.INSTANCE;
		TransitiveReduction TRANSITIVEREDUCTION = TransitiveReduction.INSTANCE;
		
		GroupProvisioningOrderGraph tempGPOG = new GroupProvisioningOrderGraph();
		TRANSITIVEREDUCTION.reduce(gPoG);
		List<OrderRelation> orderRelationsQueue = gPoG.edgeSet().stream()
			.filter(e -> e.getSource().getLabel().equals(e.getTarget().getLabel())).collect(Collectors.toList());
		
		while (!orderRelationsQueue.isEmpty()) {
			OrderRelation relation = orderRelationsQueue.get(0);
			tempGPOG = contractGroupsInGPOG(gPoG, relation.getSource(), relation.getTarget());
				if (tempGPOG != null) {
					Group compressedGroup = tempGPOG.vertexSet().stream()
						.filter(v -> v.getGroupComponents().containsAll(relation.getSource().getGroupComponents())).findFirst().get();
					
					orderRelationsQueue.remove(relation);
					List<OrderRelation> replacedOutgoingRelations = orderRelationsQueue.stream()
						.filter(r -> compressedGroup.getGroupComponents().containsAll(r.getSource().getGroupComponents())).collect(Collectors.toList());
					List<OrderRelation> replacingOutgoingRelations = tempGPOG.outgoingEdgesOf(compressedGroup).stream()
						.filter(r -> replacedOutgoingRelations.stream().anyMatch(or -> or.getTarget().equals(r.getTarget())))
						.collect(Collectors.toList());
					orderRelationsQueue.removeAll(replacedOutgoingRelations);
					orderRelationsQueue.addAll(replacingOutgoingRelations);

					List<OrderRelation> replacedIncomingRelations = orderRelationsQueue.stream()
						.filter(r -> r.getTarget().equals(relation.getTarget())).collect(Collectors.toList());
					List<OrderRelation> replacingIncomingRelations = tempGPOG.incomingEdgesOf(compressedGroup).stream()
						.filter(r -> replacedIncomingRelations.stream().anyMatch(or -> or.getSource().equals(r.getSource())))
						.collect(Collectors.toList());
					orderRelationsQueue.removeAll(replacedIncomingRelations);
					orderRelationsQueue.addAll(replacingIncomingRelations);
					
					gPoG.clearGraph();
					tempGPOG.vertexSet().forEach(v -> gPoG.addVertex(v));
					tempGPOG.edgeSet().forEach(e -> gPoG.addEdge(e.getSource(), e.getTarget()));
					tempGPOG.clearGraph();
				} 
				orderRelationsQueue.remove(relation);
		}
		Set<String> labels = new HashSet<>();
		gPoG.vertexSet().forEach(v -> labels.add(v.getLabel()));
		DirectedAcyclicGraph<Group, OrderRelation> transitiveClousure = new DirectedAcyclicGraph(OrderRelation.class);
		gPoG.vertexSet().forEach(v -> transitiveClousure.addVertex(v));
		gPoG.edgeSet().forEach(e -> transitiveClousure.addEdge(e.getSource(), e.getTarget()));
		TRANSITIVECLOSURE.closeDirectedAcyclicGraph(transitiveClousure);
		gPoG.clearGraph();
		transitiveClousure.vertexSet().forEach(v -> gPoG.addVertex(v));
		transitiveClousure.edgeSet().forEach(e -> gPoG.addEdge(e.getSource(), e.getTarget()));
		
		for (String label : labels) {
			List<Group> groupsWithSameLabel = gPoG.vertexSet().stream()
				.filter(v -> v.getLabel().equals(label)).collect(Collectors.toList());
			
			if (groupsWithSameLabel.size() > 1) {
				Queue<Group> groupQueue = new LinkedList<>();
				groupQueue.addAll(groupsWithSameLabel);

				while (!groupQueue.isEmpty()) {
					Group group = groupQueue.poll();
					List<Group> connectedGroups = new ArrayList<>();
					gPoG.outgoingEdgesOf(group).stream().forEach(o -> connectedGroups.add(o.getTarget()));
					gPoG.incomingEdgesOf(group).stream().forEach(o -> connectedGroups.add(o.getSource()));

					List<Group> transitiveIndependentGroups = groupsWithSameLabel.stream().filter(g -> !g.equals(group))
						.filter(g -> !connectedGroups.contains(g)).collect(Collectors.toList());
					if (!transitiveIndependentGroups.isEmpty()) {
						tempGPOG = contractGroupsInGPOG(gPoG, transitiveIndependentGroups.get(0), group);
						if (tempGPOG != null) {
							groupsWithSameLabel.remove(group);						
							gPoG.clearGraph();
							tempGPOG.vertexSet().forEach(v -> gPoG.addVertex(v));
							tempGPOG.edgeSet().forEach(e -> gPoG.addEdge(e.getSource(), e.getTarget()));
							tempGPOG.clearGraph();
						}
					}
				}				
			}
		}
		return gPoG;
	}

	/**
	 * 
	 * @param gPoG
	 * @param mergingGroup
	 * @param mergedGroup
	 * @return either the compressed gPoG or null if the compression results in a loop
	 */
	public static GroupProvisioningOrderGraph contractGroupsInGPOG(GroupProvisioningOrderGraph gPoG, Group mergingGroup, Group mergedGroup) {
		GroupProvisioningOrderGraph graph = new GroupProvisioningOrderGraph();
		gPoG.vertexSet().forEach(v -> graph.addVertex(v));
		gPoG.edgeSet().forEach(e -> graph.addEdge(e.getSource(), e.getTarget()));
		CycleDetector cycleDetector = new CycleDetector(graph);
		// Former target element is added to the source element group
		mergingGroup.addAllNodeTemplatesToGroupComponents(mergedGroup.groupComponents);
		
		// For each edge related to the former target element a new edge has to be added
		Set<OrderRelation> outgoingRelations = graph.outgoingEdgesOf(mergedGroup).stream().collect(Collectors.toSet());
		Set<OrderRelation> incomingRelations = graph.incomingEdgesOf(mergedGroup).stream()
			.filter(ir -> !ir.getSource().equals(mergingGroup)).collect(Collectors.toSet());
		outgoingRelations.forEach(or -> graph.addEdge(mergingGroup, or.getTarget()));
		incomingRelations.forEach(ir -> graph.addEdge(ir.getSource(), mergingGroup));
		
		// Old edges and the former target element are removed
		graph.removeAllEdges(outgoingRelations);
		graph.removeAllEdges(incomingRelations);
		Set<TNodeTemplate> movedGroupSet = mergedGroup.getGroupComponents();
		graph.removeAllEdges(graph.getAllEdges(mergingGroup, mergedGroup));
		graph.removeAllEdges(graph.getAllEdges(mergedGroup, mergingGroup));
		graph.removeVertex(mergedGroup);
		
		if (cycleDetector.detectCycles()) {
				mergingGroup.getGroupComponents().removeAll(movedGroupSet);
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
	
	/**
	 * Add a new documentation entry to the ServiceTemplate iff already no entry for the specified cSARProvisioningOrderNumber
	 * and partnerLabel is available. For each required input from another CSAR only one entry should be added
	 * 
	 * @param serviceTemplate
	 * @param cSARProvisioningOrderNumber
	 * @param partnerLabel
	 */
	private static void addInputDocumentation (TServiceTemplate serviceTemplate, int cSARProvisioningOrderNumber, String partnerLabel) {
		String cSARNumber = String.valueOf(cSARProvisioningOrderNumber);
		List<TDocumentation> documentations = serviceTemplate.getDocumentation();
		List<Object> documentationentries = new ArrayList<>(); 
		documentations.stream().forEach(d -> documentationentries.addAll(d.getContent()));
		
		if (!documentationentries.stream().anyMatch(o -> o instanceof String && ((String) o).contains(cSARNumber))) {
			TDocumentation input = new TDocumentation();
			input.getContent().add("This Service Template requires input from ServiceTemplate number " + cSARProvisioningOrderNumber
				+ " from the Participant " + partnerLabel);
			serviceTemplate.getDocumentation().add(input);
		}
	}
}
