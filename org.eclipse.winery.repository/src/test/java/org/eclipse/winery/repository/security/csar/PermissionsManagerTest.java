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

package org.eclipse.winery.repository.security.csar;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import javax.crypto.SecretKey;

import org.eclipse.winery.accountability.AccountabilityManager;
import org.eclipse.winery.accountability.AccountabilityManagerFactory;
import org.eclipse.winery.accountability.exceptions.AccountabilityException;
import org.eclipse.winery.accountability.exceptions.BlockchainException;
import org.eclipse.winery.repository.TestWithGitBackedRepository;
import org.eclipse.winery.repository.backend.AccountabilityConfigurationManager;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.security.KeystoreManager;
import org.eclipse.winery.security.SecurityProcessor;
import org.eclipse.winery.security.SecurityProcessorFactory;
import org.eclipse.winery.security.datatypes.DistinguishedName;
import org.eclipse.winery.security.datatypes.KeyEntityInformation;
import org.eclipse.winery.security.datatypes.KeyPairInformation;
import org.eclipse.winery.security.exceptions.GenericKeystoreManagerException;
import org.eclipse.winery.security.exceptions.GenericSecurityProcessorException;
import org.eclipse.winery.security.support.enums.AsymmetricEncryptionAlgorithmEnum;
import org.eclipse.winery.security.support.enums.SymmetricEncryptionAlgorithmEnum;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
class PermissionsManagerTest extends TestWithGitBackedRepository {

    private static final String PRIMARY_KEYSTORE_FILE_NAME = "UTC--2018-03-05T15-33-22.456000000Z--e4b51a3d4e77d2ce2a9d9ce107ec8ec7cff5571d.json";
    private static final String SECONDARY_KEYSTORE_FILE_NAME = "UTC--2018-05-31T17-09-06.917268191Z--696c7c33ac2aa448880f7c1e5f85eb8c2401cf03.json";
    private static final String PRIMARY_ADDRESS = "0xe4b51a3d4e77d2ce2a9d9ce107ec8ec7cff5571d";
    private static final String SECONDARY_ADDRESS = "0x696c7c33ac2aa448880f7c1e5f85eb8c2401cf03";
    private static final String SAMPLE_DISTINGUISHED_NAME = "CN=P2OfficialPublicKey, O=org, C=DE";

    private PermissionsManager manager;
    private KeystoreManager keystoreManager;
    private SecurityProcessor securityProcessor;
    private String permSCAddress;

    /*
     * This static block guarantees that setting the property that allows java to use strong encryption is initialized
     * before any other code that could initialize the security framework before (such as Git or Web3J)
     */
    static {
        // Available since Java8u151, allows 256bit key usage
        Security.setProperty("crypto.policy", "unlimited");
    }

