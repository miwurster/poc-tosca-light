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
import { LiveModelingStates } from '../../models/enums';
import { LiveModelingNodeTemplateData } from '../../models/liveModelingNodeTemplateData';
import { LiveModelingLog } from '../../models/liveModelingLog';
import { Action } from 'redux';
import {
    LiveModelingActions, SendLogAction, SetContainerUrlAction, SetCurrentCsarIdAction, SetCurrentServiceTemplateInstanceIdAction, SetNodeTemplateDataAction,
    SetStateAction
} from '../actions/live-modeling.actions';

export interface LiveModelingState {
    state: LiveModelingStates;
    logs: LiveModelingLog[];
    containerUrl: string;
    currentCsarId: string;
    currentServiceTemplateInstanceId: string;
    nodeTemplatesData: LiveModelingNodeTemplateData[];
}

export const INITIAL_LIVE_MODELING_STATE: LiveModelingState = {
    state: LiveModelingStates.DISABLED,
    logs: <LiveModelingLog[]>[],
    containerUrl: null,
    currentCsarId: null,
    currentServiceTemplateInstanceId: null,
    nodeTemplatesData: <LiveModelingNodeTemplateData[]>[]
};

export const LiveModelingReducer =
    function (lastState: LiveModelingState = INITIAL_LIVE_MODELING_STATE, action: Action): LiveModelingState {
        switch (action.type) {
            case LiveModelingActions.SET_STATE:
                const state = (<SetStateAction>action).state;
                let nextState;

                switch (lastState.state) {
                    case LiveModelingStates.DISABLED:
                        if (state === LiveModelingStates.START) {
                            nextState = state;
                        }
                        break;
                    case LiveModelingStates.START:
                        if (state === LiveModelingStates.UPDATE) {
                            nextState = state;
                        }
                        break;
                    case LiveModelingStates.ENABLED:
                        if (state === LiveModelingStates.REDEPLOY ||
                            state === LiveModelingStates.UPDATE ||
                            state === LiveModelingStates.TERMINATE ||
                            state === LiveModelingStates.DISABLED
                        ) {
                            nextState = state;
                        }
                        break;
                    case LiveModelingStates.REDEPLOY:
                        if (state === LiveModelingStates.ENABLED) {
                            nextState = state;
                        }
                        break;
                    case LiveModelingStates.UPDATE:
                        if (state === LiveModelingStates.ENABLED) {
                            nextState = state;
                        }
                        break;
                    case LiveModelingStates.TERMINATE:
                        if (state === LiveModelingStates.DISABLED) {
                            nextState = state;
                        }
                        break;
                    case LiveModelingStates.ERROR: {
                        nextState = LiveModelingStates.DISABLED;
                    }
                }
                if (!nextState) {
                    nextState = LiveModelingStates.ERROR;
                }

                return <LiveModelingState>{
                    ...lastState,
                    state: nextState
                };
            case LiveModelingActions.SEND_LOG:
                const log = (<SendLogAction>action).log;

                return <LiveModelingState>{
                    ...lastState,
                    logs: [...lastState.logs, log]
                };
            case LiveModelingActions.SET_CONTAINER_URL:
                const containerUrl = (<SetContainerUrlAction>action).containerUrl;

                return <LiveModelingState>{
                    ...lastState,
                    containerUrl: containerUrl
                };
            case LiveModelingActions.SET_CURRENT_CSAR_ID:
                const csarId = (<SetCurrentCsarIdAction>action).csarId;

                return <LiveModelingState>{
                    ...lastState,
                    currentCsarId: csarId
                };
            case LiveModelingActions.SET_CURRENT_SERVICE_TEMPLATE_INSTANCE_ID:
                const serviceTemplateInstanceId = (<SetCurrentServiceTemplateInstanceIdAction>action).serviceTemplateInstanceId;

                return <LiveModelingState>{
                    ...lastState,
                    currentServiceTemplateInstanceId: serviceTemplateInstanceId
                };
            case LiveModelingActions.SET_NODE_TEMPLATE_DATA:
                const nodeTemplateData = (<SetNodeTemplateDataAction>action).nodeTemplateData;

                const nodeTemplateDataExists = lastState.nodeTemplatesData.findIndex(el => el.id === nodeTemplateData.id) > -1;
                const nextNodeTemplatesData = lastState.nodeTemplatesData.slice();
                if (nodeTemplateDataExists) {
                    nextNodeTemplatesData[nextNodeTemplatesData.findIndex(el => el.id === nodeTemplateData.id)] = nodeTemplateData;
                } else {
                    nextNodeTemplatesData.push(nodeTemplateData);
                }

                return <LiveModelingState>{
                    ...lastState,
                    nodeTemplatesData: nextNodeTemplatesData
                };
            case LiveModelingActions.DELETE_NODE_TEMPLATE_DATA:
                return <LiveModelingState>{
                    ...lastState,
                    nodeTemplatesData: <LiveModelingNodeTemplateData[]>[]
                };
            default:
                return <LiveModelingState>lastState;
        }
    };
