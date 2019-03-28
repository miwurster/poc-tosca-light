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
package org.eclipse.winery.accountability.blockchain.ethereum;

import java.io.ByteArrayInputStream;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.eclipse.winery.accountability.blockchain.ethereum.generated.Permissions;
import org.eclipse.winery.accountability.blockchain.util.SecretKeyEncoder;
import org.eclipse.winery.security.SecurityProcessor;
import org.eclipse.winery.security.SecurityProcessorFactory;
import org.eclipse.winery.security.algorithm.encryption.EncryptionAlgorithm;
import org.eclipse.winery.security.exceptions.GenericSecurityProcessorException;
import org.eclipse.winery.security.support.enums.AsymmetricEncryptionAlgorithmEnum;

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

    /**
     * Gives a set of SecretKeys (permissions) to a specific taker.
     *
     * @param takerAddress   the unique Ethereum address for the taker.
     * @param takerPublicKey the public key of the taker (used to encrypt the collection of secret keys so only the taker
     *                       can access them)
     * @param permissions    an array of SecretKeys (permissions) to give to the taker
     * @return a completable future that, when successfully executes, indicates that the permissions were successfully
     * given to the designated taker.
     * @throws InvalidKeyException If the public key of the taker is invalid.
     */
    public CompletableFuture<Void> setPermissions(String takerAddress, PublicKey takerPublicKey, SecretKey[] permissions) throws InvalidKeyException {
        EncryptionAlgorithm algorithm = SecurityProcessorFactory.getDefaultSecurityProcessor().getAsymmetricEncryptionAlgorithm();
        byte[] encryptedKeys = algorithm.encryptBytes(takerPublicKey, SecretKeyEncoder.encode(permissions));
        return ((Permissions) contract)
            .setPermission(takerAddress, encryptedKeys)
            .sendAsync()
            .thenAccept(
                receipt -> LOGGER.debug("transaction id for setPermissions operation: {}", receipt.getTransactionHash())
            );
    }

    /**
     * Retrieves all the keys given to me by all givers.
     *
     * @param myPrivateKey used to decrypt the collection of keys I have been given
     * @return a completable future that, when successfully executes, returns a map of givers and given SecretKeys (permissions).
     * Here, givers are identified via their blockchain unique id.
     */
    public CompletableFuture<Map<String, SecretKey[]>> getMyPermissions(PrivateKey myPrivateKey) {
        return ((Permissions) contract)
            .getGivers()
            .sendAsync()
            .thenApply(givers -> ((List<String>) givers)
                .stream()
                .distinct()
                .collect(Collectors.toList()))
            .thenCompose(givers -> queryAllPermissions(givers, myPrivateKey));
    }

    public CompletableFuture<Void> setMyPublicKey(PublicKey publicKey) {
        return ((Permissions) contract)
            .setPublicKey(publicKey.getEncoded())
            .sendAsync()
            .thenAccept(
                receipt -> LOGGER.debug("transaction id for setPublicKey operation: {}", receipt.getTransactionHash())
            );
    }

    public CompletableFuture<PublicKey> getParticipantPublicKey(String address) {
        return ((Permissions) contract)
            .getPublicKey(address)
            .sendAsync()
            .thenApply(keyBytes -> {
                try {
                    SecurityProcessor processor = SecurityProcessorFactory.getDefaultSecurityProcessor();
                    return processor.getX509EncodedPublicKeyFromInputStream(AsymmetricEncryptionAlgorithmEnum.ECIES_secp256k1,
                        new ByteArrayInputStream(keyBytes));
                } catch (GenericSecurityProcessorException e) {
                    LOGGER.error("Failed to recover public key. Reason: {}", e);
                    throw new CompletionException(e);
                }
            });
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
