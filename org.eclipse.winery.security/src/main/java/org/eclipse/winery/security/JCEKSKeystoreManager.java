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

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Objects;

import javax.crypto.SecretKey;

import org.eclipse.winery.security.datatypes.CertificateInformation;
import org.eclipse.winery.security.datatypes.DistinguishedName;
import org.eclipse.winery.security.datatypes.KeyEntityInformation;
import org.eclipse.winery.security.datatypes.KeyPairInformation;
import org.eclipse.winery.security.datatypes.KeyType;
import org.eclipse.winery.security.datatypes.KeystoreContentsInformation;
import org.eclipse.winery.security.exceptions.GenericKeystoreManagerException;
import org.eclipse.winery.security.exceptions.GenericSecurityProcessorException;
import org.eclipse.winery.security.support.enums.AsymmetricEncryptionAlgorithmEnum;
import org.eclipse.winery.security.support.enums.DigestAlgorithmEnum;
import org.eclipse.winery.security.support.enums.SymmetricEncryptionAlgorithmEnum;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JCEKSKeystoreManager implements KeystoreManager {

    public static final String KEYSTORE_NAME = "winery-keystore.jceks";
    private static final String KEYSTORE_PASSWORD = "password";
    private static final String KEYSTORE_TYPE = "JCEKS";
    private static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    private static final String END_CERT = "-----END CERTIFICATE-----";
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");
    private static final Logger LOGGER = LoggerFactory.getLogger(JCEKSKeystoreManager.class);

    private KeyStore keystore;
    private String keystorePath;

    public JCEKSKeystoreManager(String absoluteKeystorePath) {
        loadKeystore(absoluteKeystorePath);
    }

    private static int getKeyLength(final Key pk) {
        return pk.getEncoded().length * 8;
    }

    private void loadKeystore(String absoluteKeystorePath) {

        this.keystorePath = absoluteKeystorePath;
        try {
            KeyStore keystore = KeyStore.getInstance(KEYSTORE_TYPE);

            if (!Files.exists(Paths.get(this.keystorePath))) {
                keystore.load(null, null);
                keystore.store(new FileOutputStream(keystorePath), KEYSTORE_PASSWORD.toCharArray());
            }

            keystore.load(new FileInputStream(keystorePath), KEYSTORE_PASSWORD.toCharArray());
            this.keystore = keystore;
        } catch (Exception e) {
            LOGGER.error("Could not generate JCEKS keystore", e);
        }
    }

    private Key loadKey(String alias, KeyType type) throws GenericKeystoreManagerException {
        try {
            if (type != KeyType.PUBLIC) {
                Key key = this.keystore.getKey(alias, KEYSTORE_PASSWORD.toCharArray());
                if ((key instanceof SecretKey && KeyType.SECRET.equals(type)) || (key instanceof PrivateKey && KeyType.PRIVATE.equals(type))) {
                    return key;
                } else {
                    throw new UnrecoverableKeyException("Key with given alias does not exist");
                }
            } else {
                Certificate cert = this.keystore.getCertificate(alias);
                return cert.getPublicKey();
            }
        } catch (UnrecoverableKeyException | NoSuchAlgorithmException | KeyStoreException e) {
            LOGGER.error("Error loading a key using the provided alias", e.getMessage());
            throw new GenericKeystoreManagerException("Error loading a key using the provided alias");
        }
    }

    private KeyEntityInformation generateKeyEntityInformation(String alias, Key key) {
        return new KeyEntityInformation
            .Builder(alias, key.getAlgorithm(), key.getFormat())
            .keySizeInBits(key.getEncoded().length)
            .base64Key(key.getEncoded())
            .build();
    }

    private KeyPairInformation generateKeyPairInformation(KeyEntityInformation privateKey, Certificate certificate) throws GenericKeystoreManagerException {
        PublicKey publicKey = certificate.getPublicKey();
        KeyEntityInformation publicKeyInfo = this.generateKeyEntityInformation(privateKey.getAlias(), publicKey);
        CertificateInformation certificateInformation = this.buildCertificateInformation(privateKey.getAlias(), certificate);

        return new KeyPairInformation(privateKey, publicKeyInfo, certificateInformation);
    }

    private CertificateInformation buildCertificateInformation(String alias, Certificate cert) throws GenericKeystoreManagerException {
        if (Objects.nonNull(cert)) {
            try {
                X509Certificate x = (X509Certificate) cert;
                return new CertificateInformation.Builder(alias)
                    .serialNumber(x.getSerialNumber().toString())
                    .sigAlgName(x.getSigAlgName())
                    .subjectDN(x.getSubjectX500Principal().getName())
                    .issuerDN(x.getIssuerX500Principal().getName())
                    .notBefore(x.getNotBefore())
                    .notAfter(x.getNotAfter())
                    .pemEncodedCertificate(constructPEMCertificate(x))
                    .build();
            } catch (GenericKeystoreManagerException e) {
                e.printStackTrace();
            }
        }

        throw new GenericKeystoreManagerException("Error retrieving certificate with alias, " + alias);
    }

    private String constructPEMCertificate(Certificate x509) throws GenericKeystoreManagerException {
        try {
            final Base64.Encoder encoder = Base64.getMimeEncoder(64, LINE_SEPARATOR.getBytes());
            final byte[] rawCrtText = x509.getEncoded();
            final String encodedCertText = new String(encoder.encode(rawCrtText));
            final StringBuilder sb = new StringBuilder();
            sb.append(BEGIN_CERT);
            sb.append(LINE_SEPARATOR);
            sb.append(encodedCertText);
            sb.append(LINE_SEPARATOR);
            sb.append(END_CERT);

            return sb.toString();
        } catch (CertificateEncodingException e) {
            throw new GenericKeystoreManagerException("Error converting a certificate to PEM string");
        }
    }

    private String constructPEMCertificateChain(Certificate[] certs) throws GenericKeystoreManagerException {
        final StringBuilder sb = new StringBuilder();
        for (Certificate c : certs) {
            sb.append(constructPEMCertificate(c));
        }
        return sb.toString();
    }

    private boolean publicKeyEquals(PublicKey key1, PublicKey key2) {
        return Arrays.equals(key1.getEncoded(), key2.getEncoded());
    }

    @Override
    public KeyEntityInformation storeKey(String alias, Key key) throws GenericKeystoreManagerException {
        try {
            keystore.setKeyEntry(alias, key, KEYSTORE_PASSWORD.toCharArray(), null);
            keystore.store(new FileOutputStream(this.keystorePath), KEYSTORE_PASSWORD.toCharArray());
            return generateKeyEntityInformation(alias, key);
        } catch (CertificateException | NoSuchAlgorithmException | IOException | KeyStoreException e) {
            LOGGER.error("Error while storing a secret key", e);
            throw new GenericKeystoreManagerException("Could not store the provided secret key");
        }
    }

    @Override
    public KeyPairInformation storeKeyPair(String alias, PrivateKey privateKey, Certificate certificate) throws GenericKeystoreManagerException {
        try {
            if (Objects.nonNull(certificate)) {
                // TODO: validate certificate against private key            
                keystore.setKeyEntry(alias, privateKey, KEYSTORE_PASSWORD.toCharArray(), new Certificate[] {certificate});
                keystore.store(new FileOutputStream(this.keystorePath), KEYSTORE_PASSWORD.toCharArray());
                KeyEntityInformation privateKeyInfo = this.generateKeyEntityInformation(alias, privateKey);

                return this.generateKeyPairInformation(privateKeyInfo, certificate);
            } else {
                throw new GenericKeystoreManagerException("No certificates were provided with the private key");
            }
        } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException e) {
            LOGGER.error("Could not store the provided keypair", e);
            throw new GenericKeystoreManagerException("Could not store the provided keypair");
        }
    }

    @Override
    public KeyPairInformation storeKeyPair(String alias, KeyPair keyPair, DistinguishedName dn) throws GenericKeystoreManagerException, GenericSecurityProcessorException {
        if (Objects.nonNull(dn)) {
            Certificate certificate = SecurityProcessorFactory.getDefaultSecurityProcessor()
                .generateSelfSignedX509Certificate(keyPair, dn);
            return this.storeKeyPair(alias, keyPair.getPrivate(), certificate);
        } else {
            final GenericKeystoreManagerException e = new GenericKeystoreManagerException("No distinguished name for the certificate is supplied");
            LOGGER.error("Could not store the provided keypair. Reason: {}", e);
            throw e;
        }
    }

    @Override
    public Key loadKey(String alias) throws GenericKeystoreManagerException {
        try {
            return this.keystore.getKey(alias, KEYSTORE_PASSWORD.toCharArray());
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            LOGGER.error("Error loading a key using the provided alias", e.getMessage());
            throw new GenericKeystoreManagerException("Error loading a key using the provided alias");
        }
    }

    @Override
    public KeyPair loadKeyPair(String alias) throws GenericKeystoreManagerException {
        try {
            Key key = keystore.getKey(alias, KEYSTORE_PASSWORD.toCharArray());
            if (key instanceof PrivateKey) {
                Certificate cert = keystore.getCertificate(alias);
                PublicKey publicKey = cert.getPublicKey();
                return new KeyPair(publicKey, (PrivateKey) key);
            } else {
                throw new UnrecoverableKeyException("Keypair with given alias does not exist");
            }
        } catch (KeyStoreException | NoSuchAlgorithmException | UnrecoverableKeyException e) {
            LOGGER.error("Error loading a key using the provided alias", e.getMessage());
            throw new GenericKeystoreManagerException("Error loading a key using the provided alias");
        }
    }

    @Override
    public KeyEntityInformation getKey(String alias, KeyType type) throws GenericKeystoreManagerException {
        try {
            Key key = loadKey(alias, type);
            return this.generateKeyEntityInformation(alias, key);
        } catch (GenericKeystoreManagerException e) {
            throw new GenericKeystoreManagerException("Error loading a key using the provided alias");
        }
    }

    @Override
    public byte[] getKeyEncoded(String alias, KeyType type) throws GenericKeystoreManagerException {
        try {
            Key key = loadKey(alias, type);
            return key.getEncoded();
        } catch (GenericKeystoreManagerException e) {
            throw new GenericKeystoreManagerException("Error loading a key using the provided alias");
        }
    }

    @Override
    public String getPEMCertificateChain(String alias) throws GenericKeystoreManagerException {
        try {
            final Certificate[] certs = this.keystore.getCertificateChain(alias);
            return constructPEMCertificateChain(certs);
        } catch (KeyStoreException e) {
            throw new GenericKeystoreManagerException("Error loading a certificate using the provided alias");
        }
    }

    @Override
    public byte[] getCertificateEncoded(String alias) throws GenericKeystoreManagerException {
        try {
            return getPEMCertificateChain(alias).getBytes();
        } catch (GenericKeystoreManagerException e) {
            throw new GenericKeystoreManagerException("Error loading a certificate using the provided alias");
        }
    }

    @Override
    public KeyPairInformation getKeyPairData(String alias) throws GenericKeystoreManagerException {
        KeyEntityInformation privateKeyInfo = getKey(alias, KeyType.PRIVATE);
        Certificate certificate = loadCertificate(alias);

        return this.generateKeyPairInformation(privateKeyInfo, certificate);
    }

    @Override
    public Certificate storeCertificate(String alias, InputStream is) throws GenericKeystoreManagerException {
        try {
            Certificate c = CertificateFactory.getInstance("X509").generateCertificate(is);
            keystore.setCertificateEntry(alias, c);
            try (FileOutputStream fos = new FileOutputStream(this.keystorePath)) {
                keystore.store(fos, KEYSTORE_PASSWORD.toCharArray());
            } catch (NoSuchAlgorithmException | IOException e) {
                LOGGER.error("Error while storing a certificate", e);
                throw new GenericKeystoreManagerException("Could not store the provided certificate");
            }
            return c;
        } catch (CertificateException | KeyStoreException e) {
            LOGGER.error("Error while storing a certificate", e);
            throw new GenericKeystoreManagerException("Could not store the provided certificate");
        }
    }

    @Override
    public Certificate storeCertificate(String alias, Certificate c) throws GenericKeystoreManagerException {
        try {
            keystore.setCertificateEntry(alias, c);
            keystore.store(new FileOutputStream(this.keystorePath), KEYSTORE_PASSWORD.toCharArray());
            return c;
        } catch (CertificateException | NoSuchAlgorithmException | IOException | KeyStoreException e) {
            LOGGER.error("Error while storing a certificate", e);
            throw new GenericKeystoreManagerException("Could not store the provided certificate");
        }
    }

    @Override
    public Certificate storeCertificate(String alias, String pemEncodedString) throws GenericKeystoreManagerException {
        try {
            InputStream stream = new ByteArrayInputStream(pemEncodedString.getBytes(StandardCharsets.UTF_8));
            CertificateFactory cf = CertificateFactory.getInstance("X.509");

            return cf.generateCertificate(stream);
        } catch (CertificateException e) {
            LOGGER.error("Error while storing a certificate", e);
            throw new GenericKeystoreManagerException("Could not store the provided certificate");
        }
    }

    @Override
    public Certificate loadCertificate(String alias) throws GenericKeystoreManagerException {
        try {
            return this.keystore.getCertificate(alias);
        } catch (KeyStoreException e) {
            LOGGER.error("Error while loading a certificate", e);
            throw new GenericKeystoreManagerException("Could not load the stored certificate");
        }
    }

    @Override
    public int getKeystoreSize() throws GenericKeystoreManagerException {
        try {
            return this.keystore.size();
        } catch (KeyStoreException e) {
            LOGGER.error("Error while checking the size of the keystore", e);
            throw new GenericKeystoreManagerException(e.getMessage());
        }
    }

    @Override
    public void deleteKeystoreEntry(String alias) throws GenericKeystoreManagerException {
        try {
            this.keystore.deleteEntry(alias);
            this.keystore.store(new FileOutputStream(this.keystorePath), KEYSTORE_PASSWORD.toCharArray());
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
            throw new GenericKeystoreManagerException(e.getMessage());
        }
    }

    @Override
    public void deleteAllSecretKeys() throws GenericKeystoreManagerException {
        try {
            for (KeyEntityInformation secretKey : getKeys(false)) {
                deleteKeystoreEntry(secretKey.getAlias());
            }
            this.keystore.store(new FileOutputStream(this.keystorePath), KEYSTORE_PASSWORD.toCharArray());
        } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException | GenericKeystoreManagerException e) {
            e.printStackTrace();
            throw new GenericKeystoreManagerException(e.getMessage());
        }
    }

    @Override
    public void deleteAllKeyPairs() throws GenericKeystoreManagerException {
        try {
            for (KeyPairInformation keyPair : getKeyPairs()) {
                deleteKeystoreEntry(keyPair.getPrivateKey().getAlias());
            }
            this.keystore.store(new FileOutputStream(this.keystorePath), KEYSTORE_PASSWORD.toCharArray());
        } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException | GenericKeystoreManagerException e) {
            e.printStackTrace();
            throw new GenericKeystoreManagerException(e.getMessage());
        }
    }

    @Override
    public Collection<KeyPairInformation> getKeyPairs() throws GenericKeystoreManagerException {
        Enumeration<String> aliases;
        Collection<KeyPairInformation> keypairs = new ArrayList<>();
        try {
            aliases = this.keystore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (this.keystore.isKeyEntry(alias)) {
                    Key key;
                    try {
                        key = this.keystore.getKey(alias, KEYSTORE_PASSWORD.toCharArray());
                        if ((key instanceof PrivateKey)) {
                            Certificate certificate = loadCertificate(alias);
                            KeyEntityInformation privateKeyInfo = this.generateKeyEntityInformation(alias, key);
                            KeyPairInformation kp = this.generateKeyPairInformation(privateKeyInfo, certificate);
                            keypairs.add(kp);
                        }
                    } catch (NoSuchAlgorithmException | UnrecoverableKeyException e) {
                        LOGGER.error("Error retrieving keypairs list", e);
                        throw new GenericKeystoreManagerException("Error retrieving keypairs list");
                    } catch (GenericKeystoreManagerException exception) {
                        LOGGER.error("Error retrieving keypairs list", exception);
                        throw exception;
                    }
                }
            }
        } catch (KeyStoreException e) {
            e.printStackTrace();
        }
        return keypairs;
    }

    @Override
    public Collection<CertificateInformation> getCertificates() throws GenericKeystoreManagerException {
        Enumeration<String> aliases;
        Collection<CertificateInformation> certificates = new ArrayList<>();
        try {
            aliases = this.keystore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                Certificate cert = this.keystore.getCertificate(alias);
                if (Objects.nonNull(cert)) {
                    CertificateInformation ci = buildCertificateInformation(alias, cert);
                    certificates.add(ci);
                }
            }
            return certificates;
        } catch (KeyStoreException e) {
            LOGGER.error("Error retrieving certificates list", e);
            throw new GenericKeystoreManagerException("Error retrieving certificates list");
        }
    }

    @Override
    public CertificateInformation getCertificate(String alias) throws GenericKeystoreManagerException {
        Certificate cert;
        try {
            cert = this.keystore.getCertificate(alias);
            return buildCertificateInformation(alias, cert);
        } catch (KeyStoreException e) {
            throw new GenericKeystoreManagerException("Error retrieving certificate with alias, " + alias);
        }
    }

    @Override
    public KeystoreContentsInformation getKeystoreContent() throws GenericKeystoreManagerException {
        try {
            Collection<CertificateInformation> certs = getCertificates();
            Collection<KeyEntityInformation> keys = getKeys(false);
            Collection<KeyPairInformation> keypairs = getKeyPairs();
            return new KeystoreContentsInformation(keys, keypairs, certs);
        } catch (GenericKeystoreManagerException e) {
            throw new GenericKeystoreManagerException(e.getMessage());
        }
    }

    @Override
    public boolean keystoreExists() {
        return keystore != null;
    }

    @Override
    public boolean entityExists(String alias) {
        try {
            return keystore.containsAlias(alias);
        } catch (KeyStoreException e) {
            LOGGER.error("Error while checking if entity exists", e.getMessage());
        }
        return false;
    }

    @Override
    public Collection<SymmetricEncryptionAlgorithmEnum> getSymmetricAlgorithms() {
        return Arrays.asList(SymmetricEncryptionAlgorithmEnum.values());
    }

    @Override
    public Collection<AsymmetricEncryptionAlgorithmEnum> getAsymmetricAlgorithms() {
        return Arrays.asList(AsymmetricEncryptionAlgorithmEnum.values());
    }

    @Override
    public Collection<KeyEntityInformation> getKeys(boolean withKeyEncoded) {
        Enumeration<String> aliases;
        Collection<KeyEntityInformation> keys = new ArrayList<>();
        try {
            aliases = this.keystore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (this.keystore.isKeyEntry(alias)) {
                    Key key;
                    try {
                        key = this.keystore.getKey(alias, KEYSTORE_PASSWORD.toCharArray());
                        if ((key instanceof SecretKey)) {
                            if (!withKeyEncoded) {
                                keys.add(new KeyEntityInformation
                                    .Builder(alias, key.getAlgorithm(), key.getFormat())
                                    .keySizeInBits(key.getEncoded().length)
                                    .build()
                                );
                            } else {
                                keys.add(this.generateKeyEntityInformation(alias, key));
                            }
                        }
                    } catch (NoSuchAlgorithmException | UnrecoverableKeyException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (KeyStoreException e) {
            LOGGER.error("Could not retrieve a list of secret keys", e);
        }
        return keys;
    }

    @Override
    public String generateAlias(Key key) throws IOException, NoSuchAlgorithmException {
        return SecurityProcessorFactory
            .getDefaultSecurityProcessor()
            .getChecksum(new ByteArrayInputStream(key.getEncoded()),
                DigestAlgorithmEnum.SHA256);
    }

    @Override
    public String findAliasOfPublicKey(PublicKey key) throws KeyStoreException, GenericKeystoreManagerException {
        KeyPair current;
        Collection<KeyPairInformation> allKeyPairs = this.getKeyPairs();
        
        for (KeyPairInformation keyPairInformation : allKeyPairs) {
            current = this.loadKeyPair(keyPairInformation.getPublicKey().getAlias());
            if (publicKeyEquals(current.getPublic(), key))
                return keyPairInformation.getPublicKey().getAlias();
        }

        return null;
    }
    
} 
    
