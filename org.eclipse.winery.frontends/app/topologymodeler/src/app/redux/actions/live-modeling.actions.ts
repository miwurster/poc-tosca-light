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
import { LiveModelingStates, ServiceTemplateInstanceStates } from '../../models/enums';
import { LiveModelingLog } from '../../models/liveModelingLog';
import { LiveModelingNodeTemplateData } from '../../models/liveModelingNodeTemplateData';
import { InputParameter } from '../../models/container/input-parameter.model';
import { Csar } from '../../models/container/csar.model';

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

export interface SetCurrentCsarAction extends Action {
    csar: Csar;
}

export interface SetCurrentServiceTemplateInstanceIdAction extends Action {
    serviceTemplateInstanceId: string;
}

export interface SetNodeTemplateDataAction extends Action {
    nodeTemplateData: LiveModelingNodeTemplateData;
}

export interface SetCurrentServiceTemplateInstanceStateAction extends Action {
    serviceTemplateInstanceState: ServiceTemplateInstanceStates;
}

export interface SetBuildPlanInputParametersAction extends Action {
    inputParameters: Array<InputParameter>;
}

export interface SetSettingsAction extends Action {
    settings: any;
}

/**
 * Actions for live modeling
 */
@Injectable()
export class LiveModelingActions {
    static SET_STATE = 'SET_STATE';
    static SEND_LOG = 'SEND_LOG';
    static CLEAR_LOGS = 'CLEAR_LOGS';
    static SET_CONTAINER_URL = 'SET_CONTAINER_URL';
    static SET_CURRENT_CSAR_ID = 'SET_CURRENT_CSAR_ID';
    static SET_CURRENT_CSAR = 'SET_CURRENT_CSAR';
    static SET_CURRENT_SERVICE_TEMPLATE_INSTANCE_ID = 'SET_CURRENT_SERVICE_TEMPLATE_INSTANCE_ID';
    static SET_NODE_TEMPLATE_DATA = 'SET_NODE_TEMPLATE_DATA';
    static DELETE_NODE_TEMPLATE_DATA = 'DELETE_NODE_TEMPLATE_DATA';
    static SET_CURRENT_SERVICE_TEMPLATE_INSTANCE_STATE = 'SET_CURRENT_SERVICE_TEMPLATE_INSTANCE_STATE';
    static SET_BUILD_PLAN_INPUT_PARAMETERS = 'SET_BUILD_PLAN_INPUT_PARAMETERS';
    static SET_SETTINGS = 'SET_SETTING';
    static DISABLE_LIVE_MODELING = 'DISABLE_LIVE_MODELING';

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

    clearLogs(): Action {
        return {
            type: LiveModelingActions.CLEAR_LOGS
        };
    }

    setContainerUrl(containerUrl: string): SetContainerUrlAction {
        return {
            type: LiveModelingActions.SET_CONTAINER_URL,
            containerUrl: containerUrl
        };
    }

    setCurrentCsar(csar: Csar): SetCurrentCsarAction {
        return {
            type: LiveModelingActions.SET_CURRENT_CSAR,
            csar: csar
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
        return {
            type: LiveModelingActions.DELETE_NODE_TEMPLATE_DATA
        };
    }

    setCurrentServiceTemplateInstanceState(serviceTemplateInstanceState: ServiceTemplateInstanceStates): SetCurrentServiceTemplateInstanceStateAction {
        return {
            type: LiveModelingActions.SET_CURRENT_SERVICE_TEMPLATE_INSTANCE_STATE,
            serviceTemplateInstanceState: serviceTemplateInstanceState
        };
    }

    setBuildPlanInputParameters(inputParameters: Array<InputParameter>): SetBuildPlanInputParametersAction {
        return {
            type: LiveModelingActions.SET_BUILD_PLAN_INPUT_PARAMETERS,
            inputParameters: inputParameters
        };
    }

    setSettings(settings: any): SetSettingsAction {
        return {
            type: LiveModelingActions.SET_SETTINGS,
            settings: settings
        };
    }

    disableLiveModeling(): Action {
        return {
            type: LiveModelingActions.DISABLE_LIVE_MODELING
        };
    }
}
