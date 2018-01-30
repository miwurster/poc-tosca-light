/********************************************************************************
 * Copyright(c) 2017 Contributors to the Eclipse Foundation
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

import { Injectable } from '@angular/core';
import { Headers, Http, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';
import 'rxjs/add/operator/catch';
import { ActivatedRoute } from '@angular/router';
import { backendBaseURL, hostURL } from './configuration';
import { Subject } from 'rxjs/Subject';
import { isNullOrUndefined } from 'util';
import { TTopologyTemplate, Visuals } from './models/ttopology-template';
import { GenerateArtifactApiData } from './generateArtifactApiData';

/**
 * Responsible for interchanging data between the app and the server.
 */
@Injectable()
export class BackendService {
    readonly headers = new Headers({ 'Accept': 'application/json' });
    readonly options = new RequestOptions({ headers: this.headers });

    entityLoaded = {
        topologyTemplateAndVisuals: false,
        artifactTypes: false,
        artifactTemplates: false,
        policyTypes: false,
        policyTemplates: false,
        capabilityTypes: false,
        requirementTypes: false,
        groupedNodeTypes: false,
        ungroupedNodeTypes: false,
        relationshipTypes: false,
    };
    allEntitiesLoaded = false;

    configuration: TopologyModelerConfiguration;

    private serviceTemplate = new Subject<any>();
    serviceTemplate$ = this.serviceTemplate.asObservable();

    private visuals = new Subject<any>();
    visuals$ = this.visuals.asObservable();

    private policyTypes = new Subject<any>();
    policyTypes$ = this.policyTypes.asObservable();

    private policyTemplates = new Subject<any>();
    policyTemplates$ = this.policyTemplates.asObservable();

    private capabilityTypes = new Subject<any>();
    capabilityTypes$ = this.capabilityTypes.asObservable();

    private requirementTypes = new Subject<any>();
    requirementTypes$ = this.requirementTypes.asObservable();

    private artifactTypes = new Subject<any>();
    artifactTypes$ = this.artifactTypes.asObservable();

    private artifactTemplates = new Subject<any>();
    artifactTemplates$ = this.artifactTemplates.asObservable();

    private groupedNodeTypes = new Subject<any>();
    groupedNodeTypes$ = this.groupedNodeTypes.asObservable();

    private nodeTypes = new Subject<any>();
    nodeTypes$ = this.nodeTypes.asObservable();

    private relationshipTypes = new Subject<any>();
    relationshipTypes$ = this.relationshipTypes.asObservable();

    private topologyTemplateAndVisuals = new Subject<[TTopologyTemplate, Visuals]>();
    topologyTemplateAndVisuals$ = this.topologyTemplateAndVisuals.asObservable();

    constructor(private http: Http,
                private activatedRoute: ActivatedRoute) {

        this.activatedRoute.queryParams.subscribe((params: TopologyModelerConfiguration) => {
            if (!(isNullOrUndefined(params.id) &&
                    isNullOrUndefined(params.ns) &&
                    isNullOrUndefined(params.repositoryURL) &&
                    isNullOrUndefined(params.uiURL))) {
                this.configuration = params;
                // ServiceTemplate / TopologyTemplate
                this.requestServiceTemplate().subscribe(data => {
                    // add JSON to Promise, WineryComponent will subscribe to its Observable
                    this.serviceTemplate.next(data);
                });
                // NodeType Visuals
                this.requestAllNodeTemplateVisuals().subscribe(data => {
                    // add JSON to Promise, WineryComponent will subscribe to its Observable
                    this.visuals.next(data);
                });
                // TopologyTemplate and Visuals together
                this.requestTopologyTemplateAndVisuals().subscribe(data => {
                    this.entityLoaded.topologyTemplateAndVisuals = true;
                    this.topologyTemplateAndVisuals.next(data);
                });
                // Policy Types
                this.requestPolicyTypes().subscribe(data => {
                    // add JSON to Promise, WineryComponent will subscribe to its Observable
                    this.entityLoaded.policyTypes = true;
                    this.policyTypes.next(data);
                });
                // Policy Templates
                this.requestPolicyTemplates().subscribe(data => {
                    // add JSON to Promise, WineryComponent will subscribe to its Observable
                    this.entityLoaded.policyTemplates = true;
                    this.policyTemplates.next(data);
                });
                // Capability Types
                this.requestCapabilityTypes().subscribe(data => {
                    // add JSON to Promise, WineryComponent will subscribe to its Observable
                    this.entityLoaded.capabilityTypes = true;
                    this.capabilityTypes.next(data);
                });
                // Requirement Types
                this.requestRequirementTypes().subscribe(data => {
                    // add JSON to Promise, WineryComponent will subscribe to its Observable
                    this.entityLoaded.requirementTypes = true;
                    this.requirementTypes.next(data);
                });
                // Artifact Types
                this.requestArtifactTypes().subscribe(data => {
                    // add JSON to Promise, WineryComponent will subscribe to its Observable
                    this.entityLoaded.artifactTypes = true;
                    this.artifactTypes.next(data);
                });
                // Artifact Templates
                this.requestArtifactTemplates().subscribe(data => {
                    // add JSON to Promise, WineryComponent will subscribe to its Observable
                    this.entityLoaded.artifactTemplates = true;
                    this.artifactTemplates.next(data);
                });
                // Grouped NodeTypes
                this.requestGroupedNodeTypes().subscribe(data => {
                    // add JSON to Promise, WineryComponent will subscribe to its Observable
                    this.entityLoaded.groupedNodeTypes = true;
                    this.groupedNodeTypes.next(data);
                });
                // NodeTypes
                this.requestNodeTypes().subscribe(data => {
                    // add JSON to Promise, WineryComponent will subscribe to its Observable
                    this.entityLoaded.ungroupedNodeTypes = true;
                    this.nodeTypes.next(data);
                });
                // Relationship Types
                this.requestRelationshipTypes().subscribe(data => {
                    this.entityLoaded.relationshipTypes = true;
                    // add JSON to Promise, WineryComponent will subscribe to its Observable
                    this.relationshipTypes.next(data);
                });
            } else {
                // TODO: how does it have to behave when no params are specified?
            }
        });

        this.everythingLoaded().then(() => {
            console.log('all data arrived');

            console.log(this.entityLoaded);
            // TODO: fire actual event here
        });

    }

