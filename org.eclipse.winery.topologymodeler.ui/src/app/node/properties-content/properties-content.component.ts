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

import {
    Component,
    Input,
    OnChanges,
    OnDestroy,
    OnInit, Pipe, PipeTransform,
    SimpleChanges
} from '@angular/core';
import {Subject} from 'rxjs/Subject';
import {NgRedux} from '@angular-redux/store';
import {IWineryState} from '../../redux/store/winery.store';
import {WineryActions} from '../../redux/actions/winery.actions';
import {Subscription} from 'rxjs/Subscription';

@Component({
    selector: 'winery-properties-content',
    templateUrl: './properties-content.component.html',
    styleUrls: ['./properties-content.component.css']
})
export class PropertiesContentComponent implements OnInit, OnChanges, OnDestroy {

    properties: Subject<string> = new Subject<string>();
    keyOfEditedKVProperty: Subject<string> = new Subject<string>();
    propertyDefinitionType: string;
    keys: any[];
    @Input() currentNodeData: any;
    @Input() currentProperties: string;
    @Input() groupedNodeTypes: any[];
    key: string;
    xmlProperty: string;

    nodeProperties;

    subscriptionProperties: Subscription;
    subscriptionKeyOfEditedKVProperty: Subscription;

    constructor (private $ngRedux: NgRedux<IWineryState>,
                 private actions: WineryActions) {
    }

    ngOnChanges (changes: SimpleChanges) {
        setTimeout(() => {
            if (changes.currentProperties) {
                try {
                    const currentProperties = changes.currentProperties.currentValue;
                    if (this.propertyDefinitionType === 'KV') {
                        this.nodeProperties = currentProperties.kvproperties;
                        console.log("nodeProperties: ", this.nodeProperties);
                    } else if (this.propertyDefinitionType === 'XML') {
                        this.xmlProperty = currentProperties.any;
                    }
                } catch (e) {
                }
            }
        }, 1);
    }

    ngOnInit () {
        // find out which type of properties shall be displayed
        this.findOutPropertyDefinitionTypeForThisInstance(this.currentNodeData.currentNodeType);

        // find out which row was edited by key
        this.subscriptionKeyOfEditedKVProperty = this.keyOfEditedKVProperty
            .debounceTime(200)
            .distinctUntilChanged()
            .subscribe(key => {
                this.key = key;
            });
        // set key value property with a debounceTime of 300ms
        this.subscriptionProperties = this.properties
            .debounceTime(300)
            .distinctUntilChanged()
            .subscribe(value => {
                let property;
                if (this.propertyDefinitionType === 'KV') {
                    this.nodeProperties[this.key] = value;
                    property = this.nodeProperties;
                } else {
                    property = value;
                }
                switch (this.currentNodeData.currentNodePart) {
                    case 'DEPLOYMENT_ARTIFACTS':
                        this.$ngRedux.dispatch(this.actions.setDeploymentArtifactsProperty({
                            nodeDepArtProperty: {
                                newDepArtProperty: property,
                                propertyType: this.propertyDefinitionType,
                                nodeId: this.currentNodeData.currentNodeId
                            }
                        }));
                        break;
                    case 'REQUIREMENTS':
                        this.$ngRedux.dispatch(this.actions.setRequirementsProperty({
                            nodeReqProperty: {
                                newReqProperty: property,
                                propertyType: this.propertyDefinitionType,
                                nodeId: this.currentNodeData.currentNodeId
                            }
                        }));
                        break;
                    case 'CAPABILITIES':
                        this.$ngRedux.dispatch(this.actions.setCapabilityProperty({
                            nodeCapProperty: {
                                newCapProperty: property,
                                propertyType: this.propertyDefinitionType,
                                nodeId: this.currentNodeData.currentNodeId
                            }
                        }));
                        break;
                    case 'POLICIES':
                        this.$ngRedux.dispatch(this.actions.setPoliciesProperty({
                            nodePoliciesProperty: {
                                newPoliciesProperty: property,
                                propertyType: this.propertyDefinitionType,
                                nodeId: this.currentNodeData.currentNodeId
                            }
                        }));
                        break;
                    case 'TARGET_LOCATIONS':
                        this.$ngRedux.dispatch(this.actions.setTargetLocProperty({
                            nodeTargetLocProperty: {
                                newTargetLocProperty: property,
                                propertyType: this.propertyDefinitionType,
                                nodeId: this.currentNodeData.currentNodeId
                            }
                        }));
                        break;
                    case 'PROPERTIES':
                        this.$ngRedux.dispatch(this.actions.setProperty({
                            nodeProperty: {
                                newProperty: property,
                                propertyType: this.propertyDefinitionType,
                                nodeId: this.currentNodeData.currentNodeId
                            }
                        }));
                        break;
                }
            });
    }

    /**
     * This function determines which kind of properties the nodeType embodies.
     * We have 3 possibilities: none, XML element, or Key value pairs.
     * @param nodeType
     * @param {any[]} groupedNodeTypes
     */
    findOutPropertyDefinitionTypeForThisInstance (nodeType: any): void {
        if (this.groupedNodeTypes) {
            for (const nameSpace of this.groupedNodeTypes) {
                for (const nodeTypeVar of nameSpace.children) {
                    if (nodeTypeVar.id === nodeType) {
                        // if PropertiesDefinition doesn't exist then it must be of type NONE
                        if (nodeTypeVar.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].propertiesDefinition == null) {
                            this.propertyDefinitionType = 'NONE';
                        } else {
                            // if no XML element inside PropertiesDefinition then it must be of type Key Value
                            if (!nodeTypeVar.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].propertiesDefinition.element) {
                                this.propertyDefinitionType = 'KV';
                                this.keys = nodeTypeVar.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].any[0].propertyDefinitionKVList;
                            } else {
                                // else we have XML
                                this.propertyDefinitionType = 'XML';
                            }
                        }
                    }
                }
            }
        }
    }

    ngOnDestroy () {
        this.subscriptionProperties.unsubscribe();
        this.subscriptionKeyOfEditedKVProperty.unsubscribe();
    }
}
