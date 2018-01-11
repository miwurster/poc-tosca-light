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

import {Action} from 'redux';
import {
    DecMaxInstances,
    DecMinInstances,
    DeleteNodeAction,
    IncMaxInstances,
    IncMinInstances,
    SaveNodeTemplateAction,
    SaveRelationshipAction, SendCurrentNodeIdAction,
    SendPaletteOpenedAction,
    SetCababilityAction,
    SetCapPropertyAction,
    SetDepArtifactsPropertyAction, SetDeploymentArtifactsAction,
    SetPoliciesPropertyAction, SetPolicyAction,
    SetPropertyAction,
    SetReqPropertyAction, SetRequirementAction,
    SetTargetLocPropertyAction,
    SidebarMaxInstanceChanges,
    SidebarMinInstanceChanges,
    SidebarNodeNamechange,
    SidebarStateAction,
    UpdateNodeCoordinatesAction,
    UpdateRelationshipNameAction,
    WineryActions
} from '../actions/winery.actions';
import {TNodeTemplate, TRelationshipTemplate, TTopologyTemplate} from 'app/models/ttopology-template';

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
                return {
                    ...lastState,
                    sidebarContents: newSidebarData
                };
            case WineryActions.CHANGE_MIN_INSTANCES:
                const sideBarNodeId: any = (<SidebarMinInstanceChanges>action).minInstances.id;
                const minInstances: any = (<SidebarMinInstanceChanges>action).minInstances.count;
                const indexChangeMinInstances = lastState.currentJsonTopology.nodeTemplates.map(el => el.id).indexOf(sideBarNodeId);
                const fool = true;
                console.log( {...lastState,
                    currentJsonTopology: {
            ...lastState.currentJsonTopology,
                    nodeTemplates: lastState.currentJsonTopology.nodeTemplates.map(nodeTemplate => nodeTemplate.id === sideBarNodeId ?
                    new TNodeTemplate(
                        lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].properties,
                        // id
                        lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].id,
                        // type
                        lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].type,
                        // name
                        lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].name,
                        minInstances,
                        lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].maxInstances,
                        lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].color,
                        lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].imageUrl,
                        lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].any,
                        lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].documentation,
                        lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].otherAttributes,
                        lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].x,
                        lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].y,
                        lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].capabilities,
                        lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].requirements,
                        lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].deploymentArtifacts,
                        lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].policies,
                        lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].targetLocations,
                    ) : nodeTemplate
                )
            }});
                return {
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates.map(nodeTemplate => nodeTemplate.id === sideBarNodeId ?
                            new TNodeTemplate(
                                lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].properties,
                                // id
                                lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].id,
                                // type
                                lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].type,
                                // name
                                lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].name,
                                minInstances,
                                lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].maxInstances,
                                lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].color,
                                lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].imageUrl,
                                lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].any,
                                lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].documentation,
                                lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].otherAttributes,
                                lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].x,
                                lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].y,
                                lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].capabilities,
                                lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].requirements,
                                lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].deploymentArtifacts,
                                lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].policies,
                                lastState.currentJsonTopology.nodeTemplates[indexChangeMinInstances].targetLocations,
                            ) : nodeTemplate
                        )
                    }
                };
            case WineryActions.CHANGE_MAX_INSTANCES:
                const sideBarNodeId2: any = (<SidebarMaxInstanceChanges>action).maxInstances.id;
                const maxInstances: any = (<SidebarMaxInstanceChanges>action).maxInstances.count;
                const indexChangeMaxInstances = lastState.currentJsonTopology.nodeTemplates
                    .map(el => el.id).indexOf(sideBarNodeId2);
                return {
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === sideBarNodeId2 ?
                                new TNodeTemplate(
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeMaxInstances].properties,
                                    // id
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeMaxInstances].id,
                                    // type
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeMaxInstances].type,
                                    // name
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeMaxInstances].name,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeMaxInstances].minInstances,
                                    maxInstances,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeMaxInstances].color,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeMaxInstances].imageUrl,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeMaxInstances].any,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeMaxInstances].documentation,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeMaxInstances].otherAttributes,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeMaxInstances].x,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeMaxInstances].y,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeMaxInstances].capabilities,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeMaxInstances].requirements,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeMaxInstances].deploymentArtifacts,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeMaxInstances].policies,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeMaxInstances].targetLocations,
                                ) : nodeTemplate
                            )
                    }
                };
            case WineryActions.INC_MIN_INSTANCES:
                const id_incMinInstances: any = (<IncMinInstances>action).minInstances.id;
                const indexIncMinInstances = lastState.currentJsonTopology.nodeTemplates
                    .map(el => el.id).indexOf(id_incMinInstances);
                return {
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === id_incMinInstances ?
                                new TNodeTemplate(
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMinInstances].properties,
                                    // id
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMinInstances].id,
                                    // type
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMinInstances].type,
                                    // name
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMinInstances].name,
                                    Number(lastState.currentJsonTopology
                                        .nodeTemplates[indexIncMinInstances].minInstances) + 1,
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMinInstances].maxInstances,
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMinInstances].color,
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMinInstances].imageUrl,
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMinInstances].any,
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMinInstances].documentation,
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMinInstances].otherAttributes,
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMinInstances].x,
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMinInstances].y,
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMinInstances].capabilities,
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMinInstances].requirements,
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMinInstances].deploymentArtifacts,
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMinInstances].policies,
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMinInstances].targetLocations,
                                ) : nodeTemplate
                            )
                    }
                };
            case WineryActions.DEC_MIN_INSTANCES:
                const id_decMinInstances: any = (<DecMinInstances>action).minInstances.id;
                const indexDecMinInstances = lastState.currentJsonTopology.nodeTemplates
                    .map(el => el.id).indexOf(id_decMinInstances);
                return {
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === id_decMinInstances ?
                                new TNodeTemplate(
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMinInstances].properties,
                                    // id
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMinInstances].id,
                                    // type
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMinInstances].type,
                                    // name
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMinInstances].name,
                                    Number(lastState.currentJsonTopology
                                        .nodeTemplates[indexDecMinInstances].minInstances) - 1,
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMinInstances].maxInstances,
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMinInstances].color,
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMinInstances].imageUrl,
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMinInstances].any,
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMinInstances].documentation,
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMinInstances].otherAttributes,
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMinInstances].x,
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMinInstances].y,
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMinInstances].capabilities,
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMinInstances].requirements,
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMinInstances].deploymentArtifacts,
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMinInstances].policies,
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMinInstances].targetLocations,
                                ) : nodeTemplate
                            )
                    }
                };
            case WineryActions.INC_MAX_INSTANCES:
                const id_incMaxInstances: any = (<IncMaxInstances>action).maxInstances.id;
                const indexIncMaxInstances = lastState.currentJsonTopology.nodeTemplates
                    .map(el => el.id).indexOf(id_incMaxInstances);
                return {
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === id_incMaxInstances ?
                                new TNodeTemplate(
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMaxInstances].properties,
                                    // id
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMaxInstances].id,
                                    // type
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMaxInstances].type,
                                    // name
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMaxInstances].name,
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMaxInstances].minInstances,
                                    Number(lastState.currentJsonTopology
                                        .nodeTemplates[indexIncMaxInstances].maxInstances) + 1,
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMaxInstances].color,
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMaxInstances].imageUrl,
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMaxInstances].any,
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMaxInstances].documentation,
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMaxInstances].otherAttributes,
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMaxInstances].x,
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMaxInstances].y,
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMaxInstances].capabilities,
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMaxInstances].requirements,
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMaxInstances].deploymentArtifacts,
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMaxInstances].policies,
                                    lastState.currentJsonTopology.nodeTemplates[indexIncMaxInstances].targetLocations,
                                ) : nodeTemplate
                            )
                    }
                };
            case WineryActions.DEC_MAX_INSTANCES:
                const id_decMaxInstances: any = (<DecMaxInstances>action).maxInstances.id;
                const indexDecMaxInstances = lastState.currentJsonTopology.nodeTemplates
                    .map(el => el.id).indexOf(id_decMaxInstances);
                return {
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === id_decMaxInstances ?
                                new TNodeTemplate(
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMaxInstances].properties,
                                    // id
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMaxInstances].id,
                                    // type
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMaxInstances].type,
                                    // name
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMaxInstances].name,
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMaxInstances].minInstances,
                                    Number(lastState.currentJsonTopology
                                        .nodeTemplates[indexDecMaxInstances].maxInstances) - 1,
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMaxInstances].color,
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMaxInstances].imageUrl,
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMaxInstances].any,
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMaxInstances].documentation,
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMaxInstances].otherAttributes,
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMaxInstances].x,
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMaxInstances].y,
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMaxInstances].capabilities,
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMaxInstances].requirements,
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMaxInstances].deploymentArtifacts,
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMaxInstances].policies,
                                    lastState.currentJsonTopology.nodeTemplates[indexDecMaxInstances].targetLocations,
                                ) : nodeTemplate
                            )
                    }
                };
            case WineryActions.SET_DEPLOYMENT_ARTIFACTS_PROPERTY:
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
                return {
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === newPropertyDepArt.nodeId ?
                                new TNodeTemplate(
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArtProp].properties,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArtProp].id,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArtProp].type,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArtProp].name,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArtProp].minInstances,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArtProp].maxInstances,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArtProp].color,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArtProp].imageUrl,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArtProp].any,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArtProp].documentation,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArtProp].otherAttributes,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArtProp].x,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArtProp].y,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArtProp].capabilities,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArtProp].requirements,
                                    depArtPropertyType === 'KV' ?
                                        {
                                            ...lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArtProp].deploymentArtifacts,
                                            properties: {kvproperties: newDepArtProperties}
                                        } :
                                        {
                                            ...lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArtProp].deploymentArtifacts,
                                            properties: {any: newXMLDepArtProperty}
                                        },
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArtProp].policies,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArtProp].targetLocations,
                                ) : nodeTemplate
                            )
                    }
                };
            case WineryActions.SET_REQUIREMENT_PROPERTY:
                const newReqProperty: any = (<SetReqPropertyAction>action).nodeReqProperty;
                const reqPropertyType = newReqProperty.propertyType;
                const indexOfNodeReqProp = lastState.currentJsonTopology.nodeTemplates
                    .map(node => node.id).indexOf(newReqProperty.nodeId);
                const nodeReqPropertyTemplate = lastState.currentJsonTopology.nodeTemplates
                    .find(nodeTemplate => nodeTemplate.id === newReqProperty.nodeId);
                let currentReqProperties;
                let newReqPropObject;
                let newReqProperties: Array<any>;
                let newXMLReqProperty: string;
                if (reqPropertyType === 'KV') {
                    try {
                        currentReqProperties = nodeReqPropertyTemplate.requirements.properties.kvproperties;
                    } catch (e) {
                    }
                    newReqPropObject = {
                        key: newReqProperty.newReqProperty.key,
                        value: newReqProperty.newReqProperty.value
                    };
                    newReqProperties = [];
                    newReqProperties.push(newReqPropObject);
                    if (currentReqProperties) {
                        for (const obj of currentReqProperties) {
                            if (!newReqProperties.find(node => node.key === obj.key)) {
                                newReqProperties.push(obj);
                            }
                        }
                    }
                } else {
                    newXMLReqProperty = newReqProperty.newReqProperty;
                }
                return {
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === newReqProperty.nodeId ?
                                new TNodeTemplate(
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReqProp].properties,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReqProp].id,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReqProp].type,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReqProp].name,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReqProp].minInstances,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReqProp].maxInstances,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReqProp].color,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReqProp].imageUrl,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReqProp].any,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReqProp].documentation,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReqProp].otherAttributes,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReqProp].x,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReqProp].y,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReqProp].capabilities,
                                    reqPropertyType === 'KV' ?
                                        {
                                            ...lastState.currentJsonTopology.nodeTemplates[indexOfNodeReqProp].requirements,
                                            properties: {kvproperties: newReqProperties}
                                        } :
                                        {
                                            ...lastState.currentJsonTopology.nodeTemplates[indexOfNodeReqProp].requirements,
                                            properties: {any: newXMLReqProperty}
                                        },
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReqProp].deploymentArtifacts,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReqProp].properties,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReqProp].targetLocations,
                                ) : nodeTemplate
                            )
                    }
                };
            case WineryActions.SET_CAPABILITY_PROPERTY:
                const newCapProperty: any = (<SetCapPropertyAction>action).nodeCapProperty;
                const capPropertyType = newCapProperty.propertyType;
                const indexOfNodeCapProp = lastState.currentJsonTopology.nodeTemplates
                    .map(node => node.id).indexOf(newReqProperty.nodeId);
                const nodeCapPropertyTemplate = lastState.currentJsonTopology.nodeTemplates
                    .find(nodeTemplate => nodeTemplate.id === newCapProperty.nodeId);
                let currentCapProperties;
                let newCapPropObject;
                let newCapProperties: Array<any>;
                let newXMLCapProperty: string;
                if (capPropertyType === 'KV') {
                    try {
                        currentCapProperties = nodeCapPropertyTemplate.capabilities.properties.kvproperties;
                    } catch (e) {
                    }
                    newCapPropObject = {
                        key: newCapProperty.newCapProperty.key,
                        value: newCapProperty.newCapProperty.value
                    };
                    newCapProperties = [];
                    newCapProperties.push(newCapPropObject);
                    if (currentCapProperties) {
                        for (const obj of currentCapProperties) {
                            if (!newCapProperties.find(node => node.key === obj.key)) {
                                newCapProperties.push(obj);
                            }
                        }
                    }
                } else {
                    newXMLCapProperty = newCapProperty.newCapProperty;
                }
                return {
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === newCapProperty.nodeId ?
                                new TNodeTemplate(
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCapProp].properties,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCapProp].id,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCapProp].type,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCapProp].name,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCapProp].minInstances,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCapProp].maxInstances,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCapProp].color,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCapProp].imageUrl,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCapProp].any,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCapProp].documentation,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCapProp].otherAttributes,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCapProp].x,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCapProp].y,
                                    capPropertyType === 'KV' ?
                                        {
                                            ...lastState.currentJsonTopology.nodeTemplates[indexOfNodeCapProp].capabilities,
                                            properties: {kvproperties: newCapProperties}
                                        } :
                                        {
                                            ...lastState.currentJsonTopology.nodeTemplates[indexOfNodeCapProp].capabilities,
                                            properties: {any: newXMLCapProperty}
                                        },
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCapProp].requirements,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCapProp].deploymentArtifacts,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCapProp].policies,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCapProp].targetLocations,
                                ) : nodeTemplate
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
                return {
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === newPoliciesProperty.nodeId ?
                                new TNodeTemplate(
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolProp].properties,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolProp].id,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolProp].type,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolProp].name,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolProp].minInstances,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolProp].maxInstances,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolProp].color,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolProp].imageUrl,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolProp].any,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolProp].documentation,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolProp].otherAttributes,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolProp].x,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolProp].y,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolProp].capabilities,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolProp].requirements,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolProp].deploymentArtifacts,
                                    policiesPropertyType === 'KV' ?
                                        {
                                            ...lastState.currentJsonTopology.nodeTemplates[indexOfNodePolProp].policies,
                                            properties: {kvproperties: newPolicyProperties}
                                        } :
                                        {
                                            ...lastState.currentJsonTopology.nodeTemplates[indexOfNodePolProp].policies,
                                            properties: {any: newXMLPolicyProperty}
                                        },
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolProp].targetLocations,
                                ) : nodeTemplate
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
                return {
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === newTargetLocProperty.nodeId ?
                                new TNodeTemplate(
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeTargetLocProp].properties,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeTargetLocProp].id,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeTargetLocProp].type,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeTargetLocProp].name,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeTargetLocProp].minInstances,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeTargetLocProp].maxInstances,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeTargetLocProp].color,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeTargetLocProp].imageUrl,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeTargetLocProp].any,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeTargetLocProp].documentation,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeTargetLocProp].otherAttributes,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeTargetLocProp].x,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeTargetLocProp].y,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeTargetLocProp].capabilities,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeTargetLocProp].requirements,
                                    lastState.currentJsonTopology
                                        .nodeTemplates[indexOfNodeTargetLocProp].deploymentArtifacts,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeTargetLocProp].policies,
                                    targetLocationsPropertyType === 'KV' ?
                                        {
                                            ...lastState.currentJsonTopology.nodeTemplates[indexOfNodeTargetLocProp].targetLocations,
                                            properties: {kvproperties: newTarLocProperties}
                                        } :
                                        {
                                            ...lastState.currentJsonTopology.nodeTemplates[indexOfNodeTargetLocProp].targetLocations,
                                            properties: {any: newXMLTarLocProperty}
                                        },
                                ) : nodeTemplate
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
                return {
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === newProperty.nodeId ?
                                new TNodeTemplate(
                                    // new Property
                                    propertyType === 'KV' ?
                                        {kvproperties: newProperty.newProperty} : {any: newXMLProperty},
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeProp].id,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeProp].type,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeProp].name,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeProp].minInstances,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeProp].maxInstances,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeProp].color,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeProp].imageUrl,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeProp].any,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeProp].documentation,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeProp].otherAttributes,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeProp].x,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeProp].y,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeProp].capabilities,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeProp].requirements,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeProp].deploymentArtifacts,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeProp].policies,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeProp].targetLocations,
                                ) : nodeTemplate
                            )
                    }
                };
            case WineryActions.SET_CAPABILITY:
                const newCapability: any = (<SetCababilityAction>action).nodeCapability;
                const capability = {
                    color: newCapability.color,
                    id: newCapability.id,
                    name: newCapability.name,
                    namespace: newCapability.namespace,
                    qName: newCapability.qName
                };
                const indexOfNodeCap = lastState.currentJsonTopology.nodeTemplates
                    .map(node => node.id).indexOf(newCapability.nodeId);
                const nodeCapTemplate = lastState.currentJsonTopology.nodeTemplates
                    .find(nodeTemplate => nodeTemplate.id === newCapability.nodeId);
                let capabilitiesExist = false;
                if (nodeCapTemplate.capabilities) {
                    capabilitiesExist = true;
                }
                return {
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === newCapability.nodeId ?
                                new TNodeTemplate(
                                    // new Property
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCap].properties,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCap].id,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCap].type,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCap].name,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCap].minInstances,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCap].maxInstances,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCap].color,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCap].imageUrl,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCap].any,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCap].documentation,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCap].otherAttributes,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCap].x,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCap].y,
                                    capabilitiesExist ?
                                        {
                                            ...lastState.currentJsonTopology.nodeTemplates[indexOfNodeCap].capabilities,
                                            ...capability
                                        } : capability,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCap].requirements,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCap].deploymentArtifacts,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCap].policies,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeCap].targetLocations,
                                ) : nodeTemplate
                            )
                    }
                };
            case WineryActions.SET_REQUIREMENT:
                const newRequirement: any = (<SetRequirementAction>action).nodeRequirement;
                const requirement = {
                    color: newRequirement.color,
                    id: newRequirement.id,
                    name: newRequirement.name,
                    namespace: newRequirement.namespace,
                    qName: newRequirement.qName
                };
                const indexOfNodeReq = lastState.currentJsonTopology.nodeTemplates
                    .map(node => node.id).indexOf(newRequirement.nodeId);
                const nodeReqTemplate = lastState.currentJsonTopology.nodeTemplates
                    .find(nodeTemplate => nodeTemplate.id === newRequirement.nodeId);
                let requirementsExist = false;
                if (nodeReqTemplate.requirements) {
                    requirementsExist = true;
                }
                return {
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === newRequirement.nodeId ?
                                new TNodeTemplate(
                                    // new Property
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReq].properties,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReq].id,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReq].type,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReq].name,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReq].minInstances,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReq].maxInstances,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReq].color,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReq].imageUrl,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReq].any,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReq].documentation,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReq].otherAttributes,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReq].x,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReq].y,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReq].capabilities,
                                    requirementsExist ?
                                        {
                                            ...lastState.currentJsonTopology.nodeTemplates[indexOfNodeReq].requirements,
                                            ...requirement
                                        } : requirement,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReq].deploymentArtifacts,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReq].policies,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeReq].targetLocations,
                                ) : nodeTemplate
                            )
                    }
                };
            case WineryActions.SET_DEPLOYMENT_ARTIFACT:
                const newDepArt: any = (<SetDeploymentArtifactsAction>action).nodeDeploymentArtifact;
                const deploymentArtifact = {
                    color: newDepArt.color,
                    id: newDepArt.id,
                    name: newDepArt.name,
                    namespace: newDepArt.namespace,
                    qName: newDepArt.qName
                };
                const indexOfNodeDepArt = lastState.currentJsonTopology.nodeTemplates
                    .map(node => node.id).indexOf(newDepArt.nodeId);
                const nodeDepArtTemplate = lastState.currentJsonTopology.nodeTemplates
                    .find(nodeTemplate => nodeTemplate.id === newDepArt.nodeId);
                let depArtExist = false;
                if (nodeDepArtTemplate.deploymentArtifacts) {
                    depArtExist = true;
                }
                return {
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === newDepArt.nodeId ?
                                new TNodeTemplate(
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArt].properties,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArt].id,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArt].type,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArt].name,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArt].minInstances,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArt].maxInstances,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArt].color,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArt].imageUrl,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArt].any,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArt].documentation,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArt].otherAttributes,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArt].x,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArt].y,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArt].capabilities,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArt].requirements,
                                    depArtExist ?
                                        {
                                            ...lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArt].deploymentArtifacts,
                                            ...deploymentArtifact
                                        } : deploymentArtifact,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArt].policies,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodeDepArt].targetLocations,
                                ) : nodeTemplate
                            )
                    }
                };
            case WineryActions.SET_POLICY:
                const newPolicy: any = (<SetPolicyAction>action).nodePolicy;
                const policy = {
                    color: newPolicy.color,
                    id: newPolicy.id,
                    name: newPolicy.name,
                    namespace: newPolicy.namespace,
                    qName: newPolicy.qName,
                    templateColor: newPolicy.templateColor,
                    templateId: newPolicy.templateId,
                    templateName: newPolicy.templateName,
                    templateNamespace: newPolicy.templateNamespace,
                    templateQName: newPolicy.templateQName,
                    typeColor: newPolicy.typeColor,
                    typeId: newPolicy.typeId,
                    typeName: newPolicy.typeName,
                    typeNamespace: newPolicy.typeNamespace,
                    typeQName: newPolicy.typeQName,
                };
                const indexOfNodePolicy = lastState.currentJsonTopology.nodeTemplates
                    .map(node => node.id).indexOf(newPolicy.nodeId);
                const nodePolicyTemplate = lastState.currentJsonTopology.nodeTemplates
                    .find(nodeTemplate => nodeTemplate.id === newPolicy.nodeId);
                let policyExist = false;
                if (nodePolicyTemplate.policies) {
                    policyExist = true;
                }
                return {
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === newPolicy.nodeId ?
                                new TNodeTemplate(
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolicy].properties,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolicy].id,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolicy].type,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolicy].name,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolicy].minInstances,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolicy].maxInstances,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolicy].color,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolicy].imageUrl,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolicy].any,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolicy].documentation,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolicy].otherAttributes,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolicy].x,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolicy].y,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolicy].capabilities,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolicy].requirements,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolicy].deploymentArtifacts,
                                    policyExist ? {
                                        ...lastState.currentJsonTopology.nodeTemplates[indexOfNodePolicy].policies,
                                        ...policy
                                    } : policy,
                                    lastState.currentJsonTopology.nodeTemplates[indexOfNodePolicy].targetLocations,
                                ) : nodeTemplate
                            )
                    }
                };
            case WineryActions.CHANGE_NODE_NAME:
                const newNodeName: any = (<SidebarNodeNamechange>action).nodeNames;
                const indexChangeNodeName = lastState.currentJsonTopology.nodeTemplates
                    .map(el => el.id).indexOf(newNodeName.id);
                return {
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === newNodeName.id ?
                                new TNodeTemplate(
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeNodeName].properties,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeNodeName].id,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeNodeName].type,
                                    newNodeName.newNodeName,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeNodeName].minInstances,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeNodeName].maxInstances,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeNodeName].color,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeNodeName].imageUrl,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeNodeName].any,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeNodeName].documentation,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeNodeName].otherAttributes,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeNodeName].x,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeNodeName].y,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeNodeName].capabilities,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeNodeName].requirements,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeNodeName].deploymentArtifacts,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeNodeName].policies,
                                    lastState.currentJsonTopology.nodeTemplates[indexChangeNodeName].targetLocations,
                                ) : nodeTemplate
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
                return {
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: lastState.currentJsonTopology.nodeTemplates
                            .map(nodeTemplate => nodeTemplate.id === nodeId ?
                                new TNodeTemplate(
                                    lastState.currentJsonTopology.nodeTemplates[indexUpdateNodeCoordinates].properties,
                                    lastState.currentJsonTopology.nodeTemplates[indexUpdateNodeCoordinates].id,
                                    lastState.currentJsonTopology.nodeTemplates[indexUpdateNodeCoordinates].type,
                                    lastState.currentJsonTopology.nodeTemplates[indexUpdateNodeCoordinates].name,
                                    lastState.currentJsonTopology.nodeTemplates[indexUpdateNodeCoordinates].minInstances,
                                    lastState.currentJsonTopology.nodeTemplates[indexUpdateNodeCoordinates].maxInstances,
                                    lastState.currentJsonTopology.nodeTemplates[indexUpdateNodeCoordinates].color,
                                    lastState.currentJsonTopology.nodeTemplates[indexUpdateNodeCoordinates].imageUrl,
                                    lastState.currentJsonTopology.nodeTemplates[indexUpdateNodeCoordinates].any,
                                    lastState.currentJsonTopology.nodeTemplates[indexUpdateNodeCoordinates].documentation,
                                    otherAttributes,
                                    currentNodeCoordinates.x,
                                    currentNodeCoordinates.y,
                                    lastState.currentJsonTopology.nodeTemplates[indexUpdateNodeCoordinates].capabilities,
                                    lastState.currentJsonTopology.nodeTemplates[indexUpdateNodeCoordinates].requirements,
                                    lastState.currentJsonTopology
                                        .nodeTemplates[indexUpdateNodeCoordinates].deploymentArtifacts,
                                    lastState.currentJsonTopology.nodeTemplates[indexUpdateNodeCoordinates].policies,
                                    lastState.currentJsonTopology.nodeTemplates[indexUpdateNodeCoordinates].targetLocations,
                                ) : nodeTemplate
                            )
                    }
                };
            case WineryActions.SAVE_NODE_TEMPLATE :
                const newNode: TNodeTemplate = (<SaveNodeTemplateAction>action).nodeTemplate;
                return {
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        nodeTemplates: [...lastState.currentJsonTopology.nodeTemplates, newNode]
                    }
                };
            case WineryActions.SAVE_RELATIONSHIP :
                const newRelationship: TRelationshipTemplate = (<SaveRelationshipAction>action).relationshipTemplate;
                return {
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        relationshipTemplates: [...lastState.currentJsonTopology.relationshipTemplates, newRelationship]
                    }
                };
            case WineryActions.DELETE_NODE_TEMPLATE:
                const deletedNodeId: string = (<DeleteNodeAction>action).nodeTemplateId;
                return {
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
                return {
                    ...lastState,
                    currentJsonTopology: {
                        ...lastState.currentJsonTopology,
                        relationshipTemplates: lastState.currentJsonTopology.relationshipTemplates
                            .map(relTemplate => relTemplate.id === relData.id ?
                                new TRelationshipTemplate(
                                    lastState.currentJsonTopology.relationshipTemplates[indexRel].sourceElement,
                                    lastState.currentJsonTopology.relationshipTemplates[indexRel].targetElement,
                                    relData.newRelName,
                                    lastState.currentJsonTopology.relationshipTemplates[indexRel].id,
                                    lastState.currentJsonTopology.relationshipTemplates[indexRel].type,
                                ) : relTemplate
                            )
                    }
                };
            case WineryActions.SEND_CURRENT_NODE_ID :
                const currentNodeData: string = (<SendCurrentNodeIdAction>action).currentNodeData;
                console.log({...lastState, currentNodeId: currentNodeData});
                return {
                    ...lastState,
                    currentNodeData: currentNodeData
                };
            default:
                return lastState;
        }
    };
