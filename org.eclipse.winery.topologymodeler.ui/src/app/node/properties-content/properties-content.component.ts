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
    OnInit,
    SimpleChanges
} from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { NgRedux } from '@angular-redux/store';
import { IWineryState } from '../../redux/store/winery.store';
import { WineryActions } from '../../redux/actions/winery.actions';
import { Subscription } from 'rxjs/Subscription';
import { isNullOrUndefined } from 'util';

@Component({
    selector: 'winery-properties-content',
    templateUrl: './properties-content.component.html',
    styleUrls: ['./properties-content.component.css']
})
export class PropertiesContentComponent implements OnInit, OnChanges, OnDestroy {

    properties: Subject<string> = new Subject<string>();
    keyOfEditedKVProperty: Subject<string> = new Subject<string>();
    propertyDefinitionType: string;
    @Input() currentNodeData: any;
    key: string;

    nodeProperties: any;

    subscriptionProperties: Subscription;
    subscriptionKeyOfEditedKVProperty: Subscription;

    constructor(private $ngRedux: NgRedux<IWineryState>,
                private actions: WineryActions) {
    }

    /**
     * Angular lifecycle event.
     */
    ngOnChanges(changes: SimpleChanges) {
        setTimeout(() => {
            if (this.currentNodeData.currentNodePart === 'PROPERTIES') {
                if (changes.currentNodeData.currentValue.nodeTemplate.properties) {
                    try {
                        const currentProperties = changes.currentNodeData.currentValue.nodeTemplate.properties;
                        if (this.propertyDefinitionType === 'KV') {
                            this.nodeProperties = currentProperties.kvproperties;
                        } else if (this.propertyDefinitionType === 'XML') {
                            this.nodeProperties = currentProperties.any;
                        }
                    } catch (e) {
                    }
                }
            } else if (this.currentNodeData.currentNodePart === 'CAPABILITIES') {
                if (changes.currentNodeData.currentValue.currentCapType) {
                    this.findOutPropertyDefinitionType(changes.currentNodeData.currentValue.currentCapType,
                        this.currentNodeData.entityTypes.capabilityTypes);
                    if (this.propertyDefinitionType === 'KV') {
                        for (const cap of changes.currentNodeData.currentValue.nodeTemplate.capabilities.capability) {
                            if (cap.type === changes.currentNodeData.currentValue.currentCapType) {
                                this.nodeProperties = cap.properties.kvproperties;
                            }
                        }
                    } else if (this.propertyDefinitionType === 'XML') {
                        for (const cap of changes.currentNodeData.currentValue.nodeTemplate.capabilities.capability) {
                            if (cap.type === changes.currentNodeData.currentValue.currentCapType) {
                                if (cap.properties) {
                                    this.nodeProperties = cap.properties.any;
                                }
                            }
                        }
                    }
                }
            } else if (this.currentNodeData.currentNodePart === 'REQUIREMENTS') {
                if (changes.currentNodeData.currentValue.currentReqType) {
                    this.findOutPropertyDefinitionType(changes.currentNodeData.currentValue.currentReqType,
                        this.currentNodeData.entityTypes.requirementTypes);
                    if (this.propertyDefinitionType === 'KV') {
                        for (const cap of changes.currentNodeData.currentValue.nodeTemplate.requirements.requirement) {
                            if (cap.type === changes.currentNodeData.currentValue.currentReqType) {
                                this.nodeProperties = cap.properties.kvproperties;
                            }
                        }
                    } else if (this.propertyDefinitionType === 'XML') {
                        for (const req of changes.currentNodeData.currentValue.nodeTemplate.requirements.requirement) {
                            if (req.type === changes.currentNodeData.currentValue.currentReqType) {
                                if (req.properties) {
                                    this.nodeProperties = req.properties.any;
                                }
                            }
                        }
                    }
                }
            }
        }, 1);
    }

