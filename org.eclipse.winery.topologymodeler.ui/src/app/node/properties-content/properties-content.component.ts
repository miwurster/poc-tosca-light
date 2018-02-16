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
    @Input() currentTableRowIndex: any;
    @Input() currentElement: any;
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
                // checks if there is an incoming capability
                if (changes.currentElement.currentValue) {
                    // get type for determining which type of template is shown (KV, NONE, XML)
                    const currentCapType = changes.currentElement.currentValue.type;
                    this.findOutPropertyDefinitionType(currentCapType,
                        this.currentNodeData.entityTypes.capabilityTypes);
                    if (this.propertyDefinitionType === 'KV') {
                        // sets the properties of the capability if there are some
                        if (this.currentElement.properties) {
                            this.nodeProperties = this.currentElement.properties.kvproperties;
                        } else {
                            // if the capability has the type 'KV' but yet no properties, set the default ones from
                            // the corresponding capabilityType
                            this.currentNodeData.entityTypes.capabilityTypes.some(capType => {
                                if (capType.qName === currentCapType) {
                                    this.setKVProperties(capType);
                                }
                            });
                        }
                    } else if (this.propertyDefinitionType === 'XML') {
                        // sets the xml properties of the capability if there are some
                        if (changes.currentElement.currentValue.properties) {
                            this.nodeProperties = changes.currentElement.currentValue.properties.any;
                        } else {
                            // if the capability has the type 'XML' but yet no properties, set the default ones from
                            // the corresponding capabilityType
                            this.currentNodeData.entityTypes.capabilityTypes.some(capType => {
                                if (capType.qName === currentCapType) {
                                    this.nodeProperties = capType.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].propertiesDefinition.element;
                                }
                            });
                        }
                    }
                }
            } else if (this.currentNodeData.currentNodePart === 'REQUIREMENTS') {
                // checks if there is an incoming requirement
                if (changes.currentElement.currentValue) {
                    // get type for determining which type of template is shown (KV, NONE, XML)
                    const currentReqType = changes.currentElement.currentValue.type;
                    this.findOutPropertyDefinitionType(currentReqType,
                        this.currentNodeData.entityTypes.requirementTypes);
                    if (this.propertyDefinitionType === 'KV') {
                        // sets the properties of the requirement if there are some
                        if (this.currentElement.properties) {
                            this.nodeProperties = this.currentElement.properties.kvproperties;
                        } else {
                            // if the requirement has the type 'KV' but yet no properties, set the default ones from
                            // the corresponding requirementType
                            this.currentNodeData.entityTypes.requirementTypes.some(reqType => {
                                if (reqType.qName === currentReqType) {
                                    this.setKVProperties(reqType);
                                }
                            });
                        }
                    } else if (this.propertyDefinitionType === 'XML') {
                        // sets the xml properties of the requirement if there are some
                        if (changes.currentElement.currentValue.properties) {
                            this.nodeProperties = changes.currentElement.currentValue.properties.any;
                        } else {
                            // if the requirement has the type 'XML' but yet no properties, set the default ones from
                            // the corresponding requirementType
                            this.currentNodeData.entityTypes.requirementTypes.some(reqType => {
                                if (reqType.qName === currentReqType) {
                                    this.nodeProperties = reqType.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].propertiesDefinition.element;
                                }
                            });
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
                }
                // function for updating the properties of requirement, capabilities etc.
                // so that the reducer gets the whole capability, requirement etc. object,
                // so then it just has to save it into the store, without further proceding
                this.updatePropertiesInNodeTemplate(value);
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
     * This function sets the node's KV properties corresponding to the type for new generated Elements which
     * have the KV Properties type but yet no kv properties
     * @param any type : the element type, e.g. capabilityType, requirementType etc.
     */
    setKVProperties(type: any): void {
        const kvProperties = type.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].any[0].propertyDefinitionKVList;
        for (const obj of kvProperties) {
            const key = obj.key;
            let value;
            if (isNullOrUndefined(obj.value)) {
                value = '';
            } else {
                value = obj.value;
            }
            const keyValuePair = {
                [key]: value
            };
            this.nodeProperties = { ...this.nodeProperties, ...keyValuePair };
        }
    }

    /**
     * Subscribed to the textArea of the corresponding property component type the function updates the node template
     * properties which are then ready to be saved to the redux store
     * @param any value
     */
    updatePropertiesInNodeTemplate(value: any): void {
        if (this.currentNodeData.currentNodePart === 'CAPABILITIES') {
            if (isNullOrUndefined(this.currentNodeData.nodeTemplate.capabilities.capability[this.currentTableRowIndex].properties)) {
                const capability = {
                    ...this.currentNodeData.nodeTemplate.capabilities.capability[this.currentTableRowIndex],
                    properties: this.propertyDefinitionType === 'KV' ? { kvproperties: this.nodeProperties } : { any: value }
                };
                this.currentNodeData.nodeTemplate.capabilities.capability[this.currentTableRowIndex] = capability;
            } else {
                if (this.propertyDefinitionType === 'KV') {
                    this.currentNodeData.nodeTemplate.capabilities.capability[this.currentTableRowIndex].properties.kvproperties = this.nodeProperties;
                } else {
                    this.currentNodeData.nodeTemplate.capabilities.capability[this.currentTableRowIndex].properties.any = value;
                }
            }
        } else if (this.currentNodeData.currentNodePart === 'REQUIREMENTS') {
            if (isNullOrUndefined(this.currentNodeData.nodeTemplate.requirements.requirement[this.currentTableRowIndex].properties)) {
                const requirement = {
                    ...this.currentNodeData.nodeTemplate.requirements.requirement[this.currentTableRowIndex],
                    properties: this.propertyDefinitionType === 'KV' ? { kvproperties: this.nodeProperties } : { any: value }
                };
                this.currentNodeData.nodeTemplate.requirements.requirement[this.currentTableRowIndex] = requirement;
            } else {
                if (this.propertyDefinitionType === 'KV') {
                    this.currentNodeData.nodeTemplate.requirements.requirement[this.currentTableRowIndex].properties.kvproperties = this.nodeProperties;
                } else {
                    this.currentNodeData.nodeTemplate.requirements.requirement[this.currentTableRowIndex].properties.any = value;
                }
            }
        }
    }

    /**
     * This function determines which kind of properties the nodeType embodies.
     * We have 3 possibilities: none, XML element, or Key value pairs.
     * @param {string} type
     */
    findOutPropertyDefinitionTypeForProperties(type: string): void {
        if (this.currentNodeData.entityTypes.groupedNodeTypes) {
            for (const nameSpace of this.currentNodeData.entityTypes.groupedNodeTypes) {
                for (const nodeTypeVar of nameSpace.children) {
                    if (nodeTypeVar.id === type) {
                        // if PropertiesDefinition doesn't exist then it must be of type NONE
                        if (isNullOrUndefined(nodeTypeVar.full.serviceTemplateOrNodeTypeOrNodeTypeImplementation[0].propertiesDefinition)) {
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
     * @param {string} type
     * @param {any[]} typeArray
     */
    findOutPropertyDefinitionType(type: string, typeArray: any[]): void {
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
