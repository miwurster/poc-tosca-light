/*******************************************************************************
 * Copyright (c) 2020 Contributors to the Eclipse Foundation
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

import { EntityTypesModel } from './entityTypesModel';
import { CapabilityDefinitionModel } from './capabilityDefinitionModel';
import { RequirementDefinitionModel } from './requirementDefinitonModel';
import { EntityType, TPolicyType } from './ttopology-template';
import { QName } from './qname';
import { TopologyTemplateUtil } from './topologyTemplateUtil';

export class InheritanceUtils {

    static getCapabilityDefinitionsOfNodeType(nodeType: string, entityTypes: EntityTypesModel): CapabilityDefinitionModel[] {
        const listOfEffectiveCapabilityDefinitions: CapabilityDefinitionModel[] = [];
        const listOfBequeathingNodeTypes = this.getInheritanceAncestry(nodeType, entityTypes.unGroupedNodeTypes);
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
        const listOfBequeathingNodeTypes = this.getInheritanceAncestry(nodeType, entityTypes.unGroupedNodeTypes);
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

    static hasParentType(element: EntityType): boolean {
        return (element && element.full
            && element.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0]
            && element.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].derivedFrom
        );
    }

    /**
     * Gets the active set of allowed target node types for this YAML policy type
     * i.e., returns the targets array which is the lowest possible.
     * @param policyTypeQName
     * @param policyTypes
     */
    static getEffectiveTargetsOfYamlPolicyType(policyTypeQName: string, policyTypes: TPolicyType[]): string[] {
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

    static getEffectiveKVPropertiesOfTemplateElement(templateElementProperties: any, typeQName: string, entityTypes: EntityType[]): any {
        const typeName = new QName(typeQName).localName;
        const defaultTypeProperties = TopologyTemplateUtil.getDefaultPropertiesFromEntityTypes(typeName, entityTypes);
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

}