    @BeforeEach
    void initializeAccountabilityLayer() throws IOException, AccountabilityException, ExecutionException, InterruptedException {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(PRIMARY_KEYSTORE_FILE_NAME)) {
            AccountabilityConfigurationManager configurationManager = RepositoryFactory.getRepository().getAccountabilityConfigurationManager();
            configurationManager.setNewKeystoreFile(inputStream, PRIMARY_KEYSTORE_FILE_NAME);
            AccountabilityManager accountabilityManager = AccountabilityManagerFactory.getAccountabilityManager(configurationManager.properties);
            this.permSCAddress = accountabilityManager.deployPermissionsSmartContract().get();
        }
    }

    private void changeActiveUser(String newKeystoreFile) throws IOException, GitAPIException {
        setRevisionTo("origin/plain");

        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(newKeystoreFile)) {
            AccountabilityConfigurationManager configurationManager = RepositoryFactory.getRepository().getAccountabilityConfigurationManager();
            configurationManager.setNewKeystoreFile(inputStream, newKeystoreFile);
            configurationManager.properties.put("ethereum-permissions-smart-contract-address", permSCAddress);
            configurationManager.saveProperties();
        }

        this.manager = RepositoryFactory.getRepository().getPermissionsManager();
        this.keystoreManager = KeystoreManagerFactory.getInstance();
        this.securityProcessor = SecurityProcessorFactory.getDefaultSecurityProcessor();
    }

    @Test
    void testRemovingGivenKey() throws IOException, GitAPIException, GenericSecurityProcessorException, NoSuchAlgorithmException, AccountabilityException, BlockchainException, GenericKeystoreManagerException, ExecutionException, InterruptedException {
        this.changeActiveUser(SECONDARY_KEYSTORE_FILE_NAME);
        KeyPair keyPair = securityProcessor.generateKeyPair(AsymmetricEncryptionAlgorithmEnum.ECIES_secp256k1);
        KeyPairInformation info = keystoreManager.storeKeyPair(keystoreManager.generateAlias(keyPair.getPublic()),
            keyPair, new DistinguishedName(SAMPLE_DISTINGUISHED_NAME));
        this.manager.setMyOfficialKeyPair(info.getPublicKey().getAlias()).get();

        this.changeActiveUser(PRIMARY_KEYSTORE_FILE_NAME);
        SecretKey permission = securityProcessor.generateSecretKey(SymmetricEncryptionAlgorithmEnum.AES512);
        KeyEntityInformation permissionInfo = keystoreManager.storeKey(keystoreManager.generateAlias(permission), permission);
        manager.givePermission(SECONDARY_ADDRESS, permissionInfo.getAlias()).get();
        keystoreManager.deleteKeystoreEntry(permissionInfo.getAlias());
        ArrayList<SecretKey> givenKeys = this.manager.getGivenPermissions(SECONDARY_ADDRESS);
        Assertions.assertEquals(0, givenKeys.size());

        this.changeActiveUser(PRIMARY_KEYSTORE_FILE_NAME);
        ArrayList<SecretKey> takenKeys = manager.getTakenPermissions(PRIMARY_ADDRESS);
        Assertions.assertEquals(0, takenKeys.size());
    }

    @Test
    void testChangingOfficialKeyPair() throws IOException, GitAPIException, GenericSecurityProcessorException, NoSuchAlgorithmException, GenericKeystoreManagerException, BlockchainException, AccountabilityException, ExecutionException, InterruptedException {
        this.changeActiveUser(SECONDARY_KEYSTORE_FILE_NAME);
        KeyPair keyPair = securityProcessor.generateKeyPair(AsymmetricEncryptionAlgorithmEnum.ECIES_secp256k1);
        KeyPairInformation info = keystoreManager.storeKeyPair(keystoreManager.generateAlias(keyPair.getPublic()),
            keyPair, new DistinguishedName(SAMPLE_DISTINGUISHED_NAME));
        this.manager.setMyOfficialKeyPair(info.getPublicKey().getAlias()).get();

        this.changeActiveUser(PRIMARY_KEYSTORE_FILE_NAME);
        SecretKey permission = securityProcessor.generateSecretKey(SymmetricEncryptionAlgorithmEnum.AES512);
        KeyEntityInformation permissionInfo = keystoreManager.storeKey(keystoreManager.generateAlias(permission), permission);
        manager.givePermission(SECONDARY_ADDRESS, permissionInfo.getAlias()).get();

        this.changeActiveUser(SECONDARY_KEYSTORE_FILE_NAME);
        // now the second user does not have the keypair in the keystore (the repo is wiped)
        // we create a new one instead of reusing the old one
        keyPair = securityProcessor.generateKeyPair(AsymmetricEncryptionAlgorithmEnum.ECIES_secp256k1);
        info = keystoreManager.storeKeyPair(keystoreManager.generateAlias(keyPair.getPublic()),
            keyPair, new DistinguishedName(SAMPLE_DISTINGUISHED_NAME));
        this.manager.setMyOfficialKeyPair(info.getPublicKey().getAlias()).get();
        UpdatePermissionsResult result = manager.updateListOfPermissionsGivenToMe().get();
        Assertions.assertEquals(0, result.getAddedKeysCount());
        Assertions.assertEquals(1, result.getBadGiversCount());
    }

    @Test
    void testSettingOfficialPublicKey() throws IOException, GitAPIException, GenericSecurityProcessorException, NoSuchAlgorithmException, AccountabilityException, BlockchainException, GenericKeystoreManagerException, ExecutionException, InterruptedException {
        this.changeActiveUser(PRIMARY_KEYSTORE_FILE_NAME);
        KeyPair keyPair = securityProcessor.generateKeyPair(AsymmetricEncryptionAlgorithmEnum.ECIES_secp256k1);
        KeyPairInformation info = keystoreManager.storeKeyPair(keystoreManager.generateAlias(keyPair.getPublic()),
            keyPair, new DistinguishedName(SAMPLE_DISTINGUISHED_NAME));
        this.manager.setMyOfficialKeyPair(info.getPublicKey().getAlias()).get();
        KeyPair retrieved = this.manager.getMyOfficialKeyPair().get();
        Assertions.assertArrayEquals(keyPair.getPublic().getEncoded(), retrieved.getPublic().getEncoded());
    }

    @Test
    void testGivingPermissions() throws IOException, GenericSecurityProcessorException, NoSuchAlgorithmException, GenericKeystoreManagerException, GitAPIException, AccountabilityException, BlockchainException, InvalidKeyException, ExecutionException, InterruptedException {
        // set official key pair for second participant
        this.changeActiveUser(SECONDARY_KEYSTORE_FILE_NAME);
        KeyPair participant2KP = securityProcessor.generateKeyPair(AsymmetricEncryptionAlgorithmEnum.ECIES_secp256k1);
        KeyPairInformation info = keystoreManager.storeKeyPair(keystoreManager.generateAlias(participant2KP.getPublic()),
            participant2KP, new DistinguishedName(SAMPLE_DISTINGUISHED_NAME));
        manager.setMyOfficialKeyPair(info.getPublicKey().getAlias()).get();

        // This tests giving permissions
        this.changeActiveUser(PRIMARY_KEYSTORE_FILE_NAME);
        SecretKey permission1 = securityProcessor.generateSecretKey(SymmetricEncryptionAlgorithmEnum.AES512);
        SecretKey permission2 = securityProcessor.generateSecretKey(SymmetricEncryptionAlgorithmEnum.AES512);
        KeyEntityInformation permission1Info = keystoreManager.storeKey(keystoreManager.generateAlias(permission1), permission1);
        KeyEntityInformation permission2Info = keystoreManager.storeKey(keystoreManager.generateAlias(permission2), permission2);
        manager.givePermission(SECONDARY_ADDRESS, permission1Info.getAlias()).get();
        manager.givePermission(SECONDARY_ADDRESS, permission2Info.getAlias()).get();
        ArrayList<SecretKey> givenPermissions = manager.getGivenPermissions(SECONDARY_ADDRESS);
        Assertions.assertEquals(2, givenPermissions.size());
        Assertions.assertTrue(givenPermissions.contains(permission1));
        Assertions.assertTrue(givenPermissions.contains(permission2));

        // This tests taking permissions
        this.changeActiveUser(SECONDARY_KEYSTORE_FILE_NAME);
        // we store the official key pair again in the key store since changing the active user wipes it out.
        keystoreManager.storeKeyPair(info.getPublicKey().getAlias(),
            participant2KP, new DistinguishedName(SAMPLE_DISTINGUISHED_NAME));
        UpdatePermissionsResult result = manager.updateListOfPermissionsGivenToMe().get();
        Assertions.assertEquals(2, result.getAddedKeysCount());
        Assertions.assertEquals(0, result.getBadGiversCount());

        ArrayList<SecretKey> takenPermissions = manager.getTakenPermissions(PRIMARY_ADDRESS);
        Assertions.assertEquals(2, takenPermissions.size());
        Assertions.assertTrue(takenPermissions.contains(permission1));
        Assertions.assertTrue(takenPermissions.contains(permission2));
    }
}
