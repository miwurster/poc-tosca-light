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
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import javax.crypto.SecretKey;

import org.eclipse.winery.accountability.EthereumPropertiesHelper;
import org.eclipse.winery.accountability.KeyHelper;
import org.eclipse.winery.accountability.blockchain.BlockchainFactory;
import org.eclipse.winery.accountability.exceptions.BlockchainException;
import org.eclipse.winery.security.support.enums.SymmetricEncryptionAlgorithmEnum;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
class EthereumAccessLayerTest {
    private static final String KEYSTORE_PASSWORD = "987654321";
    private static final String CONFIGURATION_FILE_NAME = "defaultaccountabilityconfig.properties";
    private static final String PRIMARY_KEYSTORE_FILE_NAME = "UTC--2018-03-05T15-33-22.456000000Z--e4b51a3d4e77d2ce2a9d9ce107ec8ec7cff5571d.json";
    private static final String SECONDARY_KEYSTORE_FILE_NAME = "UTC--2018-05-31T17-09-06.917268191Z--696c7c33ac2aa448880f7c1e5f85eb8c2401cf03.json";
    private static final String PRIMARY_ADDRESS = "0xe4b51a3d4e77d2ce2a9d9ce107ec8ec7cff5571d";
    private static final String SECONDARY_ADDRESS = "0x696c7c33ac2aa448880f7c1e5f85eb8c2401cf03";
    private static final String SECRET_KEY_PHRASE = "secret key! do not tell anyone about it!";
    private EthereumPropertiesHelper helper = new EthereumPropertiesHelper();
    private EthereumAccessLayer blockchainAccess;

    EthereumAccessLayer loadAccessLayer(String keystoreFileName) throws IOException, BlockchainException {
        Properties props = this.helper.getProperties(keystoreFileName);

        return (EthereumAccessLayer) BlockchainFactory
            .getBlockchainAccess(BlockchainFactory.AvailableBlockchains.ETHEREUM, props);
    }

    @BeforeEach
    void setUp() throws IOException, BlockchainException, ExecutionException, InterruptedException {
        Security.addProvider(new BouncyCastleProvider());
        // Available since Java8u151, allows 256bit key usage
        Security.setProperty("crypto.policy", "unlimited");
        this.helper.initializeAccessLayer();
        this.blockchainAccess = loadAccessLayer(PRIMARY_KEYSTORE_FILE_NAME);
    }

    @Test
    void testKeystoreGeneration() throws BlockchainException {
        final Path path = blockchainAccess.createNewKeystore(KEYSTORE_PASSWORD);
        blockchainAccess.unlockCredentials(KEYSTORE_PASSWORD, path.toString());
        blockchainAccess.close();
    }

    @Test
    void testGivingPermissions() throws IOException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, BlockchainException, ExecutionException, InterruptedException {
        // test giving a single permission
        // first set the official public key of the taker
        BlockchainFactory.reset();
        this.blockchainAccess = this.loadAccessLayer(SECONDARY_KEYSTORE_FILE_NAME);
        KeyPair takerKeyPair = KeyHelper.generateECIESKeyPair();
        this.blockchainAccess.setMyPublicKey(takerKeyPair.getPublic()).get();
        // now give the premission
        BlockchainFactory.reset();
        this.blockchainAccess = this.loadAccessLayer(PRIMARY_KEYSTORE_FILE_NAME);
        SecretKey permission = KeyHelper.convertStringToSecretKey(SECRET_KEY_PHRASE, SymmetricEncryptionAlgorithmEnum.AES512);
        this.blockchainAccess
            .setPermissions(SECONDARY_ADDRESS, new SecretKey[] {permission})
            .get();
        // now check that permission is taken
        BlockchainFactory.reset();
        this.blockchainAccess = this.loadAccessLayer(SECONDARY_KEYSTORE_FILE_NAME);
        Map<String, SecretKey[]> permissions = this.blockchainAccess.getMyPermissions(takerKeyPair.getPrivate()).get();
        Assertions.assertTrue(permissions.keySet().contains(PRIMARY_ADDRESS));
        Assertions.assertEquals(1, permissions.get(PRIMARY_ADDRESS).length);
        Assertions.assertTrue(Arrays.asList(permissions.get(PRIMARY_ADDRESS)).contains(permission));

        // test changing the single permission to a set of two other permissions from the same giver
        SecretKey permission1 = KeyHelper.convertStringToSecretKey(SECRET_KEY_PHRASE + "A", SymmetricEncryptionAlgorithmEnum.AES512);
        SecretKey permission2 = KeyHelper.convertStringToSecretKey(SECRET_KEY_PHRASE + "B", SymmetricEncryptionAlgorithmEnum.AES512);
        BlockchainFactory.reset();
        this.blockchainAccess = this.loadAccessLayer(PRIMARY_KEYSTORE_FILE_NAME);
        this.blockchainAccess
            .setPermissions(SECONDARY_ADDRESS, new SecretKey[] {permission1, permission2})
            .get();
        BlockchainFactory.reset();
        this.blockchainAccess = this.loadAccessLayer(SECONDARY_KEYSTORE_FILE_NAME);
        permissions = this.blockchainAccess.getMyPermissions(takerKeyPair.getPrivate()).get();
        Assertions.assertEquals(2, permissions.get(PRIMARY_ADDRESS).length);
        Assertions.assertFalse(Arrays.asList(permissions.get(PRIMARY_ADDRESS)).contains(permission));
        Assertions.assertTrue(Arrays.asList(permissions.get(PRIMARY_ADDRESS)).contains(permission1));
        Assertions.assertTrue(Arrays.asList(permissions.get(PRIMARY_ADDRESS)).contains(permission2));
    }

    @Test
    void testGivingPermissionsToParticipantWithoutOfficialPublicKey() throws IOException, BlockchainException, NoSuchAlgorithmException {
        BlockchainFactory.reset();
        this.blockchainAccess = this.loadAccessLayer(PRIMARY_KEYSTORE_FILE_NAME);
        SecretKey permission = KeyHelper.convertStringToSecretKey(SECRET_KEY_PHRASE, SymmetricEncryptionAlgorithmEnum.AES512);
        Assertions.assertThrows(Exception.class, () ->
            this.blockchainAccess
                .setPermissions(SECONDARY_ADDRESS, new SecretKey[] {permission})
                .get());
    }
}
