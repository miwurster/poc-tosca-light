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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
import org.eclipse.winery.security.exceptions.GenericKeystoreManagerException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PermissionsManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionsManager.class);
    // we declare a constant for each property we store in the preferences file.
    private static final String OFFICIAL_KEY_PAIR_ALIAS_PROP = "OfficialKeyPairAlias";
    private static PermissionsManager instance;
    private final File PREFERENCES_FILE;
    private final File KEY_ASSIGNMENTS_FILE;

    private Properties properties = null;
    private Map<String, KeyAssignments> keyAssignmentsMap;
    private ObjectMapper objectMapper;

    private PermissionsManager(File preferencesFile, File keyAssignmentsFile) {
        this.PREFERENCES_FILE = preferencesFile;
        this.KEY_ASSIGNMENTS_FILE = keyAssignmentsFile;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public static PermissionsManager getInstance(File preferencesFile, File keyPermissionsFile) {
        if (instance == null) {
            instance = new PermissionsManager(preferencesFile, keyPermissionsFile);
        }

        instance.loadPreferences();
        instance.loadKeyAssignments();

        return instance;
    }

    private void loadPreferences() {
        this.properties = new Properties();

        if (PREFERENCES_FILE.exists()) {
            try (InputStream inputStream = new FileInputStream(PREFERENCES_FILE)) {
                properties.load(inputStream);
            } catch (IOException e) {
                LOGGER.error("Error while loading permissions preferences file. Reason: {}", e);
                throw new RuntimeException(e);
            }
        } else {
            this.properties.put(OFFICIAL_KEY_PAIR_ALIAS_PROP, "");
            this.savePreferenceFile();
        }
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

    private void savePreferenceFile() {
        try {
            ensureFileExists(PREFERENCES_FILE);

            try (OutputStream stream = new FileOutputStream(PREFERENCES_FILE)) {
                this.properties.store(stream, null);
            }
        } catch (Exception e) {
            LOGGER.error("Could not save preferences file!", e);
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

    private PrivateKey getMyPrivateKey() throws GenericKeystoreManagerException {
        return getMyOfficialKeyPair().getPrivate();
    }

    private SecretKey extractSecretKey(KeystoreManager manager, KeyAssignments value) throws GenericKeystoreManagerException {
        Key key = manager.loadKey(value.getKeyAlias());

        if (!(key instanceof SecretKey)) {
            LOGGER.error("The entry with the alias {} is expected to be of type SecretKey but is actually of type {}.", value.getKeyAlias(), key.getClass().getName());
            throw new ClassCastException();
        }
        return (SecretKey) key;
    }

    public Map<String, KeyAssignments> getKeyAssignments() {
        return this.keyAssignmentsMap;
    }

    public ArrayList<SecretKey> getGivenPermissions(String taker) throws GenericKeystoreManagerException {
        ArrayList<SecretKey> result = new ArrayList<>();
        KeystoreManager manager = KeystoreManagerFactory.getInstance();

        for (KeyAssignments value : this.keyAssignmentsMap.values()) {
            if (value.getKeyTakers().contains(taker)) {
                result.add(extractSecretKey(manager, value));
            }
        }

        return result;
    }

    public ArrayList<SecretKey> getTakenPermissions(String giver) throws GenericKeystoreManagerException {
        ArrayList<SecretKey> result = new ArrayList<>();
        KeystoreManager manager = KeystoreManagerFactory.getInstance();

        for (KeyAssignments value : this.keyAssignmentsMap.values()) {
            if (value.getKeyGivers().contains(giver)) {
                result.add(extractSecretKey(manager, value));
            }
        }

        return result;
    }

    public String getMyIdentity() throws AccountabilityException {
        return this.loadAccountabilityManager().getMyIdentity();
    }

    public KeyPair getMyOfficialKeyPair() throws GenericKeystoreManagerException {
        String keyPairAlias = this.properties.getProperty(OFFICIAL_KEY_PAIR_ALIAS_PROP);
        return KeystoreManagerFactory.getInstance().loadKeyPair(keyPairAlias);
    }

    public void setMyOfficialKeyPair(String alias) throws GenericKeystoreManagerException {
        try {
            KeystoreManagerFactory.getInstance().loadKeyPair(alias);
            this.properties.put(OFFICIAL_KEY_PAIR_ALIAS_PROP, alias);
            this.savePreferenceFile();
        } catch (GenericKeystoreManagerException e) {
            LOGGER.error("Cannot set the official key pair with the specified alias. Reason: {}", e);
            throw e;
        }
    }

    public CompletableFuture<Void> updateListOfPermissionsGivenToMe() throws AccountabilityException, GenericKeystoreManagerException, BlockchainException {
        AccountabilityManager accManager = this.loadAccountabilityManager();
        KeystoreManager keyManager = KeystoreManagerFactory.getInstance();
        PrivateKey myPrivateKey = this.getMyPrivateKey();

        return accManager
            .getMyPermissions(myPrivateKey)
            .thenAccept(map -> {
                try {
                    for (String giver : map.keySet()) {
                        SecretKey[] permissions = map.get(giver);

                        for (SecretKey permission : permissions) {
                            String alias = keyManager.generateAlias(permission);
                            keyManager.storeKey(alias, permission);
                            this.addKeyAssignment(alias, giver, this.getMyIdentity());
                        }
                    }
                    this.saveKeyAssignmentsFile();
                } catch (IOException | NoSuchAlgorithmException | GenericKeystoreManagerException | AccountabilityException e) {
                    LOGGER.error("An error occurred while adding permission to keystore. Reason: {}", e);
                    throw new CompletionException(e);
                }
            });
    }

    public CompletableFuture<Void> givePermissions(String takerAddress, PublicKey takerPublicKey, String permissionAlias) throws AccountabilityException, GenericKeystoreManagerException, BlockchainException, InvalidKeyException {
        ArrayList<SecretKey> alreadyGiven = this.getGivenPermissions(takerAddress);
        KeystoreManager keyManager = KeystoreManagerFactory.getInstance();
        Key key = keyManager.loadKey(permissionAlias);
        final String myIdentity = this.getMyIdentity();

        if (!(key instanceof SecretKey)) {
            LOGGER.error("Passed alias does not correspond to a SecretKey but rather to a {}", key.getClass().getName());
            throw new IllegalArgumentException();
        }

        SecretKey permission = (SecretKey) key;

        if (alreadyGiven.contains(permission))
            return CompletableFuture.completedFuture(null);

        alreadyGiven.add(permission);
        AccountabilityManager accManager = this.loadAccountabilityManager();

        return accManager
            .setPermissions(takerAddress, takerPublicKey, alreadyGiven.toArray(new SecretKey[0]))
            .thenAccept(nothing -> {
                this.addKeyAssignment(permissionAlias, myIdentity, takerAddress);
                this.saveKeyAssignmentsFile();
            });
    }
}
