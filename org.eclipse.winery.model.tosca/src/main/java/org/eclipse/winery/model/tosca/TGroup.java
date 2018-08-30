/*******************************************************************************
 * Copyright (c) 2013-2018 Contributors to the Eclipse Foundation
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

package org.eclipse.winery.model.tosca;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAnyElement;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.CollapsedStringAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.eclipse.winery.model.tosca.visitor.Visitor;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.w3c.dom.Element;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tGroup", propOrder = {
    
})
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
public class TGroup extends TExtensibleElements {
  
   
    @XmlAttribute(name = "id", required = true)
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;
    
    @XmlAttribute(name = "name")
    protected String name;
    
    @XmlAttribute(name = "groupType")
    @XmlSchemaType(name = "anyURI")
    protected String groupType;

    @XmlElement(name = "Properties")
    protected TEntityTemplate.Properties properties;
    
    @XmlElement(name = "NodeTemplate")
    protected List<String> nodeTemplates;
    
    public TGroup() {

    }

    public TGroup(Builder builder) {
        super(builder);
        this.id = builder.id;
        this.name = builder.name;
        this.groupType = builder.groupType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TGroup)) return false;
        if (!super.equals(o)) return false;
        TGroup tPlan = (TGroup) o;
        return Objects.equals(id, tPlan.id) &&
            Objects.equals(name, tPlan.name) &&
            Objects.equals(groupType, tPlan.groupType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), id, name, groupType);
    }
    
    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String value) {
        this.id = value;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(@Nullable String value) {
        this.name = value;
    }

    @NonNull
    public String getGroupType() {
        return groupType;
    }

    public void setGroupType(@NonNull String value) {
        this.groupType = Objects.requireNonNull(value);
    }
    
    public TEntityTemplate.Properties getProperties() {
        return properties;
    }

    public void setProperties(TEntityTemplate.Properties properties) {
        this.properties = properties;
    }

    public List<String> getNodeTemplates() {
        return nodeTemplates;
    }

    public void setNodeTemplates(List<String> nodeTemplates) {
        this.nodeTemplates = nodeTemplates;
    }
    
    public void accept(Visitor visitor) {
        visitor.visit(this);
   }

    public static class Builder extends TExtensibleElements.Builder<Builder> {
        private String id;
        private String name;
        private String groupType;

        public Builder(String id, String name, String groupType) {
            this.id = id;
            this.name = name;
            this.groupType = groupType;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }
        
        public Builder setId(String id) {
            this.id= id;
            return this;
        }
        
        public Builder setGroupType(String groupType){
            this.groupType = groupType;
            return this;
        }

        @Override
        public Builder self() {
            return this;
        }

        public TGroup build() {
            return new TGroup(this);
        }
    }
}
