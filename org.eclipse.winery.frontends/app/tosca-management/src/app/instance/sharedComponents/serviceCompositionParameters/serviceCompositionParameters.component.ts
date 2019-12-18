/*******************************************************************************
 * Copyright (c) 2017-2018 Contributors to the Eclipse Foundation
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
import { Component, OnInit } from '@angular/core';
import { InstanceService } from '../../instance.service';
import { WineryNotificationService } from '../../../wineryNotificationModule/wineryNotification.service';
import { InterfaceParameter } from '../../../model/parameters';
import { YesNoEnum } from '../../../model/enums';

@Component({
    templateUrl: 'serviceCompositionParameters.component.html',
})
export class ServiceCompositionParametersComponent implements OnInit {

    loading = false;
    inputParameters: InterfaceParameter[];
    outputParameters: InterfaceParameter[];

    constructor(public sharedData: InstanceService, private notify: WineryNotificationService) { }

    ngOnInit() {
        this.inputParameters = [];
        this.outputParameters = [];
    }

    save() {
        this.loading = true;
        // TODO
        console.log('Input parameters: ' + this.inputParameters.length);
    }

    private handleSave() {
        this.loading = false;
        this.notify.success('Changes saved!');
        // TODO
    }
}
