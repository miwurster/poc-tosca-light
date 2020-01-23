/********************************************************************************
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
 ********************************************************************************/

import { Component, EventEmitter, Input, OnChanges, OnDestroy, OnInit, Output, SimpleChanges, TemplateRef, ViewChild } from '@angular/core';
import { NgRedux } from '@angular-redux/store';
import { IWineryState } from '../redux/store/winery.store';
import { Subject, Subscription } from 'rxjs';
import { WineryActions } from '../redux/actions/winery.actions';
import { JsPlumbService } from '../services/jsPlumb.service';
import { PropertyDefinitionType } from '../models/enums';
import { debounceTime, distinctUntilChanged, tap } from 'rxjs/operators';

@Component({
    selector: 'winery-properties',
    templateUrl: './properties.component.html',
    styleUrls: ['./properties.component.css']
})
export class PropertiesComponent implements OnInit, OnChanges, OnDestroy {
    @Input() currentNodeData: any;
    @Input() readonly: boolean;
    @Input() nodeId: string;
    @Output() errorEmitter: EventEmitter<boolean> = new EventEmitter<boolean>();
    key: string;
    nodeProperties: any;
    invalidNodeProperties: any = {};
    kvPatternMap: any;
    kvDescriptionMap: any;

    subscriptions: Array<Subscription> = [];

    properties: Subject<any> = new Subject<any>();

    checkEnabled: boolean;

    constructor(private ngRedux: NgRedux<IWineryState>,
                private actions: WineryActions,
                private jsPlumbService: JsPlumbService) {
    }

    /**
     * Angular lifecycle event.
     */
    ngOnChanges(changes: SimpleChanges) {
        if (changes.currentNodeData.currentValue.nodeTemplate.properties) {
            try {
                const currentProperties = changes.currentNodeData.currentValue.nodeTemplate.properties;
                if (this.currentNodeData.propertyDefinitionType === PropertyDefinitionType.KV) {
                    this.nodeProperties = currentProperties.kvproperties;
                    this.initKVDescriptionMap();
                    this.initKVPatternMap();
                } else if (this.currentNodeData.propertyDefinitionType === PropertyDefinitionType.XML) {
                    this.nodeProperties = currentProperties.any;
                }
            } catch (e) {
            }
        }
        // repaint jsPlumb to account for height change of the accordion
        setTimeout(() => this.jsPlumbService.getJsPlumbInstance().repaintEverything(), 1);
    }

    ngOnInit() {
        if (this.currentNodeData.nodeTemplate.properties) {
            try {
                const currentProperties = this.currentNodeData.nodeTemplate.properties;
                if (this.currentNodeData.propertyDefinitionType === PropertyDefinitionType.KV) {
                    this.nodeProperties = currentProperties.kvproperties;
                    this.initKVDescriptionMap();
                    this.initKVPatternMap();
                } else if (this.currentNodeData.propertyDefinitionType === PropertyDefinitionType.XML) {
                    this.nodeProperties = currentProperties.any;
                }
            } catch (e) {
            }
        }

        this.subscriptions.push(this.properties.pipe(
            debounceTime(500),
            distinctUntilChanged(),
        ).subscribe(property => {
            if (this.currentNodeData.propertyDefinitionType === PropertyDefinitionType.KV) {
                this.nodeProperties[property.key] = property.value;
                if (this.checkEnabled) {
                    this.checkProperty(property.key, property.value);
                }
            } else {
                this.nodeProperties = property;
            }
            this.ngRedux.dispatch(this.actions.setProperty({
                nodeProperty: {
                    newProperty: this.nodeProperties,
                    propertyType: this.currentNodeData.propertyDefinitionType,
                    nodeId: this.currentNodeData.nodeTemplate.id
                }
            }));
            this.ngRedux.dispatch(this.actions.checkForUnsavedChanges());
        }));

        this.subscriptions.push(this.ngRedux.select(state => state.topologyRendererState.buttonsState.checkNodePropertiesButton)
            .subscribe(checked => {
                this.checkEnabled = checked;
                if (this.checkEnabled) {
                    this.checkAllProperties();
                } else {
                    this.invalidNodeProperties = {};
                    this.errorEmitter.emit(false);
                    this.ngRedux.dispatch(this.actions.setNodePropertyValidity(this.nodeId, false));
                }
            }));
    }

    initKVDescriptionMap() {
        this.kvDescriptionMap = {};
        try {
            const propertyDefinitionKVList =
                this.currentNodeData.entityType.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].any[0].propertyDefinitionKVList;
            propertyDefinitionKVList.forEach(prop => {
                this.kvDescriptionMap[prop.key] = prop['description'];
            });
        } catch (e) {
        }
    }

    initKVPatternMap() {
        this.kvPatternMap = {};
        try {
            const propertyDefinitionKVList =
                this.currentNodeData.entityType.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].any[0].propertyDefinitionKVList;
            propertyDefinitionKVList.forEach(prop => {
                this.kvPatternMap[prop.key] = prop['pattern'];
            });
        } catch (e) {
        }
    }

    hasError(key: string): boolean {
        return !!this.invalidNodeProperties[key];
    }

    checkForErrors() {
        if (Object.keys(this.invalidNodeProperties).length > 0) {
            this.ngRedux.dispatch(this.actions.setNodePropertyValidity(this.nodeId, true));
            this.errorEmitter.emit(true);
        } else {
            this.errorEmitter.emit(false);
            this.ngRedux.dispatch(this.actions.setNodePropertyValidity(this.nodeId, false));
        }
    }

    checkAllProperties() {
        if (this.currentNodeData.propertyDefinitionType === PropertyDefinitionType.KV) {
            Object.keys(this.nodeProperties).forEach(key => {
                this.checkProperty(key, this.nodeProperties[key]);
            });
            this.checkForErrors();
        }
    }

    checkProperty(key: string, value: string) {
        try {
            delete this.invalidNodeProperties[key];
            if (value) {
                if (!(value.startsWith('get_input:') || value.startsWith('get_property:'))) {
                    const pattern = this.kvPatternMap[key];
                    if (!new RegExp(pattern).test(value)) {
                        this.invalidNodeProperties[key] = pattern;
                    }
                }
            }
        } catch (e) {

        } finally {
            this.checkForErrors();
        }
    }

    ngOnDestroy() {
        this.subscriptions.forEach(subscription => subscription.unsubscribe());
    }
}