    /**
     * Angular lifecycle event.
     */
    ngOnInit() {
        // find out which type of properties shall be displayed
        if (this.currentNodeData.currentNodePart === 'PROPERTIES') {
            this.findOutPropertyDefinitionTypeForProperties(this.currentNodeData.nodeTemplate.type);
        }

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
                if (this.propertyDefinitionType === 'KV') {
                    this.nodeProperties[this.key] = value;
                } else {
                    this.nodeProperties = value;
                    /*
                    if (this.currentNodeData.currentNodePart === 'REQUIREMENTS') {
                        // this.currentNodeData.nodeTemplate.requirements.requirement[index].properties.any = value;
                }
                */
                }
                switch (this.currentNodeData.currentNodePart) {
                    case 'DEPLOYMENT_ARTIFACTS':
                        this.$ngRedux.dispatch(this.actions.setDeploymentArtifactsProperty({
                            nodeDepArtProperty: {
                                newDepArtProperty: this.nodeProperties,
                                propertyType: this.propertyDefinitionType,
                                nodeId: this.currentNodeData.nodeTemplate.id
                            }
                        }));
                        break;
                    case 'REQUIREMENTS':
                        this.$ngRedux.dispatch(this.actions.setRequirementsProperty({
                            nodeReqProperty: {
                                newReqProperty: this.currentNodeData.nodeTemplate.requirements,
                                propertyType: this.propertyDefinitionType,
                                nodeId: this.currentNodeData.nodeTemplate.id
                            }
                        }));
                        break;
                    case 'CAPABILITIES':
                        this.$ngRedux.dispatch(this.actions.setCapabilityProperty({
                            nodeCapProperty: {
                                newCapProperty: this.currentNodeData.nodeTemplate.capabilities,
                                propertyType: this.propertyDefinitionType,
                                nodeId: this.currentNodeData.nodeTemplate.id
                            }
                        }));
                        break;
                    case 'POLICIES':
                        this.$ngRedux.dispatch(this.actions.setPoliciesProperty({
                            nodePoliciesProperty: {
                                newPoliciesProperty: this.nodeProperties,
                                propertyType: this.propertyDefinitionType,
                                nodeId: this.currentNodeData.nodeTemplate.id
                            }
                        }));
                        break;
                    case 'TARGET_LOCATIONS':
                        this.$ngRedux.dispatch(this.actions.setTargetLocProperty({
                            nodeTargetLocProperty: {
                                newTargetLocProperty: this.nodeProperties,
                                propertyType: this.propertyDefinitionType,
                                nodeId: this.currentNodeData.nodeTemplate.id
                            }
                        }));
                        break;
                    case 'PROPERTIES':
                        console.log(this.nodeProperties);
                        this.$ngRedux.dispatch(this.actions.setProperty({
                            nodeProperty: {
                                newProperty: this.nodeProperties,
                                propertyType: this.propertyDefinitionType,
                                nodeId: this.currentNodeData.nodeTemplate.id
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
    findOutPropertyDefinitionTypeForProperties(type: any): void {
        if (this.currentNodeData.entityTypes.groupedNodeTypes) {
            for (const nameSpace of this.currentNodeData.entityTypes.groupedNodeTypes) {
                for (const nodeTypeVar of nameSpace.children) {
                    if (nodeTypeVar.id === type) {
                        // if PropertiesDefinition doesn't exist then it must be of type NONE
                        if (nodeTypeVar.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].propertiesDefinition == null) {
                            this.propertyDefinitionType = 'NONE';
                        } else {
                            // if no XML element inside PropertiesDefinition then it must be of type Key Value
                            if (!nodeTypeVar.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].propertiesDefinition.element) {
                                this.propertyDefinitionType = 'KV';
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

    /**
     * This function determines which kind of capability, requirement properties the nodeType embodies.
     * We have 3 possibilities: none, XML element, or Key value pairs.
     * @param {any[]} capabilities
     */
    findOutPropertyDefinitionType(type: any, typeArray: any[]): void {
        for (const capType of typeArray) {
            if (type === capType.qName) {
                // if PropertiesDefinition doesn't exist then it must be of type NONE
                if (isNullOrUndefined(capType.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].propertiesDefinition)) {
                    this.propertyDefinitionType = 'NONE';
                } else {
                    // if no XML element inside PropertiesDefinition then it must be of type Key Value
                    if (!capType.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].propertiesDefinition.element) {
                        this.propertyDefinitionType = 'KV';
                    } else {
                        // else we have XML
                        this.propertyDefinitionType = 'XML';
                    }
                }
            }
        }
    }

    ngOnDestroy() {
        this.subscriptionProperties.unsubscribe();
        this.subscriptionKeyOfEditedKVProperty.unsubscribe();
    }
}
