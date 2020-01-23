/********************************************************************************
 * Copyright (c) 2017-2019 Contributors to the Eclipse Foundation
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

import { Action, ActionCreator } from 'redux';
import { Injectable } from '@angular/core';
import { TNodeTemplate, TRelationshipTemplate } from '../../models/ttopology-template';
import { TDeploymentArtifact } from '../../models/artifactsModalData';
import { TPolicy } from '../../models/policiesModalData';
import { Visuals } from '../../models/visuals';
import { LiveModelingNodeTemplateData } from '../../models/liveModelingNodeTemplateData';

export interface SendPaletteOpenedAction extends Action {
    paletteOpened: boolean;
}

export interface HideNavBarAndPaletteAction extends Action {
    hideNavBarAndPalette: boolean;
}

export interface SidebarStateAction extends Action {
    sidebarContents: {
        sidebarVisible: boolean,
        nodeClicked: boolean,
        id: string,
        nameTextFieldValue: string,
        type: string,
        minInstances: string,
        maxInstances: string,
        properties: string,
        liveModelingNodeTemplateData: LiveModelingNodeTemplateData,
        source: string,
        target: string
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

export interface DeleteRelationshipAction extends Action {
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

export interface SetDeploymentArtifactAction extends Action {
    nodeDeploymentArtifact: {
        nodeId: string,
        newDeploymentArtifact: TDeploymentArtifact
    };
}

export interface DeleteDeploymentArtifactAction extends Action {
    nodeDeploymentArtifact: {
        nodeId: string,
        deletedDeploymentArtifact: any
    };
}

export interface SetPolicyAction extends Action {
    nodePolicy: {
        nodeId: string,
        newPolicy: TPolicy
    };
}

export interface SetTargetLocation extends Action {
    nodeTargetLocation: {
        nodeId: string,
        newTargetLocation: string
    };
}

export interface DeletePolicyAction extends Action {
    nodePolicy: {
        nodeId: string,
        deletedPolicy: any
    };
}

export interface SendCurrentNodeIdAction extends Action {
    currentNodeData: any;
}

export interface SetNodeVisuals extends Action {
    visuals: Visuals[];
}

export interface SendLiveModelingSidebarOpenedAction extends Action {
    sidebarOpened: boolean;
}

export interface SetOverlayContentAction extends Action {
    content: string;
}

export interface SetOverlayVisibilityAction extends Action {
    visible: boolean;
}

export interface SetNodePropertyValidityAction extends Action {
    nodeId: string;
    invalid: boolean;
}

/**
 * Winery Actions
 */
@Injectable()
export class WineryActions {

    static SEND_PALETTE_OPENED = 'SEND_PALETTE_OPENED';
    static HIDE_NAVBAR_AND_PALETTE = 'HIDE_NAVBAR_AND_PALETTE';
    static SAVE_NODE_TEMPLATE = 'SAVE_NODE_TEMPLATE';
    static SAVE_RELATIONSHIP = 'SAVE_RELATIONSHIP';
    static DELETE_NODE_TEMPLATE = 'DELETE_NODE_TEMPLATE';
    static DELETE_RELATIONSHIP_TEMPLATE = 'DELETE_RELATIONSHIP_TEMPLATE';
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
    static SET_DEPLOYMENT_ARTIFACT = 'SET_DEPLOYMENT_ARTIFACT';
    static DELETE_DEPLOYMENT_ARTIFACT = 'DELETE_DEPLOYMENT_ARTIFACT';
    static SET_POLICY = 'SET_POLICY';
    static SET_TARGET_LOCATION = 'SET_TARGET_LOCATION';
    static DELETE_POLICY = 'DELETE_POLICY';
    static SEND_CURRENT_NODE_ID = 'SEND_CURRENT_NODE_ID';
    static SET_NODE_VISUALS = 'SET_NODE_VISUALS';
    static SEND_LIVE_MODELING_SIDEBAR_OPENED = 'SEND_LIVE_MODELING_SIDEBAR_OPENED';

    static SET_OVERLAY_CONTENT = 'SET_OVERLAY_CONTENT';
    static SET_OVERLAY_VISIBILITY = 'SET_OVERLAY_VISIBILITY';

    static SAVE_TOPOLOGY_TEMPLATE = 'SAVE_TOPOLOGY_TEMPLATE';
    static CHECK_FOR_UNSAVED_CHANGES = 'CHECK_FOR_UNSAVED_CHANGES';
    static SET_NODE_PROPERTY_VALIDITY = 'SET_NODE_PROPERTY_VALIDITY';

    sendPaletteOpened(paletteOpened: boolean): SendPaletteOpenedAction {
        return {
            type: WineryActions.SEND_PALETTE_OPENED,
            paletteOpened: paletteOpened
        };
    }

    hideNavBarAndPalette(hideNavBarAndPalette: boolean): HideNavBarAndPaletteAction {
        return {
            type: WineryActions.HIDE_NAVBAR_AND_PALETTE,
            hideNavBarAndPalette: hideNavBarAndPalette
        };
    }

    openSidebar(newSidebarData): SidebarStateAction {
        return {
            type: WineryActions.OPEN_SIDEBAR,
            sidebarContents: newSidebarData.sidebarContents
        };
    }

    changeNodeName(nodeNames): SidebarNodeNamechange {
        return {
            type: WineryActions.CHANGE_NODE_NAME,
            nodeNames: nodeNames.nodeNames
        };
    }

