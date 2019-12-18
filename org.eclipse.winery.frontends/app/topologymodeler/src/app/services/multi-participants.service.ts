import { Injectable } from '@angular/core';
import { TopologyModelerConfiguration } from '../models/topologyModelerConfiguration';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { BackendService } from './backend.service';
import { Observable } from 'rxjs';

@Injectable({
    providedIn: 'root',
})
export class MultiParticipantsService {

    private readonly configuration: TopologyModelerConfiguration;
    private readonly httpHeaders: HttpHeaders;

    constructor(private httpClient: HttpClient,
                private backendService: BackendService) {
        this.configuration = backendService.configuration;
        this.httpHeaders = new HttpHeaders().set('Content-Type', 'application/json');
    }

    /**
     * Create a new version of the current service template
     *
     * @param endpoint: respective endpoint for placeholder version, participant version, ...
     */
    postNewVersion(endpoint: string): Observable<any> {
        const url = this.configuration.repositoryURL
            + '/servicetemplates/'
            + encodeURIComponent(encodeURIComponent(this.configuration.ns))
            + '/'
            + encodeURIComponent(this.configuration.id)
            + '/'
            + endpoint;

        return this.httpClient.post(
            url,
            { headers: this.httpHeaders, observe: 'response', responseType: 'json' }
        );
    }

    /**
     * Create placeholder for participants.
     *
     * @param serviceTemplateId: id of service template where placeholders are generated
     */
    postPlaceholders(serviceTemplateId: string): Observable<any> {
        const url = this.configuration.repositoryURL
            + '/servicetemplates/'
            + encodeURIComponent(encodeURIComponent(this.configuration.ns))
            + '/'
            + encodeURIComponent(serviceTemplateId)
            + '/placeholder/generator';

        return this.httpClient.post(
            url,
            { headers: this.httpHeaders, observe: 'response' });
    }
}
