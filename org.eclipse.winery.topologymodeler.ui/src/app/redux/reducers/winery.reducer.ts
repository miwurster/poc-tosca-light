/********************************************************************************
 * Copyright (c) 2017 Contributors to the Eclipse Foundation
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

import { Action } from 'redux';
import {
    DecMaxInstances,
    DecMinInstances,
    DeleteNodeAction,
    IncMaxInstances,
    IncMinInstances,
    SaveNodeTemplateAction,
    SaveRelationshipAction,
    SendCurrentNodeIdAction,
    SendPaletteOpenedAction,
    SetCababilityAction,
    SetCapPropertyAction,
    SetDepArtifactsPropertyAction,
    SetDeploymentArtifactsAction,
    SetPoliciesPropertyAction,
    SetPolicyAction,
    SetPropertyAction,
    SetReqPropertyAction,
    SetRequirementAction,
    SetTargetLocPropertyAction,
    SidebarMaxInstanceChanges,
    SidebarMinInstanceChanges,
    SidebarNodeNamechange,
    SidebarStateAction,
    UpdateNodeCoordinatesAction,
    UpdateRelationshipNameAction,
    WineryActions
} from '../actions/winery.actions';
import { TNodeTemplate, TRelationshipTemplate, TTopologyTemplate } from 'app/models/ttopology-template';
import { TDeploymentArtifact } from '../../models/artifactsModalData';

export interface WineryState {
    currentPaletteOpenedState: boolean;
    sidebarContents: any;
    currentJsonTopology: TTopologyTemplate;
    currentNodeData: any;
}

export const INITIAL_WINERY_STATE: WineryState = {
    currentPaletteOpenedState: false,
    sidebarContents: {
        sidebarVisible: false,
        nodeClicked: false,
        id: '',
        nameTextFieldValue: '',
        type: '',
        minInstances: 1,
        maxInstances: 1
    },
    currentJsonTopology: new TTopologyTemplate,
    currentNodeData: {
        id: '',
        focus: false
    }
};

/**
 * Reducer for the rest of the topology modeler
 */
