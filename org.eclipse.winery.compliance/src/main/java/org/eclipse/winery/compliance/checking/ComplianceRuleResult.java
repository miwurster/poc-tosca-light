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

package org.eclipse.winery.compliance.checking;

import java.util.ArrayList;
import java.util.List;

public class ComplianceRuleResult {
    private String ruleNamespace;
    private String ruleId;
    private List<String> violatingNodesIds = new ArrayList<>();
    private Boolean isSatisfied;

    public ComplianceRuleResult() {

    }

    public ComplianceRuleResult(String ruleNamespace, String ruleId ) {
        this.ruleNamespace = ruleNamespace;
        this.ruleId = ruleId;
    }

    public String getRuleId() {
        return ruleId;
    }

    public void setRuleId(String ruleId) {
        this.ruleId = ruleId;
    }

    public List<String> getViolatingNodesIds() {
        return violatingNodesIds;
    }

    public void setViolatingNodesIds(List<String> violatingNodesIds) {
        this.violatingNodesIds = violatingNodesIds;
    }

    public Boolean getIsSatisfied() {
        return isSatisfied;
    }

    public void setIsSatisfied(Boolean satisfied) {
        this.isSatisfied = satisfied;
    }

    public String getRuleNamespace() {
        return ruleNamespace;
    }

    public void setRuleNamespace(String ruleNamespace) {
        this.ruleNamespace = ruleNamespace;
    }
}
