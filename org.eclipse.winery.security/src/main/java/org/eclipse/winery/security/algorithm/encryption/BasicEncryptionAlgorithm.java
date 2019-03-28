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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.Key;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class BasicEncryptionAlgorithm implements EncryptionAlgorithm {
    private static final Logger LOGGER = LoggerFactory.getLogger(BasicEncryptionAlgorithm.class);
    
    @Override
    public byte[] encryptBytes(Key key, byte[] plainText) throws InvalidKeyException, IOException {
        byte[] result = null;

        try (ByteArrayInputStream plainTextStream = new ByteArrayInputStream(plainText)) {
            try (InputStream cipherStream = this.encryptStream(key, plainTextStream)) {
                result = IOUtils.toByteArray(cipherStream);
            }
        } catch (IOException e) {
            LOGGER.error("Unexpected IO exception occurred: {}", e.getMessage());
            throw e;
            
        }

        return result;
    }

    @Override
    public byte[] decryptBytes(Key key, byte[] cipherText) throws InvalidKeyException, IOException {
        byte[] result = null;

        try (ByteArrayInputStream cipherTextStream = new ByteArrayInputStream(cipherText)) {
            try (InputStream plainTextStream = this.decryptStream(key, cipherTextStream)) {
                result = IOUtils.toByteArray(plainTextStream);
            }
        } catch (IOException e) {
            LOGGER.error("Unexpected IO exception occurred: {}", e.getMessage());
            throw e;
        }

        return result;
    }
}
