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

package org.eclipse.winery.security;

import java.io.IOException;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Collection;

import org.eclipse.winery.security.datatypes.CertificateInformation;
import org.eclipse.winery.security.datatypes.DistinguishedName;
import org.eclipse.winery.security.datatypes.KeyEntityInformation;
import org.eclipse.winery.security.datatypes.KeyPairInformation;
import org.eclipse.winery.security.datatypes.KeyType;
import org.eclipse.winery.security.datatypes.KeystoreContentsInformation;
import org.eclipse.winery.security.exceptions.GenericKeystoreManagerException;
import org.eclipse.winery.security.exceptions.GenericSecurityProcessorException;
import org.eclipse.winery.security.support.enums.AsymmetricEncryptionAlgorithmEnum;
import org.eclipse.winery.security.support.enums.SymmetricEncryptionAlgorithmEnum;

public interface KeystoreManager {

    boolean keystoreExists();

    boolean entityExists(String alias);

    Collection<SymmetricEncryptionAlgorithmEnum> getSymmetricAlgorithms();

    Collection<AsymmetricEncryptionAlgorithmEnum> getAsymmetricAlgorithms();

    Collection<KeyEntityInformation> getKeys(boolean withKeyEncoded);

    KeyEntityInformation getKey(String alias, KeyType type) throws GenericKeystoreManagerException;

    byte[] getKeyEncoded(String alias, KeyType type) throws GenericKeystoreManagerException;

    Collection<KeyPairInformation> getKeyPairs() throws GenericKeystoreManagerException;

    KeyPairInformation getKeyPairData(String alias) throws GenericKeystoreManagerException;

    Collection<CertificateInformation> getCertificates() throws GenericKeystoreManagerException;

    CertificateInformation getCertificate(String alias) throws GenericKeystoreManagerException;

    KeystoreContentsInformation getKeystoreContent() throws GenericKeystoreManagerException;

    int getKeystoreSize() throws GenericKeystoreManagerException;

    KeyEntityInformation storeKey(String alias, Key key) throws GenericKeystoreManagerException;

    KeyPairInformation storeKeyPair(String alias, PrivateKey privateKey, Certificate certificate) throws GenericKeystoreManagerException;

    KeyPairInformation storeKeyPair(String alias, KeyPair keyPair, DistinguishedName dn) throws GenericKeystoreManagerException, GenericSecurityProcessorException;

    Certificate storeCertificate(String alias, InputStream is) throws GenericKeystoreManagerException;

    Certificate storeCertificate(String alias, Certificate c) throws GenericKeystoreManagerException;

    Certificate storeCertificate(String alias, String pemEncodedString) throws GenericKeystoreManagerException;

    Key loadKey(String alias) throws GenericKeystoreManagerException;

    KeyPair loadKeyPair(String alias) throws GenericKeystoreManagerException;

    Certificate loadCertificate(String alias) throws GenericKeystoreManagerException;

    String getPEMCertificateChain(String alias) throws GenericKeystoreManagerException;

    byte[] getCertificateEncoded(String alias) throws GenericKeystoreManagerException;

    void deleteKeystoreEntry(String alias) throws GenericKeystoreManagerException;

    void deleteAllSecretKeys() throws GenericKeystoreManagerException;

    void deleteAllKeyPairs() throws GenericKeystoreManagerException;
    //CertificateInformation loadCertificateInformation(String alias) throws GenericKeystoreManagerException;

    String generateAlias(Key key) throws IOException, NoSuchAlgorithmException;

    String findAliasOfPublicKey(PublicKey key) throws KeyStoreException, GenericKeystoreManagerException;
}
