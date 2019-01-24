/*******************************************************************************
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

package org.eclipse.winery.accountability.blockchain;

import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.crypto.SecretKey;

import org.eclipse.winery.accountability.exceptions.BlockchainException;
import org.eclipse.winery.accountability.model.ModelProvenanceElement;
import org.eclipse.winery.accountability.model.authorization.AuthorizationInfo;

public interface BlockchainAccess {

    /**
     * Saves a version of the collaborative resource in the blockchain
     *
     * @param processIdentifier the identifier of the collaboration process
     * @param fingerprint       the fingerprint of the state of the collaborative resource we want to store
     * @return a completable future that, when completed, returns the blockchain address of the transaction that contains
     * the stored version.
     */
    CompletableFuture<String> saveFingerprint(final String processIdentifier, final String fingerprint) throws BlockchainException;

    /**
     * Gets the history of a given collaboration process
     *
     * @param processIdentifier the identifier of the collaboration process
     * @return a completable future that, when completed, returns a list containing the historic versions of the
     * collaborative resource.
     */
    CompletableFuture<List<ModelProvenanceElement>> getProvenance(final String processIdentifier) throws BlockchainException;

    /**
     * Authorizes a new participant for the given collaboration process.
     *
     * @param processIdentifier         the identifier of the collaboration process
     * @param authorizedEthereumAddress the blockchain address of the participant we want to authorize
     * @param authorizedIdentity        the real-world-identity of the participant we want to authorize
     * @return a completable future that, when completed, returns the blockchain address of the transaction that contains
     * the authorization information.
     */
    CompletableFuture<String> authorize(final String processIdentifier, final String authorizedEthereumAddress,
                                        final String authorizedIdentity) throws BlockchainException;

    /**
     * Gets the authorization tree of a given process which allows various querying capabilities.
     *
     * @param processIdentifier the identifier of the collaboration process
     * @return a completable future that, when completed, returns the authorization tree.
     */
    CompletableFuture<AuthorizationInfo> getAuthorizationTree(final String processIdentifier) throws BlockchainException;

    /**
     * Deploys the Authorization smart contract to the active blockchain network
     *
     * @return a completable future that, when completed, returns the address of the contract.
     */
    CompletableFuture<String> deployAuthorizationSmartContract();

    /**
     * Deploys the provenance smart contract to the active blockchain network
     *
     * @return a completable future that, when completed, returns the address of the contract.
     */
    CompletableFuture<String> deployProvenanceSmartContract();

    /**
     * Deploys the permissions smart contract to the active blockchain network
     *
     * @return a completable future that, when completed, returns the address of the contract.
     */
    CompletableFuture<String> deployPermissionsSmartContract();

    /**
     * Creates a new blockchain keystore file
     * @param password a password to secure the file with
     * @return the full path of the generated keystore file.
     * @throws BlockchainException when an error occurs while creating the new keystore file. 
     */
    Path createNewKeystore(String password) throws BlockchainException;

    /**
     * Sets the permissions given from the active user to a certain address.
     *
     * @param takerAddress   the address to set the permissions for.
     * @param takerPublicKey the public key of the receiver (used to encrypt the set of the given permissions).
     * @param permissions    the set secret keys (permissions) to give.
     * @return a completable future the finishes when the transaction to set the permissions succeeds.
     * @throws InvalidKeyException if encrypting the permissions fails due to invalid public key format.
     */
    CompletableFuture<Void> setPermissions(String takerAddress, PublicKey takerPublicKey, SecretKey[] permissions) throws InvalidKeyException, BlockchainException;

    /**
     * Gets the set of permissions given to the active user.
     *
     * @param myPrivateKey The private key to decrypt the set of permissions given to the current user.
     * @return a completable future that, when completed, returns a map of the giver addresses associated
     * to the set of permissions they have given to the active user.
     */
    CompletableFuture<Map<String, SecretKey[]>> getMyPermissions(PrivateKey myPrivateKey) throws BlockchainException;
    

    /**
     * Releases resources relevant to this instance
     */
    void close();
}
