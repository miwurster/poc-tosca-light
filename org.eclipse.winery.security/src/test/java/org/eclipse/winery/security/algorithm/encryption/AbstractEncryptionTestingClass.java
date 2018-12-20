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

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.Key;

import org.eclipse.winery.security.algorithm.AbstractSecurityTestClass;

import org.apache.commons.io.Charsets;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public abstract class AbstractEncryptionTestingClass extends AbstractSecurityTestClass {
    private static final String MESSAGE = "This is an arbitrary message used for testing purposes!";
    protected EncryptionAlgorithm algorithm;
    
    @Test
    void encryptDecryptStream() throws IOException, InvalidKeyException {
        try(InputStream originalPlainTextStream = IOUtils.toInputStream(MESSAGE)) {
            try(InputStream cipherTextStream = this.algorithm.encryptStream(this.getEncryptionKey(), originalPlainTextStream)) {
                try(InputStream decryptedPlainTextStream = this.algorithm.decryptStream(this.getDecryptionKey(), cipherTextStream)) {
                    final String resultingMessage = IOUtils.toString(decryptedPlainTextStream);
                    Assertions.assertEquals(MESSAGE, resultingMessage);
                }
            }
        }
    }

    @Test
    void encryptDecryptByteArray() throws IOException, InvalidKeyException {
        byte[] originalPlainTextBytes = MESSAGE.getBytes();
        byte[] encryptedBytes = this.algorithm.encryptBytes(this.getEncryptionKey(), originalPlainTextBytes);
        byte[] decryptedBytes = this.algorithm.decryptBytes(this.getDecryptionKey(), encryptedBytes);
        String result = IOUtils.toString(decryptedBytes, Charsets.UTF_8.name());
        Assertions.assertEquals(MESSAGE, result);
    }
    
    protected abstract Key getEncryptionKey(); 
    
    protected abstract Key getDecryptionKey();
}
