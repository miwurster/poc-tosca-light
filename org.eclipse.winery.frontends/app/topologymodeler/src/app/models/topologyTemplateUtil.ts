/********************************************************************************
 * Copyright (c) 2018-2020 Contributors to the Eclipse Foundation
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
import { EntityType, TNodeTemplate, TPolicyType, TRelationshipTemplate, TTopologyTemplate } from './ttopology-template';
import { QName } from './qname';
import { DifferenceStates, ToscaDiff, VersionUtils } from './ToscaDiff';
import { Visuals } from './visuals';
import { NgRedux } from '@angular-redux/store';
import { IWineryState } from '../redux/store/winery.store';
import { WineryActions } from '../redux/actions/winery.actions';
import { CapabilityDefinitionModel } from './capabilityDefinitionModel';
import { EntityTypesModel } from './entityTypesModel';
import { CapabilityModel } from './capabilityModel';
import { RequirementDefinitionModel } from './requirementDefinitonModel';
import { RequirementModel } from './requirementModel';

export class TopologyTemplateUtil {

    static HORIZONTAL_OFFSET_FOR_NODES_WITHOUT_COORDINATES = 350;
    static VERTICAL_OFFSET_FOR_NODES_WITHOUT_COORDINATES = 200;

    static createTNodeTemplateFromObject(node: TNodeTemplate, nodeVisuals: Visuals[],
                                         isYaml: boolean, types: EntityTypesModel, state?: DifferenceStates): TNodeTemplate {
        const nodeVisualsObject = this.getNodeVisualsForNodeTemplate(node.type, nodeVisuals, state);
        let properties;
        if (node.properties) {
            properties = node.properties;
        }

        let nameSpace: string;
        let targetLocationKey: string;
        let providerKey: string;
        let regionKey: string;
        let otherAttributes;
        for (const key in node.otherAttributes) {
            if (node.otherAttributes.hasOwnProperty(key)) {
                nameSpace = key.substring(key.indexOf('{'), key.indexOf('}') + 1);
                if (nameSpace) {
                    if (key.substring(key.indexOf('}') + 1) === 'location') {
                        targetLocationKey = key;
                    }
                    if (key.substring(key.indexOf('}') + 1) === 'provider') {
                        providerKey = key;
                    }
                    if (key.substring(key.indexOf('}') + 1) === 'region') {
                        regionKey = key;
                    }
                    otherAttributes = {
                        [nameSpace + 'location']: node.otherAttributes[targetLocationKey],
                        [nameSpace + 'provider']: node.otherAttributes[providerKey],
                        [nameSpace + 'region']: node.otherAttributes[regionKey],
                        [nameSpace + 'x']: node.x,
                        [nameSpace + 'y']: node.y
                    };
                } else if (key === 'location') {
                    targetLocationKey = 'location';
                }
            }
        }

        // for Yaml, we add missing capabilities, find their types, and fix their ids, we also fix requirement ids (to avoid duplicates)
        if (isYaml) {
            if (!types) {
                // todo ensure entity types model is always available. See TopologyTemplateUtil.updateTopologyTemplate
                console.error('The required entity types model is not available! Unexpected behavior');
            }
            // look for missing capabilities and add them
            const capDefs: CapabilityDefinitionModel[] = this.getCapabilityDefinitionsOfNodeType(node.type, types);
            if (!node.capabilities || !node.capabilities.capability) {
                node.capabilities = { capability: [] };
            }
            capDefs.forEach(def => {
                const capAssignment = node.capabilities.capability.find(capAss => capAss.name === def.name);
                const cap = CapabilityModel.fromCapabilityDefinitionModel(def);

                if (capAssignment) {
                    const capAssignmentIndex = node.capabilities.capability.indexOf(capAssignment);
                    cap.properties = capAssignment.properties;
                    node.capabilities.capability.splice(capAssignmentIndex, 1);
                }

                node.capabilities.capability.push(cap);

                cap.id = `${node.id}_cap_${cap.name}`;
            });

            if (node.requirements && node.requirements.requirement) {
                node.requirements.requirement.forEach(req => req.id = `${node.id}_req_${req.name}`);
            } else {
                // If the requirements are not found in the yaml representation (therefore not available here),
                // they are acquired from the inheritance hierarchy.
                const reqDefs: RequirementDefinitionModel[] = this.getCapabilityDefinitionsOfNodeType(node.type, types);
                // TODO: Check whenever there exists a case where the requirement is not initialized.
                node.requirements.requirement = [];
                for (const reqDef of reqDefs) {
                    node.requirements.requirement.push(RequirementModel.fromRequirementDefinition(reqDef));
                }
            }
        }

        return new TNodeTemplate(
            properties ? properties : {},
            node.id,
            node.type,
            node.name,
            node.minInstances,
            node.maxInstances,
            nodeVisualsObject,
            node.documentation ? node.documentation : [],
            node.any ? node.any : [],
            otherAttributes,
            node.x,
            node.y,
            node.capabilities ? node.capabilities : {},
            node.requirements ? node.requirements : {},
            node.deploymentArtifacts ? node.deploymentArtifacts : {},
            node.policies ? node.policies : {},
            state
        );
    }

    static getCapabilityDefinitionsOfNodeType(nodeType: string, entityTypes: EntityTypesModel): CapabilityDefinitionModel[] {
        const listOfEffectiveCapabilityDefinitions: CapabilityDefinitionModel[] = [];
        const listOfBequeathingNodeTypes = TopologyTemplateUtil.getInheritanceAncestry(nodeType, entityTypes.unGroupedNodeTypes);
        for (const currentNodeType of listOfBequeathingNodeTypes) {
            if (currentNodeType.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].capabilityDefinitions &&
                currentNodeType.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].capabilityDefinitions.capabilityDefinition) {
                for (const capabilityDefinition of currentNodeType.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0]
                    .capabilityDefinitions.capabilityDefinition) {
                    if (!listOfEffectiveCapabilityDefinitions
                        .some(value => value.name === capabilityDefinition.name)) {
                        listOfEffectiveCapabilityDefinitions.push(capabilityDefinition);
                    }
                }
            }
        }
        return listOfEffectiveCapabilityDefinitions;
    }

    static getRequirementDefinitionsOfNodeType(nodeType: string, entityTypes: EntityTypesModel): RequirementDefinitionModel[] {
        const listOfEffectiveRequirementDefinitions: RequirementDefinitionModel[] = [];
        const listOfBequeathingNodeTypes = TopologyTemplateUtil.getInheritanceAncestry(nodeType, entityTypes.unGroupedNodeTypes);
        for (const currentNodeType of listOfBequeathingNodeTypes) {
            if (currentNodeType.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].requirementDefinitions &&
                currentNodeType.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].requirementDefinitions.requirementDefinition) {
                for (const requirementDefinition of currentNodeType.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0]
                    .requirementDefinitions.requirementDefinition) {
                    if (!listOfEffectiveRequirementDefinitions
                        .some(value => value.name === requirementDefinition.name)) {
                        listOfEffectiveRequirementDefinitions.push(requirementDefinition);
                    }
                }
            }
        }
        return listOfEffectiveRequirementDefinitions;
    }

    static createTRelationshipTemplateFromObject(relationship: TRelationshipTemplate, state?: DifferenceStates) {
        return new TRelationshipTemplate(
            relationship.sourceElement,
            relationship.targetElement,
            relationship.name,
            relationship.id,
            relationship.type,
            relationship.properties,
            relationship.documentation,
            relationship.any,
            relationship.otherAttributes,
            state,
            relationship.policies
        );
    }

    static getNodeVisualsForNodeTemplate(nodeType: string, nodeVisuals: Visuals[], state?: DifferenceStates): Visuals {
        for (const visual of nodeVisuals) {
            const qName = new QName(visual.typeId);
            const localName = qName.localName;
            if (localName === new QName(nodeType).localName) {
                const color = !state ? visual.color : VersionUtils.getElementColorByDiffState(state);
                return <Visuals>{
                    color: color,
                    typeId: nodeType,
                    imageUrl: visual.imageUrl,
                    pattern: visual.pattern
                };
            }
        }
    }

    static initNodeTemplates(nodeTemplateArray: Array<TNodeTemplate>, nodeVisuals: Visuals[], isYaml: boolean, types: EntityTypesModel,
                             topologyDifferences?: [ToscaDiff, TTopologyTemplate]): Array<TNodeTemplate> {
        const nodeTemplates: TNodeTemplate[] = [];
        if (nodeTemplateArray.length > 0) {
            nodeTemplateArray.forEach((node, index) => {
                const offset = 10 * index;
                if (!node.x || !node.y) {
                    node.x = this.HORIZONTAL_OFFSET_FOR_NODES_WITHOUT_COORDINATES + offset;
                    node.y = this.VERTICAL_OFFSET_FOR_NODES_WITHOUT_COORDINATES + offset;
                }
                const state = topologyDifferences ? DifferenceStates.UNCHANGED : null;
                nodeTemplates.push(
                    TopologyTemplateUtil.createTNodeTemplateFromObject(node, nodeVisuals, isYaml, types, state)
                );
            });
        }

        return nodeTemplates;
    }

    /**
     * Generates default properties from node types or relationshipTypes
     * The assumption appears to be that types only add new properties and never change existing ones (e.g., change type or default value)
     * todo why name not qname?
     * todo use the 'getInheritanceAncestry' method
     * @param name
     * @param entities
     * @return properties
     */
    static getDefaultPropertiesFromEntityTypes(name: string, entities: EntityType[]): any {
        for (const element of entities) {
            if (element.name === name) {
                // if propertiesDefinition is defined it's a XML property
                if (element.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].propertiesDefinition
                    && element.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].propertiesDefinition.element) {
                    return {
                        any: element.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].propertiesDefinition.element
                    };
                } else { // otherwise KV properties or no properties at all
                    let inheritedProperties = {};
                    if (this.hasParentType(element)) {
                        let parent = element.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].derivedFrom.typeRef;
                        let continueFlag;

                        while (parent) {
                            continueFlag = false;
                            for (const parentElement of entities) {
                                if (parentElement.qName === parent) {
                                    if (this.hasKVPropDefinition(parentElement)) {
                                        inheritedProperties = {
                                            ...inheritedProperties, ...TopologyTemplateUtil.getKVProperties(parentElement)
                                        };
                                    }
                                    if (this.hasParentType(parentElement)) {
                                        parent = parentElement.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].derivedFrom.typeRef;
                                        continueFlag = true;
                                    }
                                    break;
                                }
                            }
                            if (continueFlag) {
                                continue;
                            }
                            parent = null;
                        }
                    }

                    let typeProperties = {};
                    if (this.hasKVPropDefinition(element)) {
                        typeProperties = TopologyTemplateUtil.getKVProperties(element);
                    }

                    const mergedProperties = { ...inheritedProperties, ...typeProperties };

                    return {
                        kvproperties: { ...mergedProperties }
                    };
                }
            }
        }
    }

    static hasKVPropDefinition(element: EntityType): boolean {
        return (element && element.full &&
            element.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].any &&
            element.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].any.length > 0 &&
            element.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].any[0].propertyDefinitionKVList
        );
    }

    static hasParentType(element: EntityType): boolean {
        return (element && element.full
            && element.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0]
            && element.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].derivedFrom
        );
    }

    /**
     * This function gets KV properties of a type and sets their default values
     * @param any type: the element type, e.g. capabilityType, requirementType etc.
     * @returns newKVProperties: KV Properties as Object
     */
    static getKVProperties(type: any): any {
        const newKVProperies = {};
        const kvProperties = type.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].any[0].propertyDefinitionKVList;
        for (const obj of kvProperties) {
            const key = obj.key;
            let value;
            if (!obj.value && obj.defaultValue) {
                value = obj.defaultValue;
            } else if (!obj.value) {
                // TODO quick hack: set a "system" default
                value = 'N/A';
            } else {
                value = obj.value;
            }
            newKVProperies[key] = value;
        }
        return newKVProperies;
    }

    static getParent(element: EntityType, entities: EntityType[]): EntityType {
        if (this.hasParentType(element)) {
            const parentQName = element.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].derivedFrom.type;
            return entities.find(entity => entity.qName === parentQName);
        }
        return null;
    }

    static getInheritanceAncestry(entityType: string, entityTypes: EntityType[]): EntityType[] {
        const entity = entityTypes.find(type => type.qName === entityType);
        const result = [];

        if (entity) {
            result.push(entity);
            let parent = this.getParent(entity, entityTypes);

            while (parent) {
                result.push(parent);
                parent = this.getParent(parent, entityTypes);
            }
        }

        return result;
    }

    /**
     * Gets the active set of allowed target node types for this YAML policy type
     * i.e., returns the targets array which is the lowest possible.
     * @param policyTypeQName
     * @param policyTypes
     */
    static getActiveTargetsOfYamlPolicyType(policyTypeQName: string, policyTypes: TPolicyType[]): string[] {
        const hierarchy = this.getInheritanceAncestry(policyTypeQName, policyTypes);
        let result = [];

        for (const type of hierarchy) {
            if ((<TPolicyType>type).targets) {
                result = (<TPolicyType>type).targets;
                break;
            }
        }

        return result;
    }

    static getActiveKVPropertiesOfTemplateElement(templateElementProperties: any, typeQName: string, entityTypes: EntityType[]): any {
        const typeName = new QName(typeQName).localName;
        const defaultTypeProperties = this.getDefaultPropertiesFromEntityTypes(typeName, entityTypes);
        const result = {};

        if (defaultTypeProperties && defaultTypeProperties.kvproperties) {
            Object.keys(defaultTypeProperties.kvproperties).forEach(currentPropKey => {

                if (templateElementProperties && templateElementProperties.kvproperties &&
                    Object.keys(templateElementProperties.kvproperties).some(tempPropertyKey => tempPropertyKey === currentPropKey)) {
                    result[currentPropKey] = templateElementProperties.kvproperties[currentPropKey];
                } else {
                    result[currentPropKey] = defaultTypeProperties.kvproperties[currentPropKey];
                }
            });
        }

        return { kvproperties: result };
    }

    static initRelationTemplates(relationshipTemplateArray: Array<TRelationshipTemplate>,
                                 topologyDifferences?: [ToscaDiff, TTopologyTemplate]): Array<TRelationshipTemplate> {
        const relationshipTemplates: TRelationshipTemplate[] = [];
        if (relationshipTemplateArray.length > 0) {
            relationshipTemplateArray.forEach(relationship => {
                const state = topologyDifferences ? DifferenceStates.UNCHANGED : null;
                relationshipTemplates.push(
                    TopologyTemplateUtil.createTRelationshipTemplateFromObject(relationship, state)
                );
            });
        }

        return relationshipTemplates;
    }

    static updateTopologyTemplate(ngRedux: NgRedux<IWineryState>, wineryActions: WineryActions, topology: TTopologyTemplate, isYaml: boolean) {
        const wineryState = ngRedux.getState().wineryState;

        // Required because if the palette is open, the last node inserted will be bound to the mouse movement.
        ngRedux.dispatch(wineryActions.sendPaletteOpened(false));

        wineryState.currentJsonTopology.nodeTemplates
            .forEach(
                node => ngRedux.dispatch(wineryActions.deleteNodeTemplate(node.id))
            );
        wineryState.currentJsonTopology.relationshipTemplates
            .forEach(
                relationship => ngRedux.dispatch(wineryActions.deleteRelationshipTemplate(relationship.id))
            );

        TopologyTemplateUtil.initNodeTemplates(topology.nodeTemplates, wineryState.nodeVisuals, isYaml, null)
            .forEach(
                node => ngRedux.dispatch(wineryActions.saveNodeTemplate(node))
            );
        TopologyTemplateUtil.initRelationTemplates(topology.relationshipTemplates)
            .forEach(
                relationship => ngRedux.dispatch(wineryActions.saveRelationship(relationship))
            );
    }
}
