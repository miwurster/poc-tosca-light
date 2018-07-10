/********************************************************************************
 * Copyright(c) 2018 Contributors to the Eclipse Foundation
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
import { backendBaseURL, hostURL } from '../models/configuration';
import { Subject } from 'rxjs/Subject';
import { isNullOrUndefined } from 'util';
import { EntityType, TTopologyTemplate, Visuals } from '../models/ttopology-template';
import { QNameWithTypeApiData } from '../models/generateArtifactApiData';
import { HttpClient, HttpHeaders, HttpResponse } from '@angular/common/http';
import { urlElement } from '../models/enums';
import { ToscaDiff } from '../models/ToscaDiff';
import { ToastrService } from 'ngx-toastr';
import { Observable } from 'rxjs/Observable';
import { forkJoin } from 'rxjs';
import { map } from 'rxjs/internal/operators';

/**
 * Responsible for interchanging data between the app and the server.
 */
@Injectable()
export class BackendService {

    readonly headers = new HttpHeaders().set('Accept', 'application/json');

    configuration: TopologyModelerConfiguration;
    topologyTemplateURL: string;
    serviceTemplateUiUrl: string;

    endpointConfiguration = new Subject<any>();
    endpointConfiguration$ = this.endpointConfiguration.asObservable();

    private allEntities = new Subject<any>();
    allEntities$ = this.allEntities.asObservable();

    constructor(private http: HttpClient,
                private alert: ToastrService) {
        this.endpointConfiguration$.subscribe((params: TopologyModelerConfiguration) => {
            if (!(isNullOrUndefined(params.id) && isNullOrUndefined(params.ns) &&
                isNullOrUndefined(params.repositoryURL) && isNullOrUndefined(params.uiURL))) {

                this.configuration = new TopologyModelerConfiguration(params.id,params.ns, params.repositoryURL, params.uiURL,
                    params.compareTo, params.compareTo ? true : params.isReadonly);

                const url = 'servicetemplates/'
                    + encodeURIComponent(encodeURIComponent(this.configuration.ns)) + '/'
                    + this.configuration.id;
                this.topologyTemplateURL = this.configuration.repositoryURL + '/' + url;
                this.serviceTemplateUiUrl = this.configuration.uiURL + url;

                // All Entity types
                this.requestAllEntitiesAtOnce().subscribe(data => {
                    // add JSON to Promise, WineryComponent will subscribe to its Observable
                    this.allEntities.next(data);
                });
            }
        });
    }

    /**
     * Requests all entities together.
     * We use forkJoin() to await all responses from the backend.
     * This is required
     * @returns data  The JSON from the server
     */
    private requestAllEntitiesAtOnce(): Observable<any> {
        if (this.configuration) {
            return forkJoin(
                this.requestGroupedNodeTypes(),
                this.requestArtifactTemplates(),
                this.requestTopologyTemplateAndVisuals(),
                this.requestArtifactTypes(),
                this.requestPolicyTypes(),
                this.requestCapabilityTypes(),
                this.requestRequirementTypes(),
                this.requestPolicyTemplates(),
                this.requestRelationshipTypes(),
                this.requestNodeTypes()
            );
        }
    }

    /**
     * Requests topologyTemplate and visualappearances together. If the topology should be compared, it also gets
     * the old topology as well as the diff representation.
     * We use Observable.forkJoin to await all responses from the backend.
     * This is required
     * @returns data  The JSON from the server
     */
    private requestTopologyTemplateAndVisuals(): Observable<any> {
        if (this.configuration) {
            const url = this.configuration.repositoryURL + '/servicetemplates/'
                + encodeURIComponent(encodeURIComponent(this.configuration.ns)) + '/';
            const currentUrl = url + this.configuration.id + '/topologytemplate/';
            const visualsUrl = backendBaseURL + '/nodetypes/allvisualappearancedata';
            // This is required because the information has to be returned together

            if (isNullOrUndefined(this.configuration.compareTo)) {
                return forkJoin(
                    this.http.get<TTopologyTemplate>(currentUrl),
                    this.http.get<Visuals>(visualsUrl)
                );
            } else {
                const compareUrl = url
                    + this.configuration.id + '/?compareTo='
                    + this.configuration.compareTo;
                const templateUrl = url
                    + this.configuration.compareTo + '/topologytemplate';

                return forkJoin(
                    this.http.get<TTopologyTemplate>(currentUrl),
                    this.http.get<Visuals>(visualsUrl),
                    this.http.get<ToscaDiff>(compareUrl),
                    this.http.get<TTopologyTemplate>(templateUrl)
                );
            }
        }
    }

