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
package org.eclipse.winery.accountability.blockchain.ethereum;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import javax.crypto.SecretKey;

import org.eclipse.winery.accountability.blockchain.BlockchainAccess;
import org.eclipse.winery.accountability.exceptions.BlockchainException;
import org.eclipse.winery.accountability.exceptions.EthereumException;
import org.eclipse.winery.accountability.model.ModelProvenanceElement;
import org.eclipse.winery.accountability.model.authorization.AuthorizationInfo;
import org.eclipse.winery.security.SecurityProcessor;
import org.eclipse.winery.security.SecurityProcessorFactory;
import org.eclipse.winery.security.exceptions.GenericSecurityProcessorException;
import org.eclipse.winery.security.support.enums.AsymmetricEncryptionAlgorithmEnum;

import de.danielbechler.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Contract;

public class EthereumAccessLayer implements BlockchainAccess {
    private static final String NEW_KEYSTORES_DIR_NAME = "ethereum-keystores";
    private static final Logger log = LoggerFactory.getLogger(EthereumAccessLayer.class);

    private Credentials credentials;
    private ProvenanceSmartContractWrapper provenanceContract;
    private AuthorizationSmartContractWrapper authorizationContract;
    private PermissionsSmartContractWrapper permissionsContract;
    private Web3j web3j;

    private EthereumAccessLayer(final String nodeUrl, final String credentialsPath, final String credentialsPassword,
                                final String provenanceSmartContractAddress, final String authorizationSmartContractAddress,
                                final String permissionsSmartContractAddress) throws BlockchainException {
        this.web3j = Web3j.build(new HttpService(nodeUrl));
        unlockCredentials(credentialsPassword, credentialsPath);

        if (Strings.hasText(provenanceSmartContractAddress)) {
            provenanceContract = new ProvenanceSmartContractWrapper(web3j,
                SmartContractProvider.buildProvenanceSmartContract(web3j, this.credentials, provenanceSmartContractAddress));
        }

        if (Strings.hasText(authorizationSmartContractAddress)) {
            authorizationContract = new AuthorizationSmartContractWrapper(web3j,
                SmartContractProvider.buildAuthorizationSmartContract(web3j, this.credentials, authorizationSmartContractAddress));
        }

        if (Strings.hasText(permissionsSmartContractAddress)) {
            permissionsContract = new PermissionsSmartContractWrapper(web3j,
                SmartContractProvider.buildPermissionsSmartContract(web3j, this.credentials, permissionsSmartContractAddress),
                SecurityProcessorFactory.getDefaultSecurityProcessor().getAsymmetricEncryptionAlgorithm());
        }
    }

    public EthereumAccessLayer(Properties configuration) throws BlockchainException {
        this(
            configuration.getProperty("geth-url"),
            configuration.getProperty("ethereum-credentials-file-path"),
            configuration.getProperty("ethereum-password"),
            configuration.getProperty("ethereum-provenance-smart-contract-address"),
            configuration.getProperty("ethereum-authorization-smart-contract-address"),
            configuration.getProperty("ethereum-permissions-smart-contract-address")
        );
    }

    private void unlockCredentials(String password, String fileSource) throws EthereumException {
        try {
            this.credentials = WalletUtils.loadCredentials(password, fileSource);
        } catch (IOException | CipherException e) {
            final String msg = "Error occurred while setting the user credentials for Ethereum. Reason: " +
                e.getMessage();
            log.error(msg);
            throw new EthereumException(msg, e);
        }
    }

    public CompletableFuture<String> saveFingerprint(final String processIdentifier, final String fingerprint) throws BlockchainException {
        if (Objects.isNull(this.provenanceContract)) {
            throw new EthereumException("The provenance smart contract is not instantiated (is the address set?)");
        }
        return this.provenanceContract.saveState(processIdentifier, fingerprint);
    }

