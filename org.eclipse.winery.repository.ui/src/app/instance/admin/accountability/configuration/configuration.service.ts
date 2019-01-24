/********************************************************************************
 * Copyright (c) 2018-2019 Contributors to the Eclipse Foundation
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
import { HttpClient } from '@angular/common/http';
import { Configuration } from './Configuration';
import { Observable } from 'rxjs/Rx';
import { backendBaseURL } from '../../../../configuration';
import { map, mergeMap } from 'rxjs/internal/operators';
import { Injectable } from '@angular/core';
import { ConfigurationDTO } from './ConfigurationDTO';
import { parseSelectorToR3Selector } from '@angular/compiler/src/core';

/**
 * Manages accountability configurations. Configuration entries are divided into two groups: (i) a group that is managed
 * persisted by the browser's local storage, and (ii) a group persisted by the backend. A dto containing the entries of
 * the second group is exchanged with the REST API of the backend.
 */
@Injectable()
export class ConfigurationService {
    private accountabilityUrl = backendBaseURL + '/API/accountability/configuration';
    private deployProvenanceSCUrl = this.accountabilityUrl + '/provenanceSC';
    private deployAuthorizationSCUrl = this.accountabilityUrl + '/authorizationSC';
    private deployPermissionsSCUrl = this.accountabilityUrl + '/permissionsSC';
    private createKeystoreUrl = this.accountabilityUrl + '/createKeystore';
    private readonly enableAccountabilityCheckDefaultValue = false;
    private readonly enableAccountabilityCheckKey = 'AccountabilityCheck';

    constructor(private http: HttpClient) {
    }

    isAccountablilityCheckEnabled(): boolean {
        const valueString = localStorage.getItem(this.enableAccountabilityCheckKey);

        if (valueString === null || valueString === undefined) {
            return this.enableAccountabilityCheckDefaultValue;
        }

        return valueString.toLowerCase() === 'true';
    }

    setAccountabilityCheckEnabled(isEnabled: boolean) {
        localStorage.setItem(this.enableAccountabilityCheckKey, isEnabled.toString());
    }

    loadConfiguration(): Observable<Configuration> {

        return this.http.get<ConfigurationDTO>(this.accountabilityUrl).pipe(
            map(configuration => {
                // create configuration object from received dto
                const result: Configuration = new Configuration(configuration);
                // enhance it with local configuration
                result.enableAccountability = this.isAccountablilityCheckEnabled();
                return result;
            })
        );
    }

    saveConfiguration(keyFile: File, config: Configuration): Observable<Object> {
        // handle entries managed locally
        this.setAccountabilityCheckEnabled(config.enableAccountability);

        // handle entries managed by the backend
        const formData: FormData = new FormData();

        if (keyFile !== null && keyFile !== undefined) {
            formData.append('keystoreFile', keyFile, keyFile.name);
        }

        formData.append('blockhainNodeUrl', config.blockchainNodeUrl);
        // formData.append('activeKeystore', config.activeKeystore); this is set only by the backend -readonly to front
        // end-.
        formData.append('keystorePassword', config.keystorePassword);

        if (config.authorizationSmartContractAddress == null) {
            formData.append('authorizationSmartContractAddress', '');
        } else {
            formData.append('authorizationSmartContractAddress', config.authorizationSmartContractAddress);
        }

        if (config.provenanceSmartContractAddress == null) {
            formData.append('provenanceSmartContractAddress', '');
        } else {
            formData.append('provenanceSmartContractAddress', config.provenanceSmartContractAddress);
        }

        if (config.permissionsSmartContractAddress == null) {
            formData.append('permissionsSmartContractAddress', '');
        } else {
            formData.append('permissionsSmartContractAddress', config.permissionsSmartContractAddress);
        }

        formData.append('swarmGatewayUrl', config.swarmGatewayUrl);

        return this.http.put(this.accountabilityUrl, formData);
    }

    restoreDefaults(): Observable<Configuration> {
        // this.setAccountabilityCheckEnabled(this.enableAccountabilityCheckDefaultValue); do not change this option
        // when "Restore Defaults" is pressed.
        return this.http.delete<void>(this.accountabilityUrl)
            .pipe(
                mergeMap(() => this.loadConfiguration())
            );
    }

    deployProvenanceSmartContract(): Observable<string> {
        return this.http.get(this.deployProvenanceSCUrl, { responseType: 'text' });
    }

    deployAuthorizationSmartContract(): Observable<string> {
        return this.http.get(this.deployAuthorizationSCUrl, { responseType: 'text' });
    }

    deployPermissionsSmartContract(): Observable<string> {
        return this.http.get(this.deployPermissionsSCUrl, { responseType: 'text' });
    }

    createNewKeystoreFile(password: string): void {
        // handle entries managed by the backend
        window.open(this.createKeystoreUrl + '?keystorePassword=' + password, '_blank');
    }
}
