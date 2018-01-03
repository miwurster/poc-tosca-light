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

import {Action, ActionCreator} from 'redux';
import {Injectable} from '@angular/core';
import {TNodeTemplate, TRelationshipTemplate} from '../../models/ttopology-template';

export interface SendPaletteOpenedAction extends Action {
    paletteOpened: boolean;
}

export interface SidebarStateAction extends Action {
    sidebarContents: {
        sidebarVisible: boolean,
        nodeClicked: boolean,
        id: string,
        nameTextFieldValue: string,
        type: string,
        minInstances: string,
        maxInstances: string
    };
}

export interface SidebarNodeNamechange extends Action {
    nodeNames: {
        newNodeName: string,
        id: string
    };
}

export interface SidebarMinInstanceChanges extends Action {
    minInstances: {
        id: string,
        count: number
    };
}

export interface SidebarMaxInstanceChanges extends Action {
    maxInstances: {
        id: string,
        count: number
    };
}

export interface IncMaxInstances extends Action {
    maxInstances: {
        id: string
    };
}

export interface DecMaxInstances extends Action {
    maxInstances: {
        id: string
    };
}

export interface IncMinInstances extends Action {
    minInstances: {
        id: string
    };
}

export interface DecMinInstances extends Action {
    minInstances: {
        id: string
    };
}

export interface SaveNodeTemplateAction extends Action {
    nodeTemplate: TNodeTemplate;
}

export interface UpdateNodeCoordinatesAction extends Action {
    otherAttributes: any;
}

export interface SaveRelationshipAction extends Action {
    relationshipTemplate: TRelationshipTemplate;
}

export interface DeleteNodeAction extends Action {
    nodeTemplateId: string;
}

export interface UpdateRelationshipNameAction extends Action {
    relData: {
        newRelName: string,
        id: string
    };
}

export interface SetPropertyAction extends Action {
    nodeProperty: {
        newProperty: any,
        propertyType: string,
        nodeId: string,
    };
}

export interface SetDepArtifactsPropertyAction extends Action {
    nodeDepArtProperty: {
        newDepArtProperty: any,
        propertyType: string,
        nodeId: string,
    };
}

export interface SetReqPropertyAction extends Action {
    nodeReqProperty: {
        newReqProperty: any,
        propertyType: string,
        nodeId: string,
    };
}

export interface SetCapPropertyAction extends Action {
    nodeCapProperty: {
        newCapProperty: any,
        propertyType: string,
        nodeId: string,
    };
}

export interface SetPoliciesPropertyAction extends Action {
    nodePoliciesProperty: {
        newPoliciesProperty: any,
        propertyType: string,
        nodeId: string,
    };
}

export interface SetTargetLocPropertyAction extends Action {
    nodeTargetLocProperty: {
        newTargetLocProperty: any,
        propertyType: string,
        nodeId: string,
    };
}

export interface SetCababilityAction extends Action {
    nodeCapability: {
        nodeId: string,
        color: string,
        id: string,
        name: string,
        namespace: string,
        qName: string
    };
}

export interface SetRequirementAction extends Action {
    nodeRequirement: {
        nodeId: string,
        color: string,
        id: string,
        name: string,
        namespace: string,
        qName: string
    };
}

export interface SetDeploymentArtifactsAction extends Action {
    nodeDeploymentArtifact: {
        nodeId: string,
        color: string,
        id: string,
        name: string,
        namespace: string,
        qName: string
    };
}

export interface SetPolicyAction extends Action {
    nodePolicy: {
        nodeId: string,
        templateColor: string,
        templateId: string,
        templateName: string,
        templateNamespace: string,
        templateQName: string,
        typeColor: string,
        typeId: string,
        typeName: string,
        typeNamespace: string,
        typeQName: string,
    };
}

export interface SendCurrentNodeIdAction extends Action {
    currentNodeData: any;
}

/**
 * Winery Actions
 */
@Injectable()
export class WineryActions {