    public CompletableFuture<List<ModelProvenanceElement>> getProvenance(final String processIdentifier) throws BlockchainException {
        if (Objects.isNull(this.provenanceContract)) {
            throw new EthereumException("The provenance smart contract is not instantiated (is the address set?)");
        }

        return this.provenanceContract.getProvenance(processIdentifier);
    }

    public CompletableFuture<String> authorize(final String processIdentifier, final String authorizedEthereumAddress,
                                               final String authorizedIdentity) throws BlockchainException {
        if (Objects.isNull(this.authorizationContract)) {
            throw new EthereumException("The authorization smart contract is not instantiated (is the address set?)");
        }

        return this.authorizationContract.authorize(processIdentifier, authorizedEthereumAddress, authorizedIdentity);
    }

    public CompletableFuture<AuthorizationInfo> getAuthorizationTree(final String processIdentifier) throws BlockchainException {
        if (Objects.isNull(this.authorizationContract)) {
            throw new EthereumException("The authorization smart contract is not instantiated (is the address set?)");
        }

        return this.authorizationContract.getAuthorizationTree(processIdentifier);
    }

    @Override
    public CompletableFuture<String> deployAuthorizationSmartContract() {
        return SmartContractProvider
            .deployAuthorizationSmartContract(this.web3j, credentials)
            .thenApply(Contract::getContractAddress);
    }

    @Override
    public CompletableFuture<String> deployProvenanceSmartContract() {
        return SmartContractProvider
            .deployProvenanceSmartContract(this.web3j, credentials)
            .thenApply(Contract::getContractAddress);
    }

    @Override
    public CompletableFuture<String> deployPermissionsSmartContract() {
        return SmartContractProvider
            .deployPermissionsSmartContract(this.web3j, credentials)
            .thenApply(Contract::getContractAddress);
    }

    @Override
    public Path createNewKeystore(String password) throws BlockchainException {
        Path path = Paths.get(System.getProperty("java.io.tmpdir")).resolve(NEW_KEYSTORES_DIR_NAME);
        try {
            if (!Files.exists(path)) {
                Files.createDirectory(path);
                log.debug("Created new temp directory for storing newly created Ethereum keystore (wallet) files {}", path);
            }
            
            // we manually generate the KeyPair used for the wallet because asking web3j to generate it causes a BouncyCastle-conflict
            SecurityProcessor processor = SecurityProcessorFactory.getDefaultSecurityProcessor();
            KeyPair keyPair = processor.generateKeyPair(AsymmetricEncryptionAlgorithmEnum.ECIES_secp256k1);
            final String fileName = WalletUtils.generateWalletFile(password, ECKeyPair.create(keyPair), path.toFile(),true);
            
            return path.resolve(fileName);
        }
        catch (IOException | CipherException | GenericSecurityProcessorException e) {
            String msg = String.format("An error occurred while creating a new keystore file. Reason: %s", e.getMessage());
            log.error(msg, e);
            throw new EthereumException(msg, e);
        } 
    }

    @Override
    public CompletableFuture<Void> setPermissions(String takerAddress, PublicKey takerPublicKey, SecretKey[] permissions)
        throws InvalidKeyException, BlockchainException {
        if (Objects.isNull(this.permissionsContract)) {
            throw new EthereumException("The permissions smart contract is not instantiated (is the address set?)");
        }

        return this.permissionsContract.setPermissions(takerAddress, takerPublicKey, permissions);
    }

    @Override
    public CompletableFuture<Map<String, SecretKey[]>> getMyPermissions(PrivateKey myPrivateKey) throws BlockchainException {
        if (Objects.isNull(this.permissionsContract)) {
            throw new EthereumException("The permissions smart contract is not instantiated (is the address set?)");
        }

        return this.permissionsContract.getMyPermissions(myPrivateKey);
    }

    @Override
    public void close() {
        if (this.web3j != null)
            this.web3j.shutdown();
    }
}
