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

import 'rxjs/add/operator/do';
import {Component, OnInit} from '@angular/core';
import {EntityType, TNodeTemplate, TRelationshipTemplate} from './models/ttopology-template';
import {IWineryState} from './redux/store/winery.store';
import {WineryActions} from './redux/actions/winery.actions';
import {NgRedux} from '@angular-redux/store';
import {ILoaded, LoadedService} from './loaded.service';
import {AppReadyEventService} from './app-ready-event.service';
import {BackendService} from './backend.service';
import {QName} from './qname';

/**
 * This is the root component of the topology modeler.
 */
@Component({
    selector: 'winery-topologymodeler',
    templateUrl: './winery.component.html',
    styleUrls: ['./winery.component.css']
})
export class WineryComponent implements OnInit {
    nodeTemplates: Array<TNodeTemplate> = [];
    relationshipTemplates: Array<TRelationshipTemplate> = [];
    artifactTypes: Array<any> = [];
    policyTypes: Array<any> = [];
    policyTemplates: Array<any> = [];
    capabilityTypes: Array<any> = [];
    requirementTypes: Array<any> = [];
    groupedNodeTypes: Array<any> = [];
    relationshipTypes: Array<any> = [];
    // This object contains all the arrays from above after all GET requests are done by the backend service
    // This object entityTypes is passed to all child components. Looks like this in the memory:
    // entityTypes:
    //     artifactTypes: [],
    //     policyTypes: [],
    //     policyTemplates: [],
    //     capabilityTypes: [],
    //     requirementTypes: [],
    //     groupedNodeTypes: [],
    //     relationshipTypes: []
    entityTypes: any = {};

    public loaded: ILoaded;

    constructor (private ngRedux: NgRedux<IWineryState>,
                 private actions: WineryActions,
                 private loadedService: LoadedService,
                 private appReadyEvent: AppReadyEventService,
                 private backendService: BackendService) {
        // Loading Animation
        this.loaded = null;
        this.loadedService.getLoadingState()
            .subscribe((isAppLoaded) => {
                this.loaded = isAppLoaded;
                this.appReadyEvent.trigger();
            });

    }

    /**
     * Angular LifeCycle function OnInit().
     * All necessary data is being requested inside this function via the backendService instance.
     * The data is passed to various init...() functions that parse the received JSON data into Objects that get stored
     * inside the Redux store of this application.
     */
    ngOnInit () {
        /**
         * This subscriptionProperties receives an Observable of [string, string], the former value being
         * the JSON representation of the topologyTemplate and the latter value being the JSON
         * representation of the node types' visual appearances
         * the backendService makes sure that both get requests finish before pushing data onto this Observable
         * by using Observable.forkJoin(1$, 2$);
         */
        this.backendService.topologyTemplateAndVisuals$.subscribe(JSON => {
            // topologyTemplate JSON[0]
            const topologyTemplate = JSON[0];
            // visuals JSON[1]
            const visuals = JSON[1];
            // init the NodeTemplates and RelationshipTemplates to start their rendering
            this.initTopologyTemplate(topologyTemplate.nodeTemplates, visuals, topologyTemplate.relationshipTemplates);

        });

        // Get other entity types
        // Artifact Types
        this.backendService.artifactTypes$.subscribe(JSON => {
            this.initEntityType(JSON, 'artifactTypes');
        });
        // Policy Types
        this.backendService.policyTypes$.subscribe(JSON => {
            this.initEntityType(JSON, 'policyTypes');
        });
        // Capability Types
        this.backendService.capabilityTypes$.subscribe(JSON => {
            this.initEntityType(JSON, 'capabilityTypes');
        });
        // Requirement Types
        this.backendService.requirementTypes$.subscribe(JSON => {
            this.initEntityType(JSON, 'requirementTypes');
        });
        // PolicyTemplates
        this.backendService.policyTemplates$.subscribe(JSON => {
            this.initEntityType(JSON, 'policyTemplates');
        });
        // Grouped NodeTypes
        this.backendService.groupedNodeTypes$.subscribe(JSON => {
            this.initEntityType(JSON, 'groupedNodeTypes');
        });
        // Relationship Types
        this.backendService.relationshipTypes$.subscribe(JSON => {
            this.initEntityType(JSON, 'relationshipTypes');
        });
    }