    static SEND_PALETTE_OPENED = 'SEND_PALETTE_OPENED';
    static SAVE_NODE_TEMPLATE = 'SAVE_NODE_TEMPLATE';
    static SAVE_RELATIONSHIP = 'SAVE_RELATIONSHIP';
    static DELETE_NODE_TEMPLATE = 'DELETE_NODE_TEMPLATE';
    static CHANGE_NODE_NAME = 'CHANGE_NODE_NAME';
    static OPEN_SIDEBAR = 'OPEN_SIDEBAR';
    static UPDATE_NODE_COORDINATES = 'UPDATE_NODE_COORDINATES';
    static UPDATE_REL_DATA = 'UPDATE_REL_DATA';
    static CHANGE_MIN_INSTANCES = 'CHANGE_MIN_INSTANCES';
    static CHANGE_MAX_INSTANCES = 'CHANGE_MAX_INSTANCES';
    static INC_MIN_INSTANCES = 'INC_MIN_INSTANCES';
    static DEC_MIN_INSTANCES = 'DEC_MIN_INSTANCES';
    static INC_MAX_INSTANCES = 'INC_MAX_INSTANCES';
    static DEC_MAX_INSTANCES = 'DEC_MAX_INSTANCES';
    static SET_PROPERTY = 'SET_PROPERTY';
    static SET_CAPABILITY = 'SET_CAPABILITY';
    static SET_REQUIREMENT = 'SET_REQUIREMENT';
    static SET_DEPLOYMENT_ARTIFACTS_PROPERTY = 'SET_DEPLOYMENT_ARTIFACTS_PROPERTY';
    static SET_REQUIREMENT_PROPERTY = 'SET_REQUIREMENT_PROPERTY';
    static SET_CAPABILITY_PROPERTY = 'SET_CAPABILITY_PROPERTY';
    static SET_POLICIES_PROPERTY = 'SET_POLICIES_PROPERTY';
    static SET_TARGET_LOCATIONS_PROPERTY = 'SET_TARGET_LOCATIONS_PROPERTY';
    static SET_DEPLOYMENT_ARTIFACT = 'SET_DEPLOYMENT_ARTIFACT';
    static SET_POLICY = 'SET_POLICY';
    static SEND_CURRENT_NODE_ID = 'SEND_CURRENT_NODE_ID';

