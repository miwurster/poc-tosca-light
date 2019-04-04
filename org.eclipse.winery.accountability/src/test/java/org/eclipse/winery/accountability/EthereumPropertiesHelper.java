/********************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.accountability;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.eclipse.winery.accountability.blockchain.BlockchainFactory;
import org.eclipse.winery.accountability.blockchain.ethereum.EthereumAccessLayer;
import org.eclipse.winery.accountability.exceptions.BlockchainException;

public class EthereumPropertiesHelper {
    private static final String CONFIGURATION_FILE_NAME = "defaultaccountabilityconfig.properties";
    private static final String PRIMARY_KEYSTORE_FILE_NAME = "UTC--2018-03-05T15-33-22.456000000Z--e4b51a3d4e77d2ce2a9d9ce107ec8ec7cff5571d.json";
    private String authorizationSCAddress;
    private String provenanceSCAddress;
    private String permissioningSCAddress;

    public void initializeAccessLayer() throws IOException, BlockchainException, ExecutionException, InterruptedException {
        try (InputStream propsStream = getClass().getClassLoader().getResourceAsStream(CONFIGURATION_FILE_NAME)) {
            Properties props = new Properties();
            props.load(propsStream);
            String keystorePath = Objects.requireNonNull(getClass().getClassLoader().getResource(PRIMARY_KEYSTORE_FILE_NAME)).getPath();
            props.setProperty("ethereum-credentials-file-path", keystorePath);
            EthereumAccessLayer result = (EthereumAccessLayer) BlockchainFactory
                .getBlockchainAccess(BlockchainFactory.AvailableBlockchains.ETHEREUM, props);
            BlockchainFactory.reset();
            this.authorizationSCAddress = result.deployAuthorizationSmartContract().get();
            this.provenanceSCAddress = result.deployProvenanceSmartContract().get();
            this.permissioningSCAddress = result.deployPermissionsSmartContract().get();
        }
    }

    public Properties getProperties() throws IOException {
        return this.getProperties(PRIMARY_KEYSTORE_FILE_NAME);
    }

    public Properties getProperties(String keystoreFileName) throws IOException {
        try (InputStream propsStream = getClass().getClassLoader().getResourceAsStream(CONFIGURATION_FILE_NAME)) {
            Properties props = new Properties();
            props.load(propsStream);
            String keystorePath = Objects.requireNonNull(getClass().getClassLoader().getResource(keystoreFileName)).getPath();
            props.setProperty("ethereum-credentials-file-path", keystorePath);
            props.setProperty("ethereum-permissions-smart-contract-address", this.permissioningSCAddress);
            props.setProperty("ethereum-provenance-smart-contract-address", this.provenanceSCAddress);
            props.setProperty("ethereum-authorization-smart-contract-address", this.authorizationSCAddress);

            return props;
        }
    }
}