    /**
     * Returns data that is later used by jsPlumb to render a relationship connector
     * @returns data The JSON from the server
     */
    requestRelationshipTypeVisualappearance(namespace: string, id: string): Observable<EntityType> {
        if (this.configuration) {
            const url = this.configuration.repositoryURL + '/relationshiptypes/'
                + encodeURIComponent(encodeURIComponent(namespace)) + '/'
                + id + '/visualappearance/';
            return this.http
                .get<EntityType>(url, { headers: this.headers })
                .pipe(
                    map(relationship => {
                        if (!isNullOrUndefined(this.configuration.compareTo)) {
                            relationship.color = 'grey';
                        }
                        return relationship;
                    })
                );
        }
    }

    /**
     * Requests all policy types from the backend
     * @returns {Observable<string>}
     */
    private requestPolicyTypes(): Observable<any> {
        if (this.configuration) {
            return this.http.get(backendBaseURL + '/policytypes?full', { headers: this.headers });
        }
    }

    /**
     * Requests all requirement types from the backend
     * @returns {Observable<string>}
     */
    private requestRequirementTypes(): Observable<any> {
        if (this.configuration) {
            return this.http.get(backendBaseURL + '/requirementtypes?full', { headers: this.headers });
        }
    }

    /**
     * Requests all capability types from the backend
     * @returns {Observable<string>}
     */
    private requestCapabilityTypes(): Observable<any> {
        if (this.configuration) {
            return this.http.get(backendBaseURL + '/capabilitytypes?full', { headers: this.headers });
        }
    }

    /**
     * Requests all grouped node types from the backend
     * @returns {Observable<string>}
     */
    private requestGroupedNodeTypes(): Observable<any> {
        if (this.configuration) {
            return this.http.get(backendBaseURL + '/nodetypes?grouped&full', { headers: this.headers });
        }
    }

    /**
     * Requests all ungrouped node types from the backend
     * @returns {Observable<string>}
     */
    private requestNodeTypes(): Observable<any> {
        if (this.configuration) {
            return this.http.get(backendBaseURL + '/nodetypes?full', { headers: this.headers });
        }
    }

    /**
     * Requests all policy templates from the backend
     * @returns {Observable<string>}
     */
    private requestPolicyTemplates(): Observable<any> {
        if (this.configuration) {
            return this.http.get(backendBaseURL + '/policytemplates', { headers: this.headers });
        }
    }

    /**
     * Requests all artifact types from the backend
     * @returns {Observable<string>}
     */
    private requestArtifactTypes(): Observable<any> {
        if (this.configuration) {
            return this.http.get(backendBaseURL + '/artifacttypes', { headers: this.headers });
        }
    }

    /**
     * Requests all artifact templates from the backend
     * @returns {Observable<string>}
     */
    requestArtifactTemplates(): Observable<any> {
        if (this.configuration) {
            return this.http.get(backendBaseURL + '/artifacttemplates', { headers: this.headers });
        }
    }

    /**
     * Requests all relationship types from the backend
     * @returns {Observable<string>}
     */
    private requestRelationshipTypes(): Observable<any> {
        if (this.configuration) {
            return this.http.get(backendBaseURL + '/relationshiptypes', { headers: this.headers });
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
            return this.http.get(URL, { headers: this.headers });
        }
    }