    sendPaletteOpened: ActionCreator<SendPaletteOpenedAction> =
        ((paletteOpened) => ({
            type: WineryActions.SEND_PALETTE_OPENED,
            paletteOpened: paletteOpened
        }));
    openSidebar: ActionCreator<SidebarStateAction> =
        ((newSidebarData) => ({
            type: WineryActions.OPEN_SIDEBAR,
            sidebarContents: newSidebarData.sidebarContents
        }));
    changeNodeName: ActionCreator<SidebarNodeNamechange> =
        ((nodeNames) => ({
            type: WineryActions.CHANGE_NODE_NAME,
            nodeNames: nodeNames.nodeNames
        }));
    changeMinInstances: ActionCreator<SidebarMinInstanceChanges> =
        ((minInstances) => ({
            type: WineryActions.CHANGE_MIN_INSTANCES,
            minInstances: minInstances.minInstances
        }));
    changeMaxInstances: ActionCreator<SidebarMaxInstanceChanges> =
        ((maxInstances) => ({
            type: WineryActions.CHANGE_MAX_INSTANCES,
            maxInstances: maxInstances.maxInstances
        }));
    incMinInstances: ActionCreator<IncMinInstances> =
        ((minInstances) => ({
            type: WineryActions.INC_MIN_INSTANCES,
            minInstances: minInstances.minInstances
        }));
    incMaxInstances: ActionCreator<IncMaxInstances> =
        ((maxInstances) => ({
            type: WineryActions.INC_MAX_INSTANCES,
            maxInstances: maxInstances.maxInstances
        }));
    decMinInstances: ActionCreator<DecMinInstances> =
        ((minInstances) => ({
            type: WineryActions.DEC_MIN_INSTANCES,
            minInstances: minInstances.minInstances
        }));
    decMaxInstances: ActionCreator<DecMaxInstances> =
        ((maxInstances) => ({
            type: WineryActions.DEC_MAX_INSTANCES,
            maxInstances: maxInstances.maxInstances
        }));
    saveNodeTemplate: ActionCreator<SaveNodeTemplateAction> =
        ((newNode) => ({
            type: WineryActions.SAVE_NODE_TEMPLATE,
            nodeTemplate: newNode
        }));
    saveRelationship: ActionCreator<SaveRelationshipAction> =
        ((newRelationship) => ({
            type: WineryActions.SAVE_RELATIONSHIP,
            relationshipTemplate: newRelationship
        }));
    deleteNodeTemplate: ActionCreator<DeleteNodeAction> =
        ((deletedNodeId) => ({
            type: WineryActions.DELETE_NODE_TEMPLATE,
            nodeTemplateId: deletedNodeId
        }));
    updateNodeCoordinates: ActionCreator<UpdateNodeCoordinatesAction> =
        ((currentNodeCoordinates) => ({
            type: WineryActions.UPDATE_NODE_COORDINATES,
            otherAttributes: currentNodeCoordinates
        }));
    updateRelationshipName: ActionCreator<UpdateRelationshipNameAction> =
        ((currentRelData) => ({
            type: WineryActions.UPDATE_REL_DATA,
            relData: currentRelData.relData
        }));
    setProperty: ActionCreator<SetPropertyAction> =
        ((newProperty) => ({
            type: WineryActions.SET_PROPERTY,
            nodeProperty: newProperty.nodeProperty,
            propertyType: newProperty.propertyType
        }));
    setDeploymentArtifactsProperty: ActionCreator<SetDepArtifactsPropertyAction> =
        ((newDepArtifactsProperty) => ({
            type: WineryActions.SET_DEPLOYMENT_ARTIFACTS_PROPERTY,
            nodeDepArtProperty: newDepArtifactsProperty.nodeDepArtProperty,
            propertyType: newDepArtifactsProperty.propertyType
        }));
    setRequirementsProperty: ActionCreator<SetReqPropertyAction> =
        ((newReqProperty) => ({
            type: WineryActions.SET_REQUIREMENT_PROPERTY,
            nodeReqProperty: newReqProperty.nodeReqProperty,
            propertyType: newReqProperty.propertyType
        }));
    setCapabilityProperty: ActionCreator<SetCapPropertyAction> =
        ((newCapProperty) => ({
            type: WineryActions.SET_CAPABILITY_PROPERTY,
            nodeCapProperty: newCapProperty.nodeCapProperty,
            propertyType: newCapProperty.propertyType
        }));
    setPoliciesProperty: ActionCreator<SetPoliciesPropertyAction> =
        ((newPoliciesProperty) => ({
            type: WineryActions.SET_POLICIES_PROPERTY,
            nodePoliciesProperty: newPoliciesProperty.nodePoliciesProperty,
            propertyType: newPoliciesProperty.propertyType
        }));
    setTargetLocProperty: ActionCreator<SetTargetLocPropertyAction> =
        ((newTargetLocProperty) => ({
            type: WineryActions.SET_TARGET_LOCATIONS_PROPERTY,
            nodeTargetLocProperty: newTargetLocProperty.nodeTargetLocProperty,
            propertyType: newTargetLocProperty.propertyType
        }));
    setCapability: ActionCreator<SetCababilityAction> =
        ((newCapability) => ({
            type: WineryActions.SET_CAPABILITY,
            nodeCapability: newCapability
        }));
    setRequirement: ActionCreator<SetRequirementAction> =
        ((newRequirement) => ({
            type: WineryActions.SET_REQUIREMENT,
            nodeRequirement: newRequirement
        }));
    setDeploymentArtifact: ActionCreator<SetDeploymentArtifactsAction> =
        ((newDepArt) => ({
            type: WineryActions.SET_DEPLOYMENT_ARTIFACT,
            nodeDeploymentArtifact: newDepArt
        }));
    setPolicy: ActionCreator<SetPolicyAction> =
        ((newPolicy) => ({
            type: WineryActions.SET_POLICY,
            nodePolicy: newPolicy
        }));
    sendCurrentNodeId: ActionCreator<SendCurrentNodeIdAction> =
        ((currentNodeData) => ({
            type: WineryActions.SEND_CURRENT_NODE_ID,
            currentNodeData: currentNodeData
        }));
}