    everythingLoaded() {
        return new Promise((resolve) => {
            if (this.entityLoaded.topologyTemplateAndVisuals &&
                this.entityLoaded.artifactTypes &&
                this.entityLoaded.artifactTemplates &&
                this.entityLoaded.policyTypes &&
                this.entityLoaded.policyTemplates &&
                this.entityLoaded.capabilityTypes &&
                this.entityLoaded.requirementTypes &&
                this.entityLoaded.groupedNodeTypes &&
                this.entityLoaded.ungroupedNodeTypes &&
                this.entityLoaded.relationshipTypes) {
                resolve(true);
            } else {
                resolve(false);
            }
        });
    }

    /**
     * Requests topologyTemplate and visualappearances together.
     * We use Observable.forkJoin to await both responses from the backend.
     * This is required
     * @returns data  The JSON from the server
     */
    requestTopologyTemplateAndVisuals(): Observable<[TTopologyTemplate, Visuals]> {
        if (this.configuration) {
            const url = this.configuration.repositoryURL + '/servicetemplates/'
                + encodeURIComponent(encodeURIComponent(this.configuration.ns)) + '/'
                + this.configuration.id + '/topologytemplate/';
            // This is required because the information has to be returned together
            return Observable.forkJoin(
                this.http.get(url, this.options)
                    .map(res => res.json()),
                this.http.get(backendBaseURL + '/nodetypes/allvisualappearancedata', this.options)
                    .map(res => res.json())
            );
        }
    }

    /**
     * Requests data from the server
     * @returns data  The JSON from the server
     */
    requestServiceTemplate(): Observable<Object> {
        if (this.configuration) {
            const url = this.configuration.repositoryURL + '/servicetemplates/'
                + encodeURIComponent(encodeURIComponent(this.configuration.ns)) + '/'
                + this.configuration.id + '/topologytemplate/';
            return this.http.get(url, this.options).map(res => res.json());
        }
    }

    /**
     * Returns data that is later used by jsPlumb to render a relationship connector
     * @returns data The JSON from the server
     */
    requestRelationshipTypeVisualappearance(namespace: string, id: string): Observable<any> {
        if (this.configuration) {
            const url = this.configuration.repositoryURL + '/relationshiptypes/'
                + encodeURIComponent(encodeURIComponent(namespace)) + '/'
                + id + '/visualappearance/';
            return this.http.get(url, this.options).map(res => res.json());
        }
    }

    /**
     * Requests all visual appearances used for the NodeTemplates
     * @returns {Observable<string>}
     */
    requestAllNodeTemplateVisuals(): Observable<any> {
        if (this.configuration) {
            return this.http.get(backendBaseURL + '/nodetypes/allvisualappearancedata', this.options)
                .map(res => res.json());
        }
    }

    /**
     * Requests all policy types from the backend
     * @returns {Observable<string>}
     */
    requestPolicyTypes(): Observable<any> {
        if (this.configuration) {
            return this.http.get(backendBaseURL + '/policytypes?full', this.options)
                .map(res => res.json());
        }
    }

