/*******************************************************************************
 * Copyright (c) 2013 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.repository.completion.analyzer;

import org.eclipse.winery.common.ids.definitions.NodeTypeId;
import org.eclipse.winery.common.ids.definitions.RelationshipTypeId;
import org.eclipse.winery.common.ids.definitions.RequirementTypeId;
import org.eclipse.winery.model.tosca.*;
import org.eclipse.winery.repository.backend.IRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class contains several methods to analyze the content of a TOSCA {@link TTopologyTemplate} and to fill a data model
 * with the analyzed information. This class serves the access to all types and templates of a topology.
 */
public class TOSCAAnalyzer {

    private IRepository repository;
    private List<TRequirementType> requirementTypes;
    private List<TRelationshipType> relationshipTypes;
    private List<TNodeType> nodeTypes;

    private List <TNodeTemplate> nodeTemplates;
    private List<TRelationshipTemplate> relationshipTemplates;
    private List<TRequirement> requirements;

    public TOSCAAnalyzer(IRepository repository) {

        requirementTypes = repository.getAllDefinitionsChildIds(RequirementTypeId.class).stream().map(id -> repository.getElement(id)).collect(Collectors.toList());
        relationshipTypes = repository.getAllDefinitionsChildIds(RelationshipTypeId.class).stream().map(id -> repository.getElement(id)).collect(Collectors.toList());
        nodeTypes = repository.getAllDefinitionsChildIds(NodeTypeId.class).stream().map(id -> repository.getElement(id)).collect(Collectors.toList());

        // lists containing the elements of a topology
        nodeTemplates = new ArrayList<TNodeTemplate>();
        relationshipTemplates = new ArrayList<TRelationshipTemplate>();
        requirements = new ArrayList<TRequirement>();
        
        this.repository = repository;
    }
    


    /**
     * This method analyzes the TOSCA {@link TTopologyTemplate} for {@link TNodeTemplate}s, {@link TRelationshipTemplate}s
     * and existing {@link TRequirement}s and adds them to a list.
     *
     * @param topology the TOSCA {@link TTopologyTemplate}
     */
    public void analyzeTOSCATopology(TTopologyTemplate topology) {

        // fill the data model with content of the topology
        List<TEntityTemplate> templateNodes = topology.getNodeTemplateOrRelationshipTemplate();

        for (TEntityTemplate entityTemplate : templateNodes) {
            if (entityTemplate instanceof TNodeTemplate) {
                // add the node templates and their requirements to the data model
                nodeTemplates.add((TNodeTemplate) entityTemplate);
                if (((TNodeTemplate) entityTemplate).getRequirements() != null) {
                    requirements.addAll(((TNodeTemplate) entityTemplate).getRequirements().getRequirement());
                }
            } else if (entityTemplate instanceof TRelationshipTemplate) {
                // add RelationshipTemplates
                relationshipTemplates.add((TRelationshipTemplate) entityTemplate);
            }
        }
    }

    /**
     * Returns the {@link TNodeTemplate}s of the topology.
     *
     * @return the {@link TNodeTemplate}s as a list
     */
    public List<TNodeTemplate> getNodeTemplates() {
        return nodeTemplates;
    }

    /**
     * Returns the {@link TRelationshipTemplate}s of the topology.
     *
     * @return the {@link TRelationshipTemplate}s as a list
     */
    public List<TRelationshipTemplate> getRelationshipTemplates() {
        return relationshipTemplates;
    }

    /**
     * Returns the {@link TRequirement}s of the topology.
     *
     * @return the {@link TRequirement}s as a list
     */
    public List<TRequirement> getRequirements() {
        return requirements;
    }
    
    /**
     * Clears all the templates from the data model before the analysis of a topology is restarted.
     */
    public void clear() {
        nodeTemplates.clear();
        relationshipTemplates.clear();
        requirements.clear();
    }

    public IRepository getRepository() {
        return repository;
    }

    public List<TRequirementType> getRequirementTypes() {
        return requirementTypes;
    }

    public List<TRelationshipType> getRelationshipTypes() {
        return relationshipTypes;
    }

    public List<TNodeType> getNodeTypes() {
        return nodeTypes;
    }
}
