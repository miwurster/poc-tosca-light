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
package org.eclipse.winery.accountability.blockchain.ethereum;

import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import javax.crypto.SecretKey;

import org.eclipse.winery.accountability.blockchain.ethereum.generated.Permissions;
import org.eclipse.winery.accountability.blockchain.util.SecretKeyEncoder;
import org.eclipse.winery.security.algorithm.encryption.ECIESAlgorithm;
import org.eclipse.winery.security.algorithm.encryption.EncryptionAlgorithm;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.Web3j;
import org.web3j.tx.Contract;

public class PermissionsSmartContractWrapper extends SmartContractWrapper {
    private static final Logger LOGGER = LoggerFactory.getLogger(PermissionsSmartContractWrapper.class);
    private EncryptionAlgorithm algorithm;

    PermissionsSmartContractWrapper(Web3j web3j, Contract contract, EncryptionAlgorithm algorithm) {
        super(web3j, contract);
        this.algorithm = algorithm;
    }

    public CompletableFuture<Void> setPermissions(String takerAddress, PublicKey takerPublicKey, SecretKey[] permissions) throws InvalidKeyException {
        ECIESAlgorithm algorithm = new ECIESAlgorithm();
        byte[] encryptedKeys = algorithm.encryptBytes(takerPublicKey, SecretKeyEncoder.encode(permissions));
        return ((Permissions) contract)
            .setPermission(takerAddress, encryptedKeys)
            .sendAsync()
            .thenAccept(
                receipt -> LOGGER.debug("transaction id for setPermissions operation: {}", receipt.getTransactionHash())
            );
    }

    public CompletableFuture<Map<String, SecretKey[]>> getMyPermissions(PrivateKey myPrivateKey) {
        return ((Permissions) contract)
            .getGivers()
            .sendAsync()
            .thenCompose(givers -> queryAllPermissions(givers, myPrivateKey));
    }

    private CompletableFuture<Map<String, SecretKey[]>> queryAllPermissions(List givers, PrivateKey myPrivateKey) {
        List<CompletableFuture<Pair<String, byte[]>>> futuresToJoin = new ArrayList<>();

        for (Object giver : givers) {
            futuresToJoin.add(((Permissions) contract)
                .getPermission((String) giver)
                .sendAsync()
                .thenApply(bytes -> new ImmutablePair<>((String) giver, bytes)));
        }

        return CompletableFuture
            .allOf(futuresToJoin.toArray(new CompletableFuture[0]))
            .thenCompose(ignore -> {
                try {
                    Map<String, SecretKey[]> result = new HashMap<>();

                    for (CompletableFuture<Pair<String, byte[]>> future : futuresToJoin) {
                        Pair<String, byte[]> currentResult = future.join();
                        byte[] decryptedKey = algorithm.decryptBytes(myPrivateKey, currentResult.getValue());
                        SecretKey[] currentKeys = SecretKeyEncoder.decode(decryptedKey);
                        result.put(currentResult.getKey(), currentKeys);
                    }

                    return CompletableFuture.supplyAsync(() -> result);
                } catch (CompletionException e) {
                    LOGGER.error("Error occurred while retrieving permissions", e);
                    throw e;
                } catch (Exception e) {
                    LOGGER.error("Error occurred while retrieving permissions", e);
                    throw new CompletionException(e);
                }
            });
    }
}
