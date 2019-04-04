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

import java.io.File;
import java.io.IOException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import javax.crypto.SecretKey;

import org.eclipse.winery.accountability.AccountabilityManager;
import org.eclipse.winery.accountability.AccountabilityManagerFactory;
import org.eclipse.winery.accountability.exceptions.AccountabilityException;
import org.eclipse.winery.accountability.exceptions.BlockchainException;
import org.eclipse.winery.repository.backend.RepositoryFactory;
import org.eclipse.winery.security.KeystoreManager;
import org.eclipse.winery.security.SecurityProcessorFactory;
import org.eclipse.winery.security.exceptions.GenericKeystoreManagerException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PermissionsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionsManager.class);
    private static PermissionsManager instance;
    private final File KEY_ASSIGNMENTS_FILE;

    private Map<String, KeyAssignments> keyAssignmentsMap;
    private ObjectMapper objectMapper;

    private PermissionsManager(File keyAssignmentsFile) {
        this.KEY_ASSIGNMENTS_FILE = keyAssignmentsFile;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public static PermissionsManager getInstance(File keyPermissionsFile) {
        if (instance == null) {
            SecurityProcessorFactory.allowUnlimitedEncryption();
            instance = new PermissionsManager(keyPermissionsFile);
        }

        instance.loadKeyAssignments();

        return instance;
    }

    private void loadKeyAssignments() {
        if (KEY_ASSIGNMENTS_FILE.exists()) {
            try {
                TypeReference<HashMap<String, KeyAssignments>> hashMapTypeReference =
                    new TypeReference<HashMap<String, KeyAssignments>>() {
                    };
                this.keyAssignmentsMap = this.objectMapper.readValue(KEY_ASSIGNMENTS_FILE, hashMapTypeReference);
            } catch (IOException e) {
                LOGGER.error("Error while loading key assignments file. Reason: {}", e);
                throw new RuntimeException(e);
            }
        } else {
            this.keyAssignmentsMap = new HashMap<>();
            this.saveKeyAssignmentsFile();
        }
    }

    private void saveKeyAssignmentsFile() {
        try {
            ensureFileExists(KEY_ASSIGNMENTS_FILE);
            this.objectMapper.writeValue(KEY_ASSIGNMENTS_FILE, this.keyAssignmentsMap);
        } catch (Exception e) {
            LOGGER.error("Could not save key assignments file!", e);
        }
    }

    private void ensureFileExists(File file) {
        if (!file.exists()) {
            try {
                if (file.getParentFile().mkdirs() || file.createNewFile()) {
                    LOGGER.debug("Created new file at {}", file);
                } else {
                    LOGGER.error("Could not create file at {}", file);
                    throw new RuntimeException();
                }
            } catch (IOException e) {
                LOGGER.error("Could not create file at {}. Reason: {}", file, e);
                throw new RuntimeException(e);
            }
        }
    }

    private void addKeyAssignment(String alias, String giver, String taker) {
        if (!this.keyAssignmentsMap.containsKey(alias)) {
            this.keyAssignmentsMap.put(alias, new KeyAssignments());
            this.keyAssignmentsMap.get(alias).setKeyAlias(alias);
        }

        this.keyAssignmentsMap.get(alias).addGiver(giver);
        this.keyAssignmentsMap.get(alias).addTaker(taker);
    }

    private AccountabilityManager loadAccountabilityManager() throws AccountabilityException {
        Properties props = RepositoryFactory.getRepository().getAccountabilityConfigurationManager().properties;
        return AccountabilityManagerFactory.getAccountabilityManager(props);
    }

    private CompletableFuture<PrivateKey> getMyPrivateKey() throws BlockchainException, AccountabilityException {
        return getMyOfficialKeyPair()
            .thenApply(KeyPair::getPrivate);
    }

    private SecretKey extractSecretKey(KeystoreManager manager, KeyAssignments value) throws GenericKeystoreManagerException {
        Key key = manager.loadKey(value.getKeyAlias());

        if (Objects.isNull(key)) {
            LOGGER.error("The entry with the alias {} is not found in the key store", value.getKeyAlias());
            throw new GenericKeystoreManagerException("Key not found in key store");
        }

        if (!(key instanceof SecretKey)) {
            LOGGER.error("The entry with the alias {} is expected to be of type SecretKey but is actually of type {}.", value.getKeyAlias(), key.getClass().getName());
            throw new ClassCastException();
        }

        return (SecretKey) key;
    }

    public Map<String, KeyAssignments> getKeyAssignments() {
        return this.keyAssignmentsMap;
    }

    public ArrayList<SecretKey> getGivenPermissions(String taker) {
        ArrayList<SecretKey> result = new ArrayList<>();
        KeystoreManager manager = KeystoreManagerFactory.getInstance();

        for (KeyAssignments value : this.keyAssignmentsMap.values()) {
            if (value.getKeyTakers().contains(taker)) {
                try {
                    result.add(extractSecretKey(manager, value));
                } catch (GenericKeystoreManagerException e) {
                    // if we are not able to add a specific key because it is deleted from the key store, skip it
                    // logging already done
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    public ArrayList<SecretKey> getTakenPermissions(String giver) {
        ArrayList<SecretKey> result = new ArrayList<>();
        KeystoreManager manager = KeystoreManagerFactory.getInstance();

        for (KeyAssignments value : this.keyAssignmentsMap.values()) {
            if (value.getKeyGivers().contains(giver)) {
                try {
                    result.add(extractSecretKey(manager, value));
                } catch (GenericKeystoreManagerException e) {
                    // the key could be not retrieved yet, therefore missing from the key store. In this case skip it
                    // logging already done
                    e.printStackTrace();
                }
            }
        }

        return result;
    }

    public String getMyIdentity() throws AccountabilityException {
        return this.loadAccountabilityManager().getMyIdentity();
    }

    public CompletableFuture<String> getMyOfficialKeyPairAlias() throws AccountabilityException, BlockchainException {
        AccountabilityManager accManager = this.loadAccountabilityManager();
        KeystoreManager keystoreManager = KeystoreManagerFactory.getInstance();
        final String myIdentity = accManager.getMyIdentity();

        return accManager
            .getParticipantPublicKey(myIdentity)
            .thenApply(publicKey -> {
                try {
                    return keystoreManager.findAliasOfPublicKey(publicKey);
                } catch (KeyStoreException | GenericKeystoreManagerException e) {
                    throw new CompletionException(e);
                }
            });
    }

    public CompletableFuture<KeyPair> getMyOfficialKeyPair() throws AccountabilityException, BlockchainException {
        KeystoreManager keystoreManager = KeystoreManagerFactory.getInstance();

        return this.getMyOfficialKeyPairAlias()
            .thenApply(alias -> {
                try {
                    Objects.requireNonNull(alias);
                    return keystoreManager.loadKeyPair(alias);
                } catch (Exception e) {
                    LOGGER.error("Failed to load the official key pair. Reason:{}", e);
                    throw new CompletionException(e);
                }
            });
    }

    public CompletableFuture<Void> setMyOfficialKeyPair(String alias) throws GenericKeystoreManagerException, AccountabilityException, BlockchainException {
        try {
            AccountabilityManager accManager = this.loadAccountabilityManager();
            KeystoreManager keystoreManager = KeystoreManagerFactory.getInstance();
            PublicKey key = keystoreManager.loadKeyPair(alias).getPublic();
            return accManager.setMyPublicKey(key);
        } catch (GenericKeystoreManagerException | AccountabilityException | BlockchainException e) {
            LOGGER.error("Cannot set the official key pair with the specified alias. Reason: {}", e);
            throw e;
        }
    }

    public CompletableFuture<UpdatePermissionsResult> updateListOfPermissionsGivenToMe() throws AccountabilityException, BlockchainException {
        AccountabilityManager accManager = this.loadAccountabilityManager();
        KeystoreManager keyManager = KeystoreManagerFactory.getInstance();

        return this
            .getMyPrivateKey()
            .thenCompose(myPrivateKey -> {
                try {
                    return accManager.getMyPermissions(myPrivateKey);
                } catch (BlockchainException e) {
                    throw new CompletionException(e);
                }
            })
            .thenApply(map -> {
                try {
                    int addedKeysCounter = 0;
                    int wrongEncryptionCounter = 0;

                    for (String giver : map.keySet()) {
                        SecretKey[] permissions = map.get(giver);

                        if (permissions != null) {
                            for (SecretKey permission : permissions) {
                                String alias = keyManager.generateAlias(permission);
                                this.addKeyAssignment(alias, giver, this.getMyIdentity());

                                if (!keyManager.entityExists(alias)) {
                                    keyManager.storeKey(alias, permission);
                                    ++addedKeysCounter;
                                }
                            }
                        } else {
                            ++wrongEncryptionCounter;
                        }
                    }

                    this.saveKeyAssignmentsFile();

                    return new UpdatePermissionsResult(addedKeysCounter, wrongEncryptionCounter);
                } catch (IOException | NoSuchAlgorithmException | GenericKeystoreManagerException | AccountabilityException e) {
                    LOGGER.error("An error occurred while adding permission to keystore. Reason: {}", e);
                    throw new CompletionException(e);
                }
            });
    }

    public CompletableFuture<Void> givePermission(String takerAddress, String permissionAlias) throws AccountabilityException, GenericKeystoreManagerException, BlockchainException, IllegalArgumentException {
        ArrayList<SecretKey> alreadyGiven = this.getGivenPermissions(takerAddress);
        KeystoreManager keyManager = KeystoreManagerFactory.getInstance();
        Key key = keyManager.loadKey(permissionAlias);
        final String myIdentity = this.getMyIdentity();

        if (!(key instanceof SecretKey)) {
            LOGGER.error("Passed alias does not correspond to a SecretKey but rather to a {}", key.getClass().getName());
            throw new IllegalArgumentException();
        }

        SecretKey permission = (SecretKey) key;

        // this check is tricky in the following scenario:
        // A gives permissions to B
        // B changes official public key
        // A recognizes this (magically) and tries to re-give the same permissions but encrypted with the new public key to B
        // this check will reject it!
        if (alreadyGiven.contains(permission))
            return CompletableFuture.completedFuture(null);

        alreadyGiven.add(permission);
        AccountabilityManager accManager = this.loadAccountabilityManager();

        return accManager
            .setPermissions(takerAddress, alreadyGiven.toArray(new SecretKey[0]))
            .thenAccept(nothing -> {
                this.addKeyAssignment(permissionAlias, myIdentity, takerAddress);
                this.saveKeyAssignmentsFile();
            });
    }
    
}
