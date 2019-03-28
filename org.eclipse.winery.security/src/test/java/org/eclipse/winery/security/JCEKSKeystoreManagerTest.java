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

package org.eclipse.winery.security;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.Objects;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.eclipse.winery.security.algorithm.AbstractSecurityTestClass;
import org.eclipse.winery.security.support.enums.SymmetricEncryptionAlgorithmEnum;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

class JCEKSKeystoreManagerTest extends AbstractSecurityTestClass {
    private static final String KEY_STORE_FILE_NAME = "winery-keystore.jceks";
    private JCEKSKeystoreManager manager;
    private Path tempKeyStoreFile;

    private SecretKey generateRandomSecretKey(SymmetricEncryptionAlgorithmEnum algorithm) throws NoSuchProviderException, NoSuchAlgorithmException {
        KeyGenerator keyGenerator;
        keyGenerator = KeyGenerator.getInstance(algorithm.getName(), BouncyCastleProvider.PROVIDER_NAME);
        keyGenerator.init(algorithm.getkeySizeInBits(), new SecureRandom());
        return keyGenerator.generateKey();
    }

    @BeforeEach
    public void setUp() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, IOException {
        super.setUp();
        this.tempKeyStoreFile = Files.createTempFile("winery", ".jceks");

        try (InputStream stream = Objects.requireNonNull(getClass().getClassLoader().getResource(KEY_STORE_FILE_NAME)).openStream()) {
            Files.copy(stream, tempKeyStoreFile, StandardCopyOption.REPLACE_EXISTING);
        }

        manager = new JCEKSKeystoreManager(this.tempKeyStoreFile.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(this.tempKeyStoreFile);
    }

//    @Test
//    void storeSecretKeyWithAttributes() throws NoSuchProviderException, NoSuchAlgorithmException, GenericKeystoreManagerException, UnrecoverableEntryException, KeyStoreException {
//        String alias = "WoW!!";
//        String attName = "stupidityLevel";
//        String attValue = "1200";
//        SecretKey key = generateRandomSecretKey(SymmetricEncryptionAlgorithmEnum.AES512);
//        this.manager.storeSecretKeyWithAttributes(alias, key, Collections.singleton(new BasicKeyAttribute(attName, attValue)));
//        KeyStore.Entry result = this.manager.loadEntry(alias);
//        Assertions.assertNotNull(result);
//        Assertions.assertTrue(result instanceof KeyStore.SecretKeyEntry);
//        Set<KeyStore.Entry.Attribute> attributeSet = result.getAttributes();
//        Assertions.assertEquals(1, attributeSet.size());
//        KeyStore.Entry.Attribute attribute = (KeyStore.Entry.Attribute) attributeSet.toArray()[0];
//        Assertions.assertEquals(attName, attribute.getName());
//        Assertions.assertEquals(attValue, attribute.getValue());
//    }
}