    /**
     * Requests all requirement types from the backend
     * @returns {Observable<string>}
     */
    requestRequirementTypes(): Observable<any> {
        if (this.configuration) {
            return this.http.get(backendBaseURL + '/requirementtypes?full', this.options)
                .map(res => res.json());
        }
    }

    /**
     * Requests all capability types from the backend
     * @returns {Observable<string>}
     */
    requestCapabilityTypes(): Observable<any> {
        if (this.configuration) {
            return this.http.get(backendBaseURL + '/capabilitytypes?full', this.options)
                .map(res => res.json());
        }
    }

    /**
     * Requests all grouped node types from the backend
     * @returns {Observable<string>}
     */
    requestGroupedNodeTypes(): Observable<any> {
        if (this.configuration) {
            return this.http.get(backendBaseURL + '/nodetypes?grouped&full', this.options)
                .map(res => res.json());
        }
    }

    /**
     * Requests all ungrouped node types from the backend
     * @returns {Observable<string>}
     */
    requestNodeTypes(): Observable<any> {
        if (this.configuration) {
            return this.http.get(backendBaseURL + '/nodetypes?full', this.options)
                .map(res => res.json());
        }
    }

    /**
     * Requests all policy templates from the backend
     * @returns {Observable<string>}
     */
    requestPolicyTemplates(): Observable<any> {
        if (this.configuration) {
            return this.http.get(backendBaseURL + '/policytemplates', this.options)
                .map(res => res.json());
        }
    }

    /**
     * Requests all artifact types from the backend
     * @returns {Observable<string>}
     */
    requestArtifactTypes(): Observable<any> {
        if (this.configuration) {
            return this.http.get(backendBaseURL + '/artifacttypes', this.options)
                .map(res => res.json());
        }
    }

    /**
     * Requests all artifact templates from the backend
     * @returns {Observable<string>}
     */
    requestArtifactTemplates(): Observable<any> {
        if (this.configuration) {
            return this.http.get(backendBaseURL + '/artifacttemplates?full', this.options)
                .map(res => res.json());
        }
    }

    /**
     * Requests all relationship types from the backend
     * @returns {Observable<string>}
     */
    requestRelationshipTypes(): Observable<any> {
        if (this.configuration) {
            return this.http.get(backendBaseURL + '/relationshiptypes', this.options)
                .map(res => res.json());
        }
    }

    /**
     * Requests all namespaces from the backend
     * @returns {Observable<any>} json of namespaces
     */
    requestNamespaces(all: boolean = false): Observable<any> {
        if (this.configuration) {
            let URL: string;
            if (all) {
                URL = backendBaseURL + '/admin/namespaces/?all';
            } else {
                URL = backendBaseURL + '/admin/namespaces/';
            }
            return this.http.get(URL, this.options)
                .map(res => res.json());
        }
    }

    /**
     * Saves the topologyTemplate back to the repository
     * @returns {Observable<Response>}
     */
    saveTopologyTemplate(topologyTemplate: any): Observable<any> {
        if (this.configuration) {
            const headers = new Headers({ 'Content-Type': 'application/json' });
            const options = new RequestOptions({ headers: headers });
            const url = this.configuration.repositoryURL + '/servicetemplates/'
                + encodeURIComponent(encodeURIComponent(this.configuration.ns)) + '/'
                + this.configuration.id + '/topologytemplate/';

            return this.http.put(url, JSON.stringify(topologyTemplate), options);
        }
    }

    /**
     * Used for creating new deployment artifacts inside node templates.
     * @param {GenerateArtifactApiData} artifact
     * @param {string} nodeTemplateId
     * @returns {Observable<any>}
     */
    createNewArtifact(artifact: GenerateArtifactApiData, nodeTemplateId: string): Observable<any> {
        const headers = new Headers({ 'Content-Type': 'application/json' });
        const options = new RequestOptions({ headers: headers });
        const url = this.configuration.repositoryURL + '/servicetemplates/'
            + encodeURIComponent(encodeURIComponent(this.configuration.ns)) + '/'
            + this.configuration.id + '/topologytemplate/' + nodeTemplateId + '/deploymentartifacts/';
        return this.http.post(url + '/', artifact, options);
    }

    /*  saveVisuals(data: any): Observable<Response> {
     const headers = new Headers({ 'Content-Type': 'application/json' });
     const options = new RequestOptions({ headers: headers });
     return this.http.put(backendBaseURL + this.activatedRoute.url + '/', JSON.stringify(data), options);
     }*/
}

/**
 * Defines config of TopologyModeler.
 */
export class TopologyModelerConfiguration {
    readonly id: string;
    readonly ns: string;
    readonly repositoryURL: string;
    readonly uiURL: string;
}
