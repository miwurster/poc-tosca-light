/********************************************************************************
 * Copyright (c) 2017-2020 Contributors to the Eclipse Foundation
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
package org.eclipse.winery.model.tosca.yaml;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;

import org.eclipse.winery.model.tosca.yaml.support.Annotations;
import org.eclipse.winery.model.tosca.yaml.visitor.AbstractParameter;
import org.eclipse.winery.model.tosca.yaml.visitor.AbstractResult;
import org.eclipse.winery.model.tosca.yaml.visitor.IVisitor;
import org.eclipse.winery.model.tosca.yaml.visitor.VisitorNode;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tOperationDefinition", namespace = " http://docs.oasis-open.org/tosca/ns/simple/yaml/1.3", propOrder = {
    "description",
    "inputs",
    "implementation"
})
public class TOperationDefinition implements VisitorNode {

    private String description;
    private Map<String, TParameterDefinition> inputs;
    @Annotations.StandardExtension
    private Map<String, TParameterDefinition> outputs;
    @Annotations.StandardExtension
    private TImplementation implementation;

    public TOperationDefinition() {
    }

    public TOperationDefinition(Builder builder) {
        this.setDescription(builder.description);
        this.setInputs(builder.inputs);
        this.setOutputs(builder.outputs);
        this.setImplementation(builder.implementation);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TOperationDefinition)) return false;
        TOperationDefinition that = (TOperationDefinition) o;
        return Objects.equals(getDescription(), that.getDescription()) &&
            Objects.equals(getInputs(), that.getInputs()) &&
            Objects.equals(getOutputs(), that.getOutputs()) &&
            Objects.equals(getImplementation(), that.getImplementation());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getDescription(), getInputs(), getOutputs(), getImplementation());
    }

    @Override
    public String toString() {
        return "TOperationDefinition{" +
            "description='" + getDescription() + '\'' +
            ", inputs=" + getInputs() +
            ", outputs=" + getOutputs() +
            ", implementation=" + getImplementation() +
            '}';
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @NonNull
    public Map<String, TParameterDefinition> getInputs() {
        if (this.inputs == null) {
            this.inputs = new LinkedHashMap<>();
        }

        return inputs;
    }

    public void setInputs(Map<String, TParameterDefinition> inputs) {
        this.inputs = inputs;
    }

    @NonNull
    public Map<String, TParameterDefinition> getOutputs() {
        if (this.outputs == null) {
            this.outputs = new LinkedHashMap<>();
        }
        return outputs;
    }

    public TOperationDefinition setOutputs(Map<String, TParameterDefinition> outputs) {
        this.outputs = outputs;
        return this;
    }

    @Nullable
    public TImplementation getImplementation() {
        return implementation;
    }

    public void setImplementation(TImplementation implementation) {
        this.implementation = implementation;
    }

    public <R extends AbstractResult<R>, P extends AbstractParameter<P>> R accept(IVisitor<R, P> visitor, P parameter) {
        return visitor.visit(this, parameter);
    }

    public static class Builder {

        private String description;
        private Map<String, TParameterDefinition> inputs;
        private Map<String, TParameterDefinition> outputs;
        private TImplementation implementation;

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setInputs(Map<String, TParameterDefinition> inputs) {
            this.inputs = inputs;
            return this;
        }

        public Builder setOutputs(Map<String, TParameterDefinition> outputs) {
            this.outputs = outputs;
            return this;
        }

        public Builder setImplementation(TImplementation implementation) {
            this.implementation = implementation;
            return this;
        }

        public Builder addInputs(Map<String, TParameterDefinition> inputs) {
            if (inputs == null || inputs.isEmpty()) {
                return this;
            }

            if (this.inputs == null) {
                this.inputs = new LinkedHashMap<>(inputs);
            } else {
                this.inputs.putAll(inputs);
            }

            return this;
        }

        public Builder addInputs(String name, TParameterDefinition input) {
            if (name == null || name.isEmpty()) {
                return this;
            }

            return addInputs(Collections.singletonMap(name, input));
        }

        public Builder addOutputs(Map<String, TParameterDefinition> outputs) {
            if (outputs == null || outputs.isEmpty()) {
                return this;
            }

            if (this.outputs == null) {
                this.outputs = new LinkedHashMap<>(outputs);
            } else {
                this.outputs.putAll(outputs);
            }

            return this;
        }

        public Builder addOutputs(String name, TParameterDefinition output) {
            if (name == null || name.isEmpty()) {
                return this;
            }

            return addOutputs(Collections.singletonMap(name, output));
        }

        public TOperationDefinition build() {
            return new TOperationDefinition(this);
        }
    }
}
