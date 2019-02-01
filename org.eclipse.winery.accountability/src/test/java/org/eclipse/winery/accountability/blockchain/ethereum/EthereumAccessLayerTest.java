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

package org.eclipse.winery.accountability.blockchain.ethereum;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

import org.eclipse.winery.accountability.blockchain.BlockchainFactory;
import org.eclipse.winery.accountability.exceptions.BlockchainException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EthereumAccessLayerTest {
    private static final String KEYSTORE_PASSWORD = "987654321";
    private static final String CONFIGURATION_FILE_NAME = "defaultaccountabilityconfig.properties";
    private static final String KEYSTORE_FILE_NAME = "UTC--2018-03-05T15-33-22.456000000Z--e4b51a3d4e77d2ce2a9d9ce107ec8ec7cff5571d.json";
    private EthereumAccessLayer blockchainAccess;

    @BeforeEach
    void setUp() throws IOException, BlockchainException {
        try (InputStream propsStream = getClass().getClassLoader().getResourceAsStream(CONFIGURATION_FILE_NAME)) {
            Properties props = new Properties();
            props.load(propsStream);
            // we can only tell the keystore file path during runtime.
            String keystorePath = Objects.requireNonNull(getClass().getClassLoader().getResource(KEYSTORE_FILE_NAME)).getPath();
            props.setProperty("ethereum-credentials-file-path", keystorePath);
            this.blockchainAccess = (EthereumAccessLayer)BlockchainFactory
                .getBlockchainAccess(BlockchainFactory.AvailableBlockchains.ETHEREUM, props);
        }
    }

    @Test
    void testKeystoreGeneration() throws BlockchainException {
        URL pathK = getClass().getClassLoader().getResource(KEYSTORE_FILE_NAME);
        final Path path = blockchainAccess.createNewKeystore(KEYSTORE_PASSWORD);
        blockchainAccess.unlockCredentials(KEYSTORE_PASSWORD, path.toString());
        blockchainAccess.close();
    }
}
