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
import { NgRedux } from '@angular-redux/store';
import { IWineryState } from '../redux/store/winery.store';
import { LiveModelingLog, LiveModelingLogTypes } from '../models/liveModelingLog';
import { WineryActions } from '../redux/actions/winery.actions';

@Injectable()
export class LiveModelingLoggerService {
    constructor(private ngRedux: NgRedux<IWineryState>,
                private wineryActions: WineryActions) {
    }

    public logInfo(message: string) {
        this.ngRedux.dispatch(this.wineryActions.sendLiveModelingLog(new LiveModelingLog(message, LiveModelingLogTypes.INFO)));
    }

    public logSuccess(message: string) {
        this.ngRedux.dispatch(this.wineryActions.sendLiveModelingLog(new LiveModelingLog(message, LiveModelingLogTypes.SUCCESS)));
    }

    public logWarning(message: string) {
        this.ngRedux.dispatch(this.wineryActions.sendLiveModelingLog(new LiveModelingLog(message, LiveModelingLogTypes.WARNING)));
    }

    public logError(message: string) {
        this.ngRedux.dispatch(this.wineryActions.sendLiveModelingLog(new LiveModelingLog(message, LiveModelingLogTypes.DANGER)));
    }
}
