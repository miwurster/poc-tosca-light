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

import { Injectable } from '@angular/core';
import { Headers, Http, RequestOptions } from '@angular/http';
import { Observable } from 'rxjs/Rx';
import { ActivatedRoute } from '@angular/router';
import { isNullOrUndefined } from 'util';
import { backendBaseURL } from './configuration';
import { Subject } from 'rxjs/Subject';

/**
 * Responsible for interchanging data between the app and the server.
 */
@Injectable()
export class BackendService {
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

    private groupedNodeTypes = new Subject<any>();
    groupedNodeTypes$ = this.groupedNodeTypes.asObservable();

    private relationshipTypes = new Subject<any>();
    relationshipTypes$ = this.relationshipTypes.asObservable();

    private topologyTemplateAndVisuals = new Subject<any>();
    topologyTemplateAndVisuals$ = this.topologyTemplateAndVisuals.asObservable();

    constructor(private http: Http,
                private activatedRoute: ActivatedRoute) {

        this.activatedRoute.queryParams.subscribe((params: TopologyModelerConfiguration) => {
            // Only works if url parameters are legit
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
                // Policy Types
                this.requestPolicyTypes().subscribe(data => {
                    // add JSON to Promise, WineryComponent will subscribe to its Observable
                    this.policyTypes.next(data);
                });
                // Policy Templates
                this.requestPolicyTemplates().subscribe(data => {
                    // add JSON to Promise, WineryComponent will subscribe to its Observable
                    this.policyTemplates.next(data);
                });
                // Capability Types
                this.requestCapabilityTypes().subscribe(data => {
                    // add JSON to Promise, WineryComponent will subscribe to its Observable
                    this.capabilityTypes.next(data);
                });
                // Requirement Types
                this.requestRequirementTypes().subscribe(data => {
                    // add JSON to Promise, WineryComponent will subscribe to its Observable
                    this.requirementTypes.next(data);
                });
                // Artifact Types
                this.requestArtifactTypes().subscribe(data => {
                    // add JSON to Promise, WineryComponent will subscribe to its Observable
                    this.artifactTypes.next(data);
                });
                // Grouped NodeTypes Types
                this.requestGroupedNodeTypes().subscribe(data => {
                    // add JSON to Promise, WineryComponent will subscribe to its Observable
                    this.groupedNodeTypes.next(data);
                });
                // Relationship Types
                this.requestRelationshipTypes().subscribe(data => {
                    // add JSON to Promise, WineryComponent will subscribe to its Observable
                    this.relationshipTypes.next(data);
                });
                // TopologyTemplate and Visuals together
                this.requestTopologyTemplateAndVisuals().subscribe(data => {
                    this.topologyTemplateAndVisuals.next(data);
                });
            } else {
                // empty if url params not specified
                this.serviceTemplate.next({});
                // Policy Types
                this.requestPolicyTypes().subscribe(data => {
                    // add JSON to Promise, WineryComponent will subscribe to its Observable
                    this.policyTypes.next(data);
                });
                // Policy Templates
                this.requestPolicyTemplates().subscribe(data => {
                    // add JSON to Promise, WineryComponent will subscribe to its Observable
                    this.policyTemplates.next(data);
                });
                // Capability Types
                this.requestCapabilityTypes().subscribe(data => {
                    // add JSON to Promise, WineryComponent will subscribe to its Observable
                    this.capabilityTypes.next(data);
                });
                // Requirement Types
                this.requestRequirementTypes().subscribe(data => {
                    // add JSON to Promise, WineryComponent will subscribe to its Observable
                    this.requirementTypes.next(data);
                });
                // Artifact Types
                this.requestArtifactTypes().subscribe(data => {
                    // add JSON to Promise, WineryComponent will subscribe to its Observable
                    this.artifactTypes.next(data);
                });
                // Grouped NodeTypes Types
                this.requestGroupedNodeTypes().subscribe(data => {
                    // add JSON to Promise, WineryComponent will subscribe to its Observable
                    this.groupedNodeTypes.next(data);
                });
                // Relationship Types
                this.requestRelationshipTypes().subscribe(data => {
                    // add JSON to Promise, WineryComponent will subscribe to its Observable
                    this.relationshipTypes.next(data);
                });
            }
        });
    }

    /**
     * Requests topologyTemplate and visualappearances together.
     * We use Observable.forkJoin to await both responses from the backend.
     * This is required
     * @returns data  The JSON from the server
     */
    requestTopologyTemplateAndVisuals(): Observable<[string, string]> {
        if (isNullOrUndefined(this.configuration)) {
            setTimeout(null, 100);
        }
        const headers = new Headers({'Accept': 'application/json'});
        const options = new RequestOptions({headers: headers});
        const url = this.configuration.repositoryURL + '/servicetemplates/'
            + encodeURIComponent(encodeURIComponent(this.configuration.ns)) + '/'
            + this.configuration.id + '/topologytemplate/';

        return Observable.forkJoin(
            this.http.get(url, options)
                .map(res => res.json()),
            this.http.get(backendBaseURL + '/nodetypes/allvisualappearancedata', options)
                .map(res => res.json())
        );
    }

    /**
     * Requests data from the server
     * @returns data  The JSON from the server
     */
    requestServiceTemplate(): Observable<string> {
        if (isNullOrUndefined(this.configuration)) {
            setTimeout(null, 100);
        }
        const headers = new Headers({'Accept': 'application/json'});
        const options = new RequestOptions({headers: headers});
        const url = this.configuration.repositoryURL + '/servicetemplates/'
            + encodeURIComponent(encodeURIComponent(this.configuration.ns)) + '/'
            + this.configuration.id + '/topologytemplate/';

        return this.http.get(url, options).map(res => res.json());
    }

    /**
     * Returns data that is later used by jsPlumb to render a relationship connector
     * @returns data The JSON from the server
     */
    requestRelationshipTypeVisualappearance(namespace: string, id: string): Observable<string> {
        if (isNullOrUndefined(this.configuration)) {
            setTimeout(null, 100);
        }
        const headers = new Headers({'Accept': 'application/json'});
        const options = new RequestOptions({headers: headers});
        const url = this.configuration.repositoryURL + '/relationshiptypes/'
            + encodeURIComponent(encodeURIComponent(namespace)) + '/'
            + id + '/visualappearance/';

        return this.http.get(url, options).map(res => res.json());
    }

    /**
     * Requests all visual appearances used for the NodeTemplates
     * @returns {Observable<string>}
     */
    requestAllNodeTemplateVisuals(): Observable<string> {
        if (isNullOrUndefined(this.configuration)) {
            setTimeout(null, 100);
        }
        const headers = new Headers({'Accept': 'application/json'});
        const options = new RequestOptions({headers: headers});
        return this.http.get(backendBaseURL + '/nodetypes/allvisualappearancedata', options)
            .map(res => res.json());
    }

    /**
     * Requests all policy types from the backend
     * @returns {Observable<string>}
     */
    requestPolicyTypes(): Observable<string> {
        if (isNullOrUndefined(this.configuration)) {
            setTimeout(null, 100);
        }
        const headers = new Headers({'Accept': 'application/json'});
        const options = new RequestOptions({headers: headers});
        return this.http.get(backendBaseURL + '/policytypes', options)
            .map(res => {
                return res.json();
            });
    }

    /**
     * Requests all requirement types from the backend
     * @returns {Observable<string>}
     */
    requestRequirementTypes(): Observable<string> {
        if (isNullOrUndefined(this.configuration)) {
            setTimeout(null, 100);
        }
        const headers = new Headers({'Accept': 'application/json'});
        const options = new RequestOptions({headers: headers});
        return this.http.get(backendBaseURL + '/requirementtypes', options)
            .map(res => {
                return res.json();
            });
    }

    /**
     * Requests all capability types from the backend
     * @returns {Observable<string>}
     */
    requestCapabilityTypes(): Observable<string> {
        if (isNullOrUndefined(this.configuration)) {
            setTimeout(null, 100);
        }
        const headers = new Headers({'Accept': 'application/json'});
        const options = new RequestOptions({headers: headers});
        return this.http.get(backendBaseURL + '/capabilitytypes', options)
            .map(res => {
                return res.json();
            });
    }

    /**
     * Requests all capability types from the backend
     * @returns {Observable<string>}
     */
    requestGroupedNodeTypes(): Observable<string> {
        if (isNullOrUndefined(this.configuration)) {
            setTimeout(null, 100);
        }
        const headers = new Headers({'Accept': 'application/json'});
        const options = new RequestOptions({headers: headers});
        return this.http.get(backendBaseURL + '/nodetypes?grouped&full', options)
            .map(res => {
                return res.json();
            });
    }

    /**
     * Requests all policy templates from the backend
     * @returns {Observable<string>}
     */
    requestPolicyTemplates(): Observable<string> {
        if (isNullOrUndefined(this.configuration)) {
            setTimeout(null, 100);
        }
        const headers = new Headers({'Accept': 'application/json'});
        const options = new RequestOptions({headers: headers});
        return this.http.get(backendBaseURL + '/policytemplates', options)
            .map(res => {
                return res.json();
            });
    }

    /**
     * Requests all artifact types from the backend
     * @returns {Observable<string>}
     */
    requestArtifactTypes(): Observable<string> {
        if (isNullOrUndefined(this.configuration)) {
            setTimeout(null, 100);
        }
        const headers = new Headers({'Accept': 'application/json'});
        const options = new RequestOptions({headers: headers});
        return this.http.get(backendBaseURL + '/artifacttypes', options)
            .map(res => {
                return res.json();
            });
    }

    /**
     * Requests all relationship types from the backend
     * @returns {Observable<string>}
     */
    requestRelationshipTypes(): Observable<string> {
        if (isNullOrUndefined(this.configuration)) {
            setTimeout(null, 100);
        }
        const headers = new Headers({'Accept': 'application/json'});
        const options = new RequestOptions({headers: headers});
        return this.http.get(backendBaseURL + '/relationshiptypes', options)
            .map(res => {
                return res.json();
            });
    }

    /**
     * Saves the topologyTemplate back to the repository
     * @returns {Observable<Response>}
     */
    saveTopologyTemplate(topologyTemplate: any): Observable<any> {
        if (isNullOrUndefined(this.configuration)) {
            setTimeout(null, 100);
        }
        const headers = new Headers({ 'Content-Type': 'application/json' });
        const options = new RequestOptions({ headers: headers });
        const url = this.configuration.repositoryURL + '/servicetemplates/'
            + encodeURIComponent(encodeURIComponent(this.configuration.ns)) + '/'
            + this.configuration.id + '/topologytemplate/';
        return this.http.put(url, JSON.stringify(topologyTemplate), options);
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