    /**
     * Save the received Array of Entity Types inside the respective variables
     * @param {Array<any>} entityTypeJSON
     * @param {string} entityType
     */
    initEntityType (entityTypeJSON: Array<any>, entityType: string): void {
        switch (entityType) {
            case 'artifactTypes': {
                for (const artifactType of entityTypeJSON) {
                    this.artifactTypes
                        .push(new EntityType(
                            artifactType.id,
                            artifactType.qName,
                            artifactType.name,
                            artifactType.namespace
                        ));
                }
                this.entityTypes.artifactTypes = this.artifactTypes;
                break;
            }
            case 'policyTypes': {
                for (const policyType of entityTypeJSON) {
                    this.policyTypes
                        .push(new EntityType(
                            policyType.id,
                            policyType.qName,
                            policyType.name,
                            policyType.namespace
                        ));
                }
                this.entityTypes.policyTypes = this.policyTypes;
                break;
            }
            case 'capabilityTypes': {
                for (const capabilityType of entityTypeJSON) {
                    this.capabilityTypes
                        .push(new EntityType(
                            capabilityType.id,
                            capabilityType.qName,
                            capabilityType.name,
                            capabilityType.namespace
                        ));
                }
                this.entityTypes.capabilityTypes = this.capabilityTypes;
                break;
            }
            case 'requirementTypes': {
                for (const requirementType of entityTypeJSON) {
                    this.requirementTypes
                        .push(new EntityType(
                            requirementType.id,
                            requirementType.qName,
                            requirementType.name,
                            requirementType.namespace
                        ));
                }
                this.entityTypes.requirementTypes = this.requirementTypes;
                break;
            }
            case 'policyTemplates': {
                for (const policyTemplate of entityTypeJSON) {
                    this.policyTemplates
                        .push(new EntityType(
                            policyTemplate.id,
                            policyTemplate.qName,
                            policyTemplate.name,
                            policyTemplate.namespace
                        ));
                }
                this.entityTypes.policyTemplates = this.policyTemplates;
                break;
            }
            case 'groupedNodeTypes': {
                this.groupedNodeTypes = entityTypeJSON;
                this.entityTypes.groupedNodeTypes = this.groupedNodeTypes;
                break;
            }
            case 'relationshipTypes': {
                for (const relationshipType of entityTypeJSON) {
                    // get relationship type visualappearances
                    let visualAppearance;
                    this.backendService
                    // returns Observable
                        .requestRelationshipTypeVisualappearance(
                            relationshipType.namespace,
                            relationshipType.id)
                        .subscribe((JSON) => {
                            // console.log(JSON);
                            visualAppearance = JSON;
                            this.relationshipTypes
                                .push(new EntityType(
                                    relationshipType.id,
                                    relationshipType.qName,
                                    relationshipType.name,
                                    relationshipType.namespace,
                                    visualAppearance.color
                                ));
                        });
                }
                this.entityTypes.relationshipTypes = this.relationshipTypes;
                break;
            }
        }

    }

    initTopologyTemplate (nodeTemplateArray: Array<any>, visuals: any, relationshipTemplateArray: Array<any>) {
        // init node templates
        for (const node of nodeTemplateArray) {
            let color;
            let imageUrl;
            for (const visual of visuals) {
                const qName = new QName(visual.nodeTypeId);
                const localName = qName.localName;
                if (localName === new QName(node.type).localName) {
                    color = visual.color;
                    imageUrl = visual.imageUrl;
                    if (imageUrl) {
                        imageUrl = imageUrl.replace('appearance', 'visualappearance');
                    }
                }
            }
            let properties;
            if (node.properties) {
                if (node.properties.any) {
                    properties = node.properties.any;
                } else if (node.properties.kvproperties) {
                    properties = node.properties.kvproperties;
                }
            }
            this.nodeTemplates.push(
                new TNodeTemplate(
                    properties,
                    node.id,
                    node.type,
                    node.name,
                    node.minInstances,
                    node.maxInstances,
                    color,
                    imageUrl,
                    node.documentation,
                    node.any,
                    node.otherAttributes,
                    node.x,
                    node.y,
                    node.capabilities,
                    node.requirements,
                    node.deploymentArtifacts,
                    node.policies,
                    node.targetLocations
                )
            );
        }
        for (const nodeTemplate of this.nodeTemplates) {
            this.ngRedux.dispatch(this.actions.saveNodeTemplate(nodeTemplate));
        }
        // init relationship templates
        for (const relationship of relationshipTemplateArray) {
            const relationshipType = new QName(relationship.type).localName;
            this.relationshipTemplates.push(
                new TRelationshipTemplate(
                    relationship.sourceElement,
                    relationship.targetElement,
                    relationship.name,
                    `${relationship.sourceElement.ref}_${relationshipType}_${relationship.targetElement.ref}`,
                    relationshipType,
                    relationship.documentation,
                    relationship.any,
                    relationship.otherAttributes
                )
            );
        }
        for (const relationshipTemplate of this.relationshipTemplates) {
            this.ngRedux.dispatch(this.actions.saveRelationship(relationshipTemplate));
        }
    }
}