    /**
     * This method retrieves a single Artifact Template from the backend.
     * @param {QNameWithTypeApiData} artifact
     * @returns {Observable<any>}
     */
    requestArtifactTemplate(artifact: QNameWithTypeApiData): Observable<any> {
        const url = this.configuration.repositoryURL + '/artifacttemplates/'
            + encodeURIComponent(encodeURIComponent(artifact.namespace)) + '/' + artifact.localname;
        return this.http.get(url + '/', { headers: this.headers });
    }

    /**
     * This method retrieves a single Policy Template from the backend.
     * @param {QNameWithTypeApiData} artifact
     * @returns {Observable<any>}
     */
    requestPolicyTemplate(artifact: QNameWithTypeApiData): Observable<any> {
        const url = this.configuration.repositoryURL + '/policytemplates/'
            + encodeURIComponent(encodeURIComponent(artifact.namespace)) + '/' + artifact.localname;
        return this.http.get(url + '/', { headers: this.headers });
    }

    /**
     * Saves the topologyTemplate back to the repository
     * @returns {Observable<Response>}
     */
    saveTopologyTemplate(topologyTemplate: any): Observable<HttpResponse<string>> {
        if (this.configuration) {
            const headers = new HttpHeaders().set('Content-Type', 'application/json');
            const url = this.configuration.repositoryURL + '/servicetemplates/'
                + encodeURIComponent(encodeURIComponent(this.configuration.ns)) + '/'
                + this.configuration.id + '/topologytemplate/';

            return this.http.put(url, topologyTemplate, {
                headers: headers, responseType: 'text', observe: 'response'
            });
        }
    }

    /**
     * Imports the template.
     * @returns {Observable<any>}
     */
    importTopology(importedTemplateQName: string): Observable<HttpResponse<string>> {
        const headers = new HttpHeaders().set('Content-Type', 'text/plain');
        const url = this.topologyTemplateURL + urlElement.TopologyTemplate + 'merge';
        return this.http.post(url + '/', importedTemplateQName, {
            headers: headers,
            observe: 'response',
            responseType: 'text'
        });
    }

    /**
     * Splits the template.
     * @returns {Observable<any>}
     */
    splitTopology(): Observable<HttpResponse<string>> {
        const headers = new HttpHeaders().set('Content-Type', 'application/json');
        const url = this.topologyTemplateURL + urlElement.TopologyTemplate + 'split';
        return this.http.post(url + '/', {}, { headers: headers, observe: 'response', responseType: 'text' });
    }

    /**
     * Matches the template.
     * @returns {Observable<any>}
     */
    matchTopology(): Observable<HttpResponse<string>> {
        const headers = new HttpHeaders().set('Content-Type', 'application/json');
        const url = this.topologyTemplateURL + urlElement.TopologyTemplate + 'match';
        return this.http.post(url + '/', {}, { headers: headers, observe: 'response', responseType: 'text' });
    }

    /**
     * Used for creating new artifact templates on the backend.
     * @param {QNameWithTypeApiData} artifact
     * @returns {Observable<any>}
     */
    createNewArtifact(artifact: QNameWithTypeApiData): Observable<HttpResponse<string>> {
        const headers = new HttpHeaders().set('Content-Type', 'application/json');
        const url = this.configuration.repositoryURL + '/artifacttemplates/';
        return this.http.post(url + '/', artifact, { headers: headers, responseType: 'text', observe: 'response' });
    }

    /**
     * Requests all topology template ids
     * @returns {Observable<string>}
     */
    requestAllTopologyTemplates(): Observable<EntityType[]> {
        const url = hostURL + urlElement.Winery + urlElement.ServiceTemplates;
        return this.http.get<EntityType[]>(url + '/', { headers: this.headers });
    }
}

/**
 * Defines config of TopologyModeler.
 */
export class TopologyModelerConfiguration {
    constructor(public readonly id: string,
                public readonly ns: string,
                public readonly repositoryURL: string,
                public readonly uiURL: string,
                public readonly compareTo?: string,
                public readonly isReadonly?: boolean) {

    }
}
