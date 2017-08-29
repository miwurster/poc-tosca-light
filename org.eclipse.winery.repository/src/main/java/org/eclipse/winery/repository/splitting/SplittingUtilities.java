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
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.eclipse.winery.common.ids.definitions.RelationshipTypeId;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TRelationshipTemplate;
import org.eclipse.winery.model.tosca.TRelationshipType;
import org.eclipse.winery.model.tosca.TTopologyTemplate;
import org.eclipse.winery.model.tosca.utils.ModelUtilities;
import org.eclipse.winery.repository.backend.RepositoryFactory;

public class SplittingUtilities {

	/**
	 * Find all node templates which has no incoming hostedOn relationships (highest level nodes)
	 *
	 * @return list of node templates
	 */
	public static List<TNodeTemplate> getNodeTemplatesWithoutIncomingHostedOnRelationships(TTopologyTemplate topologyTemplate) {

		return topologyTemplate.getNodeTemplates()
			.stream()
			.filter(nt -> getHostedOnPredecessorsOfNodeTemplate(topologyTemplate, nt).isEmpty())
			.collect(Collectors.toList());
	}

	/**
	 * Find all predecessors of a node template. the predecessor is the target of a hostedOn relationship to the
	 * nodeTemplate
	 *
	 * @param nodeTemplate for which all predecessors should be found
	 * @return list of predecessors
	 */
	public static List<TNodeTemplate> getHostedOnPredecessorsOfNodeTemplate(TTopologyTemplate topologyTemplate, TNodeTemplate nodeTemplate) {
		List<TNodeTemplate> predecessorNodeTemplates = new ArrayList<>();
		predecessorNodeTemplates.clear();
		List<TRelationshipTemplate> incomingRelationships = ModelUtilities.getIncomingRelationshipTemplates(topologyTemplate, nodeTemplate);
		for (TRelationshipTemplate relationshipTemplate : incomingRelationships) {
			if (getBasisRelationshipType(relationshipTemplate.getType()).getValidTarget() != null &&
				getBasisRelationshipType(relationshipTemplate.getType()).getValidTarget().getTypeRef().getLocalPart().equalsIgnoreCase("Container")) {
				predecessorNodeTemplates.add(ModelUtilities.getSourceNodeTemplateOfRelationshipTemplate(topologyTemplate, relationshipTemplate));
			}
		}
		return predecessorNodeTemplates;
	}
	
	public static List<TRelationshipTemplate> getHostedOnOutgoingRelationshipTemplates (TTopologyTemplate topologyTemplate, TNodeTemplate nodeTemplate) {
		List<TRelationshipTemplate> hostedOnOutgoingRelationshipTemplates = new ArrayList<>();
		for (TRelationshipTemplate relationshipTemplate : ModelUtilities.getOutgoingRelationshipTemplates(topologyTemplate, nodeTemplate)) {
			if (SplittingUtilities.getBasisRelationshipType(relationshipTemplate.getType()).getValidTarget() != null &&
				SplittingUtilities.getBasisRelationshipType(relationshipTemplate.getType()).getValidTarget().getTypeRef().getLocalPart().equalsIgnoreCase("Container")) {
				hostedOnOutgoingRelationshipTemplates.add(relationshipTemplate);
			}
		}
		return hostedOnOutgoingRelationshipTemplates;
	}

	public static List<TRelationshipTemplate> getConnectsToOutgoingRelationshipTemplates (TTopologyTemplate topologyTemplate, TNodeTemplate nodeTemplate) {
		List<TRelationshipTemplate> connectsToOutgoingRelationshipTemplates = new ArrayList<>();
		for (TRelationshipTemplate relationshipTemplate : ModelUtilities.getOutgoingRelationshipTemplates(topologyTemplate, nodeTemplate)) {
			if (SplittingUtilities.getBasisRelationshipType(relationshipTemplate.getType()).getValidTarget() != null &&
				SplittingUtilities.getBasisRelationshipType(relationshipTemplate.getType()).getValidTarget().getTypeRef().getLocalPart().equalsIgnoreCase("Endpoint")) {
				connectsToOutgoingRelationshipTemplates.add(relationshipTemplate);
			}
		}
		return connectsToOutgoingRelationshipTemplates;
	}
	
	public static TRelationshipType getBasisRelationshipType(QName relationshipTypeQName) {
		RelationshipTypeId parentRelationshipTypeId = new RelationshipTypeId(relationshipTypeQName);
		TRelationshipType parentRelationshipType = RepositoryFactory.getRepository().getElement(parentRelationshipTypeId);
		TRelationshipType basisRelationshipType = parentRelationshipType;

		while (parentRelationshipType != null) {
			basisRelationshipType = parentRelationshipType;

			if (parentRelationshipType.getDerivedFrom() != null) {
				relationshipTypeQName = parentRelationshipType.getDerivedFrom().getTypeRef();
				parentRelationshipTypeId = new RelationshipTypeId(relationshipTypeQName);
				parentRelationshipType = RepositoryFactory.getRepository().getElement(parentRelationshipTypeId);
			} else {
				parentRelationshipType = null;
			}
		}
		return basisRelationshipType;
	}
}
