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

package org.eclipse.winery.security.datatypes;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class CertificateInformation {
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");

    @JsonProperty
    private String alias;
    @JsonProperty
    private String serialNumber;
    @JsonProperty
    private String sigAlgName;
    @JsonProperty
    private String issuerDN;
    @JsonProperty
    private String subjectDN;
    @JsonSerialize(using = JsonDateSerializer.class)
    private Date notBefore;
    @JsonSerialize(using = JsonDateSerializer.class)
    private Date notAfter;
    @JsonProperty
    private String pem;

    private CertificateInformation(Builder builder) {
        alias = builder.alias;
        serialNumber = builder.serialNumber;
        sigAlgName = builder.sigAlgName;
        issuerDN = builder.issuerDN;
        subjectDN = builder.subjectDN;
        notBefore = builder.notBefore;
        notAfter = builder.notAfter;
        pem = builder.pemEncodedCertificate;
    }

    public String getPem() {
        return pem;
    }

    public String getSubjectDN() {
        return subjectDN;
    }

    public String getIssuerDN() {
        return issuerDN;
    }

    public String getSigAlgName() {
        return sigAlgName;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public String getAlias() {
        return alias;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static class Builder {
        private final String alias;

        private String serialNumber;
        private String sigAlgName;
        private String subjectDN;
        private Date notBefore;
        private Date notAfter;
        private String issuerDN;
        private String pemEncodedCertificate;

        public Builder(String alias) {
            this.alias = alias;
        }

        public Builder serialNumber(String serialNumber) {
            this.serialNumber = serialNumber;
            return this;
        }

        public Builder sigAlgName(String sigAlgName) {
            this.sigAlgName = sigAlgName;
            return this;
        }

        public Builder subjectDN(String subjectDN) {
            this.subjectDN = subjectDN;
            return this;
        }

        public Builder issuerDN(String issuerDN) {
            this.issuerDN = issuerDN;
            return this;
        }

        public Builder notBefore(Date notBefore) {
            this.notBefore = notBefore;
            return this;
        }

        public Builder notAfter(Date notAfter) {
            this.notAfter = notAfter;
            return this;
        }

        public Builder pemEncodedCertificate(String pemEncodedCertificate) {
            this.pemEncodedCertificate = pemEncodedCertificate;
            return this;
        }

        public CertificateInformation build() {
            return new CertificateInformation(this);
        }
    }

    public String printValidityPeriod() {
        return "[" + dateFormat.format(notBefore) + ", " + dateFormat.format(notAfter) + "]";
    }

    private static class JsonDateSerializer extends JsonSerializer<Date> {
        @Override
        public void serialize(Date date, JsonGenerator gen, SerializerProvider provider) throws IOException {
            String formattedDate = dateFormat.format(date);
            gen.writeString(formattedDate);
        }
    }
}
