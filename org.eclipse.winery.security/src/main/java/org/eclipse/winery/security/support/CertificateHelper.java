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
package org.eclipse.winery.security.support;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.winery.security.datatypes.DistinguishedName;
import org.eclipse.winery.security.exceptions.GenericSecurityProcessorException;
import org.eclipse.winery.security.support.enums.SignatureAlgorithmEnum;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.AuthorityKeyIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CertificateHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateHelper.class);

    public static Certificate[] getX509Certificates(InputStream certInputStream) throws GenericSecurityProcessorException {
        try {
            return createCertificates(certInputStream);
        } catch (Exception e) {
            LOGGER.error("Error processing the provided X509 certificate", e);
            throw new GenericSecurityProcessorException("Error processing the provided X509 certificate chain", e);
        }
    }

    public static Certificate generateSelfSignedX509Certificate(KeyPair keypair, DistinguishedName distinguishedName) throws GenericSecurityProcessorException {
        String signatureAlgorithm;
        try {
            signatureAlgorithm = SignatureAlgorithmEnum.getDefaultOptionForAlgorithmAsString(keypair.getPrivate().getAlgorithm());
        } catch (IllegalArgumentException e) {
            LOGGER.error("Signature algorithm for keypair algorithm is not found", e);
            throw new GenericSecurityProcessorException("Signature algorithm for keypair algorithm is not found", e);
        }
        try {
            X500Name dn = buildX500Name(distinguishedName);

            long now = System.currentTimeMillis();
            Date startDate = new Date(now);
            BigInteger certSerialNumber = new BigInteger(Long.toString(now));
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(startDate);
            calendar.add(Calendar.YEAR, 1); // <-- 1 Yr validity

            Date endDate = calendar.getTime();

            ContentSigner sigGen = new JcaContentSignerBuilder(signatureAlgorithm)
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .build(keypair.getPrivate());

            X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
                dn,
                certSerialNumber,
                startDate,
                endDate,
                dn,
                keypair.getPublic()
            ).addExtension(new ASN1ObjectIdentifier("2.5.29.35"), false, new AuthorityKeyIdentifier(keypair.getPublic().getEncoded())
            ).addExtension(new ASN1ObjectIdentifier("2.5.29.19"), false, new BasicConstraints(false) // true if it is allowed to sign other certs
            ).addExtension(new ASN1ObjectIdentifier("2.5.29.15"), true, new X509KeyUsage(
                X509KeyUsage.digitalSignature |
                    X509KeyUsage.nonRepudiation |
                    X509KeyUsage.keyEncipherment |
                    X509KeyUsage.dataEncipherment)
            );

            X509CertificateHolder certHolder = certBuilder.build(sigGen);

            return new JcaX509CertificateConverter().setProvider(BouncyCastleProvider.PROVIDER_NAME).getCertificate(certHolder);
        } catch (OperatorCreationException | CertIOException | CertificateException e) {
            LOGGER.error("Error generating a self-signed certificate", e);
            throw new GenericSecurityProcessorException("Error generating a self-signed certificate", e);
        }
    }

    private static X500Name buildX500Name(DistinguishedName distinguishedName) throws GenericSecurityProcessorException {
        if (distinguishedName.isValid()) {
            Map<String, String> rdns = distinguishedName.getIdentityData();

            X500NameBuilder builder = new X500NameBuilder(BCStyle.INSTANCE);
            builder.addRDN(BCStyle.CN, rdns.get("CN"));
            builder.addRDN(BCStyle.O, rdns.get("O"));
            builder.addRDN(BCStyle.C, rdns.get("C"));
            if (rdns.containsKey("OU")) {
                builder.addRDN(BCStyle.OU, rdns.get("OU"));
            }
            if (rdns.containsKey("L")) {
                builder.addRDN(BCStyle.L, rdns.get("L"));
            }
            if (rdns.containsKey("ST")) {
                builder.addRDN(BCStyle.ST, rdns.get("ST"));
            }

            return builder.build();
        }

        throw new GenericSecurityProcessorException("The provided distinguished name either is not valid or incomplete");
    }

    private static Certificate[] createCertificates(InputStream certInputStream) throws Exception {
        final CertificateFactory factory = CertificateFactory.getInstance("X.509");

        final List<X509Certificate> result = new ArrayList<>();
        final BufferedReader r = new BufferedReader(new InputStreamReader(certInputStream));

        String s = r.readLine();
        if (s == null || !s.contains("BEGIN CERTIFICATE")) {
            r.close();
            throw new GenericSecurityProcessorException("Error processing the provided X509 certificate chain");
        }

        StringBuilder b = new StringBuilder();
        while (s != null) {
            if (s.contains("END CERTIFICATE")) {
                String hexString = b.toString();
                final byte[] bytes = Base64.getDecoder().decode(hexString);
                X509Certificate cert = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(bytes));
                result.add(cert);
                b = new StringBuilder();
            } else {
                if (!s.startsWith("----")) {
                    b.append(s);
                }
            }
            s = r.readLine();
        }
        r.close();

        return result.toArray(new X509Certificate[0]);
    }
}
