/********************************************************************************
 * Copyright (c) 2018 Contributors to the Eclipse Foundation
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

import org.eclipse.winery.repository.security.csar.datatypes.*;
import org.eclipse.winery.repository.security.csar.exceptions.GenericKeystoreManagerException;
import org.eclipse.winery.repository.security.csar.support.AsymmetricEncryptionAlgorithm;
import org.eclipse.winery.repository.security.csar.support.SymmetricEncryptionAlgorithm;

import java.io.InputStream;
import java.security.*;
import java.security.cert.Certificate;
import java.util.Collection;

public interface KeystoreManager {

    boolean keystoreExists();
    
    boolean entityExists(String alias);
    
    Collection<SymmetricEncryptionAlgorithm> getSymmetricAlgorithms();
    
    Collection<AsymmetricEncryptionAlgorithm> getAsymmetricAlgorithms();
    
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
    
}