    changeMinInstances(minInstances): SidebarMinInstanceChanges {
        return {
            type: WineryActions.CHANGE_MIN_INSTANCES,
            minInstances: minInstances.minInstances
        };
    }

    changeMaxInstances(maxInstances): SidebarMaxInstanceChanges {
        return {
            type: WineryActions.CHANGE_MAX_INSTANCES,
            maxInstances: maxInstances.maxInstances
        };
    }

    incMinInstances(minInstances): IncMinInstances {
        return {
            type: WineryActions.INC_MIN_INSTANCES,
            minInstances: minInstances.minInstances
        };
    }

    incMaxInstances(maxInstances): IncMaxInstances {
        return {
            type: WineryActions.INC_MAX_INSTANCES,
            maxInstances: maxInstances.maxInstances
        };
    }

    decMinInstances(minInstances): DecMinInstances {
        return {
            type: WineryActions.DEC_MIN_INSTANCES,
            minInstances: minInstances.minInstances
        };
    }

    decMaxInstances(maxInstances): DecMaxInstances {
        return {
            type: WineryActions.DEC_MAX_INSTANCES,
            maxInstances: maxInstances.maxInstances
        };
    }

    saveNodeTemplate(newNode): SaveNodeTemplateAction {
        return {
            type: WineryActions.SAVE_NODE_TEMPLATE,
            nodeTemplate: newNode
        };
    }

    saveRelationship(newRelationship): SaveRelationshipAction {
        return {
            type: WineryActions.SAVE_RELATIONSHIP,
            relationshipTemplate: newRelationship
        };
    }

    deleteNodeTemplate(deletedNodeId): DeleteNodeAction {
        return {
            type: WineryActions.DELETE_NODE_TEMPLATE,
            nodeTemplateId: deletedNodeId
        };
    }

    deleteRelationshipTemplate(deletedRelationshipId): DeleteRelationshipAction {
        return {
            type: WineryActions.DELETE_RELATIONSHIP_TEMPLATE,
            nodeTemplateId: deletedRelationshipId
        };
    }

    updateNodeCoordinates(currentNodeCoordinates): UpdateNodeCoordinatesAction {
        return {
            type: WineryActions.UPDATE_NODE_COORDINATES,
            otherAttributes: currentNodeCoordinates
        };
    }

    updateRelationshipName(currentRelData): UpdateRelationshipNameAction {
        return {
            type: WineryActions.UPDATE_REL_DATA,
            relData: currentRelData.relData
        };
    }

    setProperty(newProperty): SetPropertyAction {
        return {
            type: WineryActions.SET_PROPERTY,
            nodeProperty: newProperty.nodeProperty
        };
    }

    setCapability(newCapability): SetCababilityAction {
        return {
            type: WineryActions.SET_CAPABILITY,
            nodeCapability: newCapability
        };
    }

    setRequirement(newRequirement): SetRequirementAction {
        return {
            type: WineryActions.SET_REQUIREMENT,
            nodeRequirement: newRequirement
        };
    }

    setDeploymentArtifact(newDepArt): SetDeploymentArtifactAction {
        return {
            type: WineryActions.SET_DEPLOYMENT_ARTIFACT,
            nodeDeploymentArtifact: newDepArt
        };
    }

    deleteDeploymentArtifact(deletedDeploymentArtifact): DeleteDeploymentArtifactAction {
        return {
            type: WineryActions.DELETE_DEPLOYMENT_ARTIFACT,
            nodeDeploymentArtifact: deletedDeploymentArtifact
        };
    }

    setPolicy(newPolicy): SetPolicyAction {
        return {
            type: WineryActions.SET_POLICY,
            nodePolicy: newPolicy
        };
    }

    setTargetLocation(newTargetLocation): SetTargetLocation {
        return {
            type: WineryActions.SET_TARGET_LOCATION,
            nodeTargetLocation: newTargetLocation
        };
    }

    deletePolicy(deletedPolicy): DeletePolicyAction {
        return {
            type: WineryActions.DELETE_POLICY,
            nodePolicy: deletedPolicy
        };
    }

    sendCurrentNodeId(currentNodeData): SendCurrentNodeIdAction {
        return {
            type: WineryActions.SEND_CURRENT_NODE_ID,
            currentNodeData: currentNodeData
        };
    }

    setNodeVisuals(visuals): SetNodeVisuals {
        return {
            type: WineryActions.SET_NODE_VISUALS,
            visuals: visuals
        };
    }

    sendLiveModelingSidebarOpened(sidebarOpened): SendLiveModelingSidebarOpenedAction {
        return {
            type: WineryActions.SEND_LIVE_MODELING_SIDEBAR_OPENED,
            sidebarOpened: sidebarOpened
        };
    }

    setOverlayContent(content): SetOverlayContentAction {
        return {
            type: WineryActions.SET_OVERLAY_CONTENT,
            content: content
        };
    }

    setOverlayVisibility(visible): SetOverlayVisibilityAction {
        return {
            type: WineryActions.SET_OVERLAY_VISIBILITY,
            visible: visible
        };
    }

    saveTopologyTemplate(): Action {
        return {
            type: WineryActions.SAVE_TOPOLOGY_TEMPLATE
        };
    }

    checkForUnsavedChanges(): Action {
        return {
            type: WineryActions.CHECK_FOR_UNSAVED_CHANGES
        };
    }

    setNodePropertyValidity(nodeId: string, invalid: boolean): SetNodePropertyValidityAction {
        return {
            type: WineryActions.SET_NODE_PROPERTY_VALIDITY,
            nodeId: nodeId,
            invalid: invalid
        };
    }
}