export const WineryReducer =
    function (lastState: WineryState = INITIAL_WINERY_STATE, action: Action): WineryState {
        switch (action.type) {
            case WineryActions.SEND_PALETTE_OPENED:
                const paletteOpened: boolean = (<SendPaletteOpenedAction>action).paletteOpened;
                return <WineryState>{
                    ...lastState,
                    currentPaletteOpenedState: paletteOpened
                };
            case WineryActions.OPEN_SIDEBAR:
                const newSidebarData: any = (<SidebarStateAction>action).sidebarContents;
                return <WineryState>{
                    ...lastState,
                    sidebarContents: newSidebarData
                };
            case WineryActions.CHANGE_MIN_INSTANCES:
                const sideBarNodeId: any = (<SidebarMinInstanceChanges>action).minInstances.id;
                const minInstances: any = (<SidebarMinInstanceChanges>action).minInstances.count;
                const indexChangeMinInstances = lastState.currentJsonTopology.nodeTemplates.map(el => el.id).indexOf(sideBarNodeId);
                const fool = true;
                console.log({
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates.map(nodeTemplate => nodeTemplate.id === sideBarNodeId ?
                            nodeTemplate.generateNewNodeTemplateWithUpdatedAttribute('minInstances', minInstances.toString()) : nodeTemplate
                        )
                    }
                });
                return <WineryState>{
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates.map(nodeTemplate => nodeTemplate.id === sideBarNodeId ?
                            nodeTemplate.generateNewNodeTemplateWithUpdatedAttribute('minInstances', minInstances.toString()) : nodeTemplate
                        )
                    }
                };
            case WineryActions.CHANGE_MAX_INSTANCES:
                const sideBarNodeId2: any = (<SidebarMaxInstanceChanges>action).maxInstances.id;
                const maxInstances: any = (<SidebarMaxInstanceChanges>action).maxInstances.count;
                const indexChangeMaxInstances = lastState.currentJsonTopology.nodeTemplates
                    .map(el => el.id).indexOf(sideBarNodeId2);
                return <WineryState>{
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === sideBarNodeId2 ?
                                nodeTemplate.generateNewNodeTemplateWithUpdatedAttribute('maxInstances', maxInstances.toString()) : nodeTemplate
                            )
                    }
                };
            case WineryActions.INC_MIN_INSTANCES:
                const id_incMinInstances: any = (<IncMinInstances>action).minInstances.id;
                const indexIncMinInstances = lastState.currentJsonTopology.nodeTemplates
                    .map(el => el.id).indexOf(id_incMinInstances);
                return <WineryState>{
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === id_incMinInstances ?
                                nodeTemplate.generateNewNodeTemplateWithUpdatedAttribute('minInstances',
                                    (Number(lastState.currentJsonTopology.nodeTemplates[indexIncMinInstances].minInstances) + 1).toString())
                                : nodeTemplate
                            )
                    }
                };
            case WineryActions.DEC_MIN_INSTANCES:
                const id_decMinInstances: any = (<DecMinInstances>action).minInstances.id;
                const indexDecMinInstances = lastState.currentJsonTopology.nodeTemplates
                    .map(el => el.id).indexOf(id_decMinInstances);
                return <WineryState>{
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === id_decMinInstances ?
                                nodeTemplate.generateNewNodeTemplateWithUpdatedAttribute('minInstances',
                                    (Number(lastState.currentJsonTopology.nodeTemplates[indexDecMinInstances].minInstances) - 1).toString())
                                : nodeTemplate
                            )
                    }
                };
            case WineryActions.INC_MAX_INSTANCES:
                const id_incMaxInstances: any = (<IncMaxInstances>action).maxInstances.id;
                const indexIncMaxInstances = lastState.currentJsonTopology.nodeTemplates
                    .map(el => el.id).indexOf(id_incMaxInstances);
                return <WineryState>{
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === id_incMaxInstances ?
                                nodeTemplate.generateNewNodeTemplateWithUpdatedAttribute('maxInstances',
                                    (Number(lastState.currentJsonTopology.nodeTemplates[indexIncMaxInstances].maxInstances) + 1).toString())
                                : nodeTemplate
                            )
                    }
                };
            case WineryActions.DEC_MAX_INSTANCES:
                const id_decMaxInstances: any = (<DecMaxInstances>action).maxInstances.id;
                const indexDecMaxInstances = lastState.currentJsonTopology.nodeTemplates
                    .map(el => el.id).indexOf(id_decMaxInstances);
                return <WineryState>{
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === id_decMaxInstances ?
                                nodeTemplate.generateNewNodeTemplateWithUpdatedAttribute('maxInstances',
                                    (Number(lastState.currentJsonTopology.nodeTemplates[indexDecMaxInstances].maxInstances) - 1).toString())
                                : nodeTemplate
                            )
                    }
                };
            /*case WineryActions.SET_DEPLOYMENT_ARTIFACTS_PROPERTY:
                const newPropertyDepArt: any = (<SetDepArtifactsPropertyAction>action).nodeDepArtProperty;
                const depArtPropertyType = newPropertyDepArt.propertyType;
                const indexOfNodeDepArtProp = lastState.currentJsonTopology.nodeTemplates
                    .map(node => node.id).indexOf(newPropertyDepArt.nodeId);

                const nodeDepArtPropertyTemplate = lastState.currentJsonTopology.nodeTemplates
                    .find(nodeTemplate => nodeTemplate.id === newPropertyDepArt.nodeId);
                let currentDepArtProperties;
                let newDepArtPropObject;
                let newDepArtProperties: Array<any>;
                let newXMLDepArtProperty: string;
                if (depArtPropertyType === 'KV') {
                    try {
                        currentDepArtProperties = nodeDepArtPropertyTemplate.deploymentArtifacts.properties.kvproperties;
                    } catch (e) {
                    }
                    newDepArtPropObject = {
                        key: newPropertyDepArt.newDepArtProperty.key,
                        value: newPropertyDepArt.newDepArtProperty.value
                    };
                    newDepArtProperties = [];
                    newDepArtProperties.push(newDepArtPropObject);
                    if (currentDepArtProperties) {
                        for (const obj of currentDepArtProperties) {
                            if (!newDepArtProperties.find(node => node.key === obj.key)) {
                                newDepArtProperties.push(obj);
                            }
                        }
                    }
                } else {
                    newXMLDepArtProperty = newPropertyDepArt.newDepArtProperty;
                }
                return <WineryState>{
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === newPropertyDepArt.nodeId ?
                                nodeTemplate.generateNewNodeTemplateWithUpdatedAttribute('deploymentArtifacts',
                                    depArtPropertyType === 'KV' ?
                                        {
                                            ...lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArtProp].deploymentArtifacts,
                                            properties: {kvproperties: newDepArtProperties}
                                        } :
                                        {
                                            ...lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArtProp].deploymentArtifacts,
                                            properties: {any: newXMLDepArtProperty}
                                        }) : nodeTemplate
                            )
                    }
                };*/
            case WineryActions.SET_REQUIREMENT_PROPERTY:
                const newReqProperty: any = (<SetReqPropertyAction>action).nodeReqProperty;
                const newReqProperties = newReqProperty.newReqProperty;
                return <WineryState>{
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === newReqProperty.nodeId ?
                                nodeTemplate.generateNewNodeTemplateWithUpdatedAttribute('requirements',
                                    newReqProperties) : nodeTemplate
                            )
                    }
                };
            case WineryActions.SET_CAPABILITY_PROPERTY:
                const newCapProperty: any = (<SetCapPropertyAction>action).nodeCapProperty;
                const newCapProperties = newCapProperty.newCapProperty;
                return <WineryState>{
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === newCapProperty.nodeId ?
                                nodeTemplate.generateNewNodeTemplateWithUpdatedAttribute('capabilities',
                                    newCapProperties) : nodeTemplate
                            )
                    }
                };
            case WineryActions.SET_POLICIES_PROPERTY:
                const newPoliciesProperty: any = (<SetPoliciesPropertyAction>action).nodePoliciesProperty;
                const indexOfNodePolProp = lastState.currentJsonTopology.nodeTemplates
                    .map(node => node.id).indexOf(newPoliciesProperty.nodeId);
                const policiesPropertyType = newPoliciesProperty.propertyType;
                const nodePolicyPropertyTemplate = lastState.currentJsonTopology.nodeTemplates
                    .find(nodeTemplate => nodeTemplate.id === newPoliciesProperty.nodeId);
                let currentPolicyProperties;
                let newPolicyPropObject;
                let newPolicyProperties: Array<any>;
                let newXMLPolicyProperty: string;
                if (policiesPropertyType === 'KV') {
                    try {
                        currentPolicyProperties = nodePolicyPropertyTemplate.policies.properties.kvproperties;
                    } catch (e) {
                    }
                    newPolicyPropObject = {
                        key: newPoliciesProperty.newPoliciesProperty.key,
                        value: newPoliciesProperty.newPoliciesProperty.value
                    };
                    newPolicyProperties = [];
                    newPolicyProperties.push(newPolicyPropObject);
                    if (currentPolicyProperties) {
                        for (const obj of currentPolicyProperties) {
                            if (!newPolicyProperties.find(node => node.key === obj.key)) {
                                newPolicyProperties.push(obj);
                            }
                        }
                    }
                } else {
                    newXMLPolicyProperty = newPoliciesProperty.newPoliciesProperty;
                }
                return <WineryState>{
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === newPoliciesProperty.nodeId ?
                                nodeTemplate.generateNewNodeTemplateWithUpdatedAttribute('policies',
                                    policiesPropertyType === 'KV' ?
                                        {
                                            ...lastState.currentJsonTopology.nodeTemplates[indexOfNodePolProp].policies,
                                            properties: { kvproperties: newPolicyProperties }
                                        } :
                                        {
                                            ...lastState.currentJsonTopology.nodeTemplates[indexOfNodePolProp].policies,
                                            properties: { any: newXMLPolicyProperty }
                                        }) : nodeTemplate
                            )
                    }
                };
            case WineryActions.SET_TARGET_LOCATIONS_PROPERTY:
                const newTargetLocProperty: any = (<SetTargetLocPropertyAction>action).nodeTargetLocProperty;
                const indexOfNodeTargetLocProp = lastState.currentJsonTopology.nodeTemplates
                    .map(node => node.id).indexOf(newTargetLocProperty.nodeId);
                const targetLocationsPropertyType = newTargetLocProperty.propertyType;
                const nodeTarLocPropertyTemplate = lastState.currentJsonTopology.nodeTemplates
                    .find(nodeTemplate => nodeTemplate.id === newTargetLocProperty.nodeId);
                let currentTarLocProperties;
                let newTarLocPropObject;
                let newTarLocProperties: Array<any>;
                let newXMLTarLocProperty: string;
                if (targetLocationsPropertyType === 'KV') {
                    try {
                        currentTarLocProperties = nodeTarLocPropertyTemplate.targetLocations.properties.kvproperties;
                    } catch (e) {
                    }
                    newTarLocPropObject = {
                        key: newTargetLocProperty.newTargetLocProperty.key,
                        value: newTargetLocProperty.newTargetLocProperty.value
                    };
                    newTarLocProperties = [];
                    newTarLocProperties.push(newTarLocPropObject);
                    if (currentTarLocProperties) {
                        for (const obj of currentTarLocProperties) {
                            if (!newTarLocProperties.find(node => node.key === obj.key)) {
                                newTarLocProperties.push(obj);
                            }
                        }
                    }
                } else {
                    newXMLTarLocProperty = newTargetLocProperty.newTargetLocProperty;
                }
                return <WineryState>{
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === newTargetLocProperty.nodeId ?
                                nodeTemplate.generateNewNodeTemplateWithUpdatedAttribute('targetLocations',
                                    targetLocationsPropertyType === 'KV' ?
                                        {
                                            ...lastState.currentJsonTopology.nodeTemplates[indexOfNodeTargetLocProp].targetLocations,
                                            properties: { kvproperties: newTarLocProperties }
                                        } :
                                        {
                                            ...lastState.currentJsonTopology.nodeTemplates[indexOfNodeTargetLocProp].targetLocations,
                                            properties: { any: newXMLTarLocProperty }
                                        }) : nodeTemplate
                            )
                    }
                };
            case WineryActions.SET_PROPERTY:
                const newProperty: any = (<SetPropertyAction>action).nodeProperty;
                const propertyType = newProperty.propertyType;
                const indexOfNodeProp = lastState.currentJsonTopology.nodeTemplates
                    .map(node => node.id).indexOf(newProperty.nodeId);

                const nodePropertyTemplate = lastState.currentJsonTopology.nodeTemplates
                    .find(nodeTemplate => nodeTemplate.id === newProperty.nodeId);
                let newProperties: Array<any>;
                let newXMLProperty: string;
                if (propertyType === 'KV') {
                    newProperties = newProperty.newProperty;
                } else {
                    newXMLProperty = newProperty.newProperty;
                }
                return <WineryState>{
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === newProperty.nodeId ?
                                nodeTemplate.generateNewNodeTemplateWithUpdatedAttribute('properties',
                                    propertyType === 'KV' ?
                                        { kvproperties: newProperty.newProperty } : { any: newXMLProperty }) : nodeTemplate
                            )
                    }
                };
            case WineryActions.SET_CAPABILITY:
                const newCapability: any = (<SetCababilityAction>action).nodeCapability;
                return <WineryState>{
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === newCapability.nodeId ?
                                nodeTemplate.generateNewNodeTemplateWithUpdatedAttribute('capabilities',
                                    {
                                        capability: newCapability.capability
                                    }) : nodeTemplate
                            )
                    }
                };
            case WineryActions.SET_REQUIREMENT:
                const newRequirement: any = (<SetRequirementAction>action).nodeRequirement;
                return <WineryState>{
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === newRequirement.nodeId ?
                                nodeTemplate.generateNewNodeTemplateWithUpdatedAttribute('requirements',
                                        {
                                            requirement: newRequirement.requirement
                                        }) : nodeTemplate
                            )
                    }
                };
            case WineryActions.SET_DEPLOYMENT_ARTIFACT:
                const newDepArt: any = (<SetDeploymentArtifactsAction>action).nodeDeploymentArtifact;
                const newDeploymentArtifact: TDeploymentArtifact = newDepArt.newDeploymentArtifact;
                const indexOfNodeDepArt = lastState.currentJsonTopology.nodeTemplates
                    .map(node => node.id).indexOf(newDepArt.nodeId);
                const nodeDepArtTemplate = lastState.currentJsonTopology.nodeTemplates
                    .find(nodeTemplate => nodeTemplate.id === newDepArt.nodeId);
                let depArtExist = false;
                if (nodeDepArtTemplate.deploymentArtifacts) {
                    depArtExist = true;
                }
                return <WineryState>{
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === newDepArt.nodeId ?
                                nodeTemplate.generateNewNodeTemplateWithUpdatedAttribute('deploymentArtifacts',
                                    depArtExist ? {
                                        deploymentArtifact: [
                                            ...lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArt].deploymentArtifacts.deploymentArtifact,
                                            newDeploymentArtifact
                                        ]
                                    } : {
                                        deploymentArtifact: [
                                            newDeploymentArtifact
                                        ]
                                    }) : nodeTemplate
                            )
                    }
                };
            case WineryActions.SET_POLICY:
                const newPolicy: any = (<SetPolicyAction>action).nodePolicy;
                const policy = newPolicy.newPolicy;
                const indexOfNodePolicy = lastState.currentJsonTopology.nodeTemplates
                    .map(node => node.id).indexOf(newPolicy.nodeId);
                const nodePolicyTemplate = lastState.currentJsonTopology.nodeTemplates
                    .find(nodeTemplate => nodeTemplate.id === newPolicy.nodeId);
                let policyExist = false;
                if (nodePolicyTemplate.policies) {
                    policyExist = true;
                }
                return <WineryState>{
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === newPolicy.nodeId ?
                                nodeTemplate.generateNewNodeTemplateWithUpdatedAttribute('policies',
                                    policyExist ? {
                                        policy: [
                                            ...lastState.currentJsonTopology.nodeTemplates[indexOfNodePolicy].policies.policy,
                                            policy
                                        ]
                                    } : {
                                        policy: [
                                            policy
                                        ]
                                    }) : nodeTemplate
                            )
                    }
                };
            case WineryActions.CHANGE_NODE_NAME:
                const newNodeName: any = (<SidebarNodeNamechange>action).nodeNames;
                const indexChangeNodeName = lastState.currentJsonTopology.nodeTemplates
                    .map(el => el.id).indexOf(newNodeName.id);
                return <WineryState>{
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === newNodeName.id ?
                                nodeTemplate.generateNewNodeTemplateWithUpdatedAttribute('name', newNodeName.newNodeName)
                                : nodeTemplate
                            )
                    }
                };
            case WineryActions.UPDATE_NODE_COORDINATES:
                const currentNodeCoordinates: any = (<UpdateNodeCoordinatesAction>action).otherAttributes;
                const nodeId = currentNodeCoordinates.id;
                const otherAttributes = {
                    location: currentNodeCoordinates.location,
                    x: currentNodeCoordinates.x,
                    y: currentNodeCoordinates.y
                };
                const indexUpdateNodeCoordinates = lastState.currentJsonTopology.nodeTemplates
                    .map(nodeTemplate => nodeTemplate.id).indexOf(nodeId);
                return <WineryState>{
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === nodeId ?
                                nodeTemplate.generateNewNodeTemplateWithUpdatedAttribute('otherAttributes', otherAttributes)
                                : nodeTemplate
                            )
                    }
                };
            case WineryActions.SAVE_NODE_TEMPLATE :
                const newNode: TNodeTemplate = (<SaveNodeTemplateAction>action).nodeTemplate;
                return <WineryState>{
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: [...lastState.currentJsonTopology.nodeTemplates, newNode]
                    }
                };
            case WineryActions.SAVE_RELATIONSHIP :
                const newRelationship: TRelationshipTemplate = (<SaveRelationshipAction>action).relationshipTemplate;
                return <WineryState>{
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        relationshipTemplates: [...lastState.currentJsonTopology.relationshipTemplates, newRelationship]
                    }
                };
            case WineryActions.DELETE_NODE_TEMPLATE:
                const deletedNodeId: string = (<DeleteNodeAction>action).nodeTemplateId;
                return <WineryState>{
                    ...lastState,
                    currentJsonTopology: {
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .filter(nodeTemplate => nodeTemplate.id !== deletedNodeId),
                        relationshipTemplates: lastState.currentJsonTopology.relationshipTemplates.filter(
                            relationshipTemplate => relationshipTemplate.sourceElement.ref !== deletedNodeId &&
                                relationshipTemplate.targetElement.ref !== deletedNodeId)
                    }
                };
            case WineryActions.UPDATE_REL_DATA:
                const relData: any = (<UpdateRelationshipNameAction>action).relData;
                const indexRel = lastState.currentJsonTopology.relationshipTemplates
                    .map(rel => rel.id).indexOf(relData.id);
                return <WineryState>{
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        relationshipTemplates: lastState.currentJsonTopology.relationshipTemplates
                            .map(relTemplate => relTemplate.id === relData.id ?
                                relTemplate.generateNewRelTemplateWithUpdatedAttribute('name', relData.newRelName)
                                : relTemplate
                            )
                    }
                };
            case WineryActions.SEND_CURRENT_NODE_ID :
                const currentNodeData: string = (<SendCurrentNodeIdAction>action).currentNodeData;
                console.log({ ...lastState, currentNodeId: currentNodeData });
                return <WineryState>{
                    ...lastState,
                    currentNodeData: currentNodeData
                };
            default:
                return <WineryState> lastState;
        }
    };
