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

package org.eclipse.winery.repository.rest.resources.apiData;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.winery.model.tosca.TPolicy;
import org.eclipse.winery.model.tosca.constants.Namespaces;

public class PoliciesApiData {

    public List<PolicyApiData> policies;
    public List<SecurityPolicyApiData> securityPolicies;

    public PoliciesApiData(List<TPolicy> policies) {
        this.policies = new ArrayList<>();
        this.securityPolicies = new ArrayList<>();
        for (TPolicy p : policies) {
            if (p.getPolicyType().toString().contains(Namespaces.URI_OPENTOSCA_SECURE_POLICYTYPE)) {
                this.securityPolicies.add(new SecurityPolicyApiData(p));
            }
            else {
                this.policies.add(new PolicyApiData(p));
            }
        }
    }

    private class PolicyApiData {
        public String name;
        public QNameApiData type;
        public QNameApiData template;

        public PolicyApiData(TPolicy p) {
            this.name = p.getName();
            this.type = new QNameApiData(p.getPolicyType().getLocalPart(), p.getPolicyType().getNamespaceURI());
            this.template = new QNameApiData(p.getPolicyRef().getLocalPart(), p.getPolicyRef().getNamespaceURI());
        }
    }
    
    private class SecurityPolicyApiData extends PolicyApiData {
        public boolean isApplied;

        public SecurityPolicyApiData(TPolicy p) {
            super(p);
            this.isApplied = p.getIsApplied();
        }
    }
}
