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
package org.eclipse.winery.accountability.blockchain.util;

import java.util.ArrayList;
import java.util.List;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.eclipse.winery.security.support.enums.SymmetricEncryptionAlgorithmEnum;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts a collection of SecretKeys into an array of bytes, and vice versa.
 */
public abstract class SecretKeyEncoder {
    /**
     * Enum to map the supported symmetric keys to their byte encodings
     */
    enum KeyEncodingEnum {
        AES256Encoding(SymmetricEncryptionAlgorithmEnum.AES256, (byte) 0),
        AES512Encoding(SymmetricEncryptionAlgorithmEnum.AES512, (byte) 1);

        private SymmetricEncryptionAlgorithmEnum key;
        private byte byteEncoding;

        KeyEncodingEnum(SymmetricEncryptionAlgorithmEnum key, byte byteEncoding) {
            this.key = key;
            this.byteEncoding = byteEncoding;
        }

        public static byte getEncodingByKey(SymmetricEncryptionAlgorithmEnum key) throws IllegalArgumentException {
            for (KeyEncodingEnum a : values()) {
                if (key.equals(a.key))
                    return a.byteEncoding;
            }

            throw new IllegalArgumentException("The provided key type does not have a byte encoding specified: "
                + key.getName());
        }

        public static SymmetricEncryptionAlgorithmEnum getKeyByEncoding(byte encoding) throws IllegalArgumentException {
            for (KeyEncodingEnum a : values()) {
                if (encoding == a.byteEncoding)
                    return a.key;
            }

            throw new IllegalArgumentException("The provided encoding does not correspond to any symmetric key type: "
                + encoding);
        }
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretKeyEncoder.class);

    public static byte[] encode(SecretKey[] keysToEncode) throws IllegalArgumentException {

        try {
            List<Byte> result = new ArrayList<>();
            SymmetricEncryptionAlgorithmEnum currentKeyType;
            byte[] currentKeyMaterial;
            byte currentEncoding;

            for (SecretKey currentKey : keysToEncode) {
                currentKeyMaterial = currentKey.getEncoded();
                currentKeyType = SymmetricEncryptionAlgorithmEnum.findKey(currentKey.getAlgorithm(),
                    currentKeyMaterial.length * 8);
                currentEncoding = KeyEncodingEnum.getEncodingByKey(currentKeyType);
                result.add(currentEncoding);

                for (byte b : currentKeyMaterial) {
                    result.add(b);
                }
            }

            return ArrayUtils.toPrimitive(result.toArray(new Byte[0]));
        } catch (IllegalArgumentException e) {
            LOGGER.error("Key not supported!", e);
            throw e;
        }
    }

    public static SecretKey[] decode(byte[] encodedKeys) throws IllegalArgumentException {

        try {
            int position = 0;
            List<SecretKeySpec> result = new ArrayList<>();
            SymmetricEncryptionAlgorithmEnum currentKeyType;
            byte[] currentKeyMaterial;
            byte currentEncoding;
            SecretKeySpec currentKeySpec;

            while (position < encodedKeys.length) {
                currentEncoding = encodedKeys[position];
                currentKeyType = KeyEncodingEnum.getKeyByEncoding(currentEncoding);
                currentKeyMaterial = new byte[currentKeyType.getkeySizeInBits() / 8];
                System.arraycopy(encodedKeys, position + 1,
                    currentKeyMaterial, 0, currentKeyMaterial.length);
                currentKeySpec = new SecretKeySpec(currentKeyMaterial, currentKeyType.getName());
                result.add(currentKeySpec);
                position += currentKeyMaterial.length + 1;
            }

            return result.toArray(new SecretKeySpec[0]);
        } catch (IllegalArgumentException e) {
            LOGGER.error("Encoding not supported", e);
            throw e;
        }
    }
}
