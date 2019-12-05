/*******************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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

import { Injectable } from '@angular/core';
import { Action } from 'redux';
import { LiveModelingStates } from '../../models/enums';
import { LiveModelingLog } from '../../models/liveModelingLog';
import { LiveModelingNodeTemplateData } from '../../models/liveModelingNodeTemplateData';

export interface SetStateAction extends Action {
    state: LiveModelingStates;
}

export interface SendLogAction extends Action {
    log: LiveModelingLog;
}

export interface SetContainerUrlAction extends Action {
    containerUrl: string;
}

export interface SetCurrentCsarIdAction extends Action {
    csarId: string;
}

export interface SetCurrentServiceTemplateInstanceIdAction extends Action {
    serviceTemplateInstanceId: string;
}

export interface SetNodeTemplateDataAction extends Action {
    nodeTemplateData: LiveModelingNodeTemplateData;
}

/**
 * Actions for live modeling
 */
@Injectable()
export class LiveModelingActions {
    static SET_STATE = 'SET_STATE';
    static SEND_LOG = 'SEND_LOG';
    static SET_CONTAINER_URL = 'SET_CONTAINER_URL';
    static SET_CURRENT_CSAR_ID = 'SET_CURRENT_CSAR_ID';
    static SET_CURRENT_SERVICE_TEMPLATE_INSTANCE_ID = 'SET_CURRENT_SERVICE_TEMPLATE_INSTANCE_ID';
    static SET_NODE_TEMPLATE_DATA = 'SET_NODE_TEMPLATE_DATA';
    static DELETE_NODE_TEMPLATE_DATA = 'DELETE_NODE_TEMPLATE_DATA';

    setState(state: LiveModelingStates): SetStateAction {
        return {
            type: LiveModelingActions.SET_STATE,
            state: state
        };
    }

    sendLog(log: LiveModelingLog): SendLogAction {
        return {
            type: LiveModelingActions.SEND_LOG,
            log: log
        };
    }

    setContainerUrl(containerUrl: string): SetContainerUrlAction {
        return {
            type: LiveModelingActions.SET_CONTAINER_URL,
            containerUrl: containerUrl
        };
    }

    setCurrentCsarId(csarId: string): SetCurrentCsarIdAction {
        return {
            type: LiveModelingActions.SET_CURRENT_CSAR_ID,
            csarId: csarId
        };
    }

    setCurrentServiceTemplateInstanceId(serviceTemplateInstanceId: string): SetCurrentServiceTemplateInstanceIdAction {
        return {
            type: LiveModelingActions.SET_CURRENT_SERVICE_TEMPLATE_INSTANCE_ID,
            serviceTemplateInstanceId: serviceTemplateInstanceId
        };
    }

    setNodeTemplateData(nodeTemplateData: LiveModelingNodeTemplateData): SetNodeTemplateDataAction {
        return {
            type: LiveModelingActions.SET_NODE_TEMPLATE_DATA,
            nodeTemplateData: nodeTemplateData
        };
    }

    deleteNodeTemplateData(): Action {
        return { type: LiveModelingActions.DELETE_NODE_TEMPLATE_DATA };
    }
}
