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
package org.eclipse.winery.security.algorithm.encryption;

import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.NoSuchPaddingException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ECIES is a hybrid encryption scheme that uses both asymmetric encryption (Elliptic Curve) with symmetric encryption
 * (AES) with the benefit of a having an efficient execution time for large amounts of data while still having the 
 * convenience of public-key cryptography. 
 */
public class ECIESAlgorithm extends BasicEncryptionAlgorithm {
    /**
     * The name of the encryption scheme (hybrid encryption)
     */
    private static final String NAME = "ECIES";
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ECIESAlgorithm.class);
    
    @Override
    public InputStream encryptStream(Key key, InputStream plainText) throws InvalidKeyException {
        if(!(key instanceof PublicKey)) {
            throw new InvalidKeyException("ECIES algorithm expects a key of type PublicKey for encryption whereas the key passed is of type " + key.getClass().getTypeName());
        }
        
        InputStream result = null;
        
        try {
            final Cipher cipher = Cipher.getInstance(NAME);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            result = new CipherInputStream(plainText, cipher);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            // We should never get into here!
            LOGGER.error("Unexpected cryptographic exception occurred: {}", e.getMessage());
        }
        
        return result;
    }
    

    @Override
    public InputStream decryptStream(Key key, InputStream cipherText) throws InvalidKeyException {
        if(!(key instanceof PrivateKey)) {
            throw new InvalidKeyException("ECIES algorithm expects a key of type PrivateKey for decryption whereas the key passed is of type " + key.getClass().getTypeName());
        }
        
        InputStream result = null;

        try {
            final Cipher cipher = Cipher.getInstance(NAME);
            cipher.init(Cipher.DECRYPT_MODE, key);
            result = new CipherInputStream(cipherText, cipher);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException e) {
            // We should never get into here!
            LOGGER.error("Unexpected cryptographic exception occurred: {}", e.getMessage());
        }

        return result;
    }

}
