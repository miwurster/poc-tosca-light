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

import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import org.eclipse.winery.model.tosca.constants.Namespaces;
import org.eclipse.winery.model.tosca.kvproperties.WinerysPropertiesDefinition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.eclipse.jdt.annotation.Nullable;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tServiceTemplate", propOrder = {
    "propertiesDefinition",
    "tags",
    "boundaryDefinitions",
    "topologyTemplate",
    "plans"
})
public class TServiceTemplate extends HasId implements HasName, HasTargetNamespace {
    public static final String NS_SUFFIX_PROPERTIESDEFINITION_WINERY = "propertiesdefinition/winery";
    
    @XmlElements( {
        @XmlElement(name = "PropertiesDefinition", namespace = Namespaces.TOSCA_WINERY_EXTENSIONS_NAMESPACE, type = TServiceTemplate.PropertiesDefinition.class),
        @XmlElement(name = "PropertiesDefinition", namespace = Namespaces.TOSCA_WINERY_EXTENSIONS_NAMESPACE, type = WinerysPropertiesDefinition.class)
    })
    protected Object propertiesDefinition;

    @XmlElement(name = "Tags")
    protected TTags tags;

    @XmlElement(name = "BoundaryDefinitions")
    protected TBoundaryDefinitions boundaryDefinitions;

    @XmlElement(name = "TopologyTemplate", required = true)
    protected TTopologyTemplate topologyTemplate;

    @XmlElement(name = "Plans")
    protected TPlans plans;

    @XmlAttribute(name = "name")
    protected String name;

    @XmlAttribute(name = "targetNamespace")
    @XmlSchemaType(name = "anyURI")
    protected String targetNamespace;

    @XmlAttribute(name = "substitutableNodeType")
    protected QName substitutableNodeType;

    public TServiceTemplate() {
    }

    public TServiceTemplate(Builder builder) {
        super(builder);
        this.tags = builder.tags;
        this.boundaryDefinitions = builder.boundaryDefinitions;
        this.topologyTemplate = builder.topologyTemplate;
        this.plans = builder.plans;
        this.name = builder.name;
        this.targetNamespace = builder.targetNamespace;
        this.substitutableNodeType = builder.substitutableNodeType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TServiceTemplate)) return false;
        if (!super.equals(o)) return false;
        TServiceTemplate that = (TServiceTemplate) o;
        return Objects.equals(tags, that.tags) &&
            Objects.equals(boundaryDefinitions, that.boundaryDefinitions) &&
            Objects.equals(topologyTemplate, that.topologyTemplate) &&
            Objects.equals(plans, that.plans) &&
            Objects.equals(name, that.name) &&
            Objects.equals(targetNamespace, that.targetNamespace) &&
            Objects.equals(substitutableNodeType, that.substitutableNodeType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), tags, boundaryDefinitions, topologyTemplate, plans, name, targetNamespace, substitutableNodeType);
    }

    @Nullable
    public TTags getTags() {
        return tags;
    }

    public void setTags(@Nullable TTags value) {
        this.tags = value;
    }

    @Nullable
    public TBoundaryDefinitions getBoundaryDefinitions() {
        return boundaryDefinitions;
    }

    public void setBoundaryDefinitions(@Nullable TBoundaryDefinitions value) {
        this.boundaryDefinitions = value;
    }

    /**
     * Even though the XSD requires that the topology template is always set, during modeling, it might be null
     */
    @Nullable
    public TTopologyTemplate getTopologyTemplate() {
        return topologyTemplate;
    }

    public void setTopologyTemplate(@Nullable TTopologyTemplate value) {
        this.topologyTemplate = value;
    }

    @Nullable
    public TPlans getPlans() {
        return plans;
    }

    public void setPlans(TPlans value) {
        this.plans = value;
    }

    @Nullable
    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    @Nullable
    public String getTargetNamespace() {
        return targetNamespace;
    }

    public void setTargetNamespace(String value) {
        this.targetNamespace = value;
    }

    @Nullable
    public QName getSubstitutableNodeType() {
        return substitutableNodeType;
    }

    public void setSubstitutableNodeType(QName value) {
        this.substitutableNodeType = value;
    }

    public Object getPropertiesDefinition() {
        return propertiesDefinition;
    }

    public void setPropertiesDefinition(Object value) {
        this.propertiesDefinition = value;
    }

    /**
     * <p>Java class for anonymous complex type.
     * <p>
     * <p>The following schema fragment specifies the expected content contained within this class.
     * <p>
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;attribute name="element" type="{http://www.w3.org/2001/XMLSchema}QName" />
     *       &lt;attribute name="type" type="{http://www.w3.org/2001/XMLSchema}QName" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class PropertiesDefinition extends TEntityType.PropertiesDefinition {
    }

    /**
     * This is a special method for Winery. Winery allows to define a property definition by specifying name/type
     * values. Instead of parsing the extensible elements returned TDefinitions, this method is a convenience method to
     * access this information
     *
     * @return a WinerysPropertiesDefinition object, which includes a map of name/type-pairs denoting the associated
     * property definitions. A default element name and namespace is added if it is not defined in the underlying XML.
     * null if no Winery specific KV properties are defined for the given entity type
     */
    @XmlTransient
    @JsonIgnore
    public WinerysPropertiesDefinition getWinerysPropertiesDefinition() {
        // similar implementation as org.eclipse.winery.repository.resources.entitytypes.properties.PropertiesDefinitionResource.getListFromEntityType(TEntityType)
        WinerysPropertiesDefinition res = null;
        if (this.getPropertiesDefinition() instanceof WinerysPropertiesDefinition) {
            res = (WinerysPropertiesDefinition) this.getPropertiesDefinition();
        }

        if (res != null) {
            // we put defaults if elementname and namespace have not been set

            if (res.getElementName() == null) {
                res.setElementName("Properties");
            }

            if (res.getNamespace() == null) {
                // we use the targetnamespace of the original element
                String ns = this.getTargetNamespace();
                if (!ns.endsWith("/")) {
                    ns += "/";
                }
                ns += NS_SUFFIX_PROPERTIESDEFINITION_WINERY;
                res.setNamespace(ns);
            }
        }

        return res;
    }
    
    public static class Builder extends HasId.Builder<Builder> {
        private final TTopologyTemplate topologyTemplate;

        private TTags tags;
        private TBoundaryDefinitions boundaryDefinitions;
        private TPlans plans;
        private String name;
        private String targetNamespace;
        private QName substitutableNodeType;

        public Builder(String id, TTopologyTemplate topologyTemplate) {
            super(id);
            this.topologyTemplate = topologyTemplate;
        }

        public Builder setTags(TTags tags) {
            this.tags = tags;
            return this;
        }

        public Builder setBoundaryDefinitions(TBoundaryDefinitions boundaryDefinitions) {
            this.boundaryDefinitions = boundaryDefinitions;
            return this;
        }

        public Builder setPlans(TPlans plans) {
            this.plans = plans;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setTargetNamespace(String targetNamespace) {
            this.targetNamespace = targetNamespace;
            return this;
        }

        public Builder setSubstitutableNodeType(QName substitutableNodeType) {
            this.substitutableNodeType = substitutableNodeType;
            return this;
        }

        public Builder addTags(TTags tags) {
            if (tags == null || tags.getTag().isEmpty()) {
                return this;
            }

            if (this.tags == null) {
                this.tags = tags;
            } else {
                this.tags.getTag().addAll(tags.getTag());
            }
            return this;
        }

        public Builder addTags(List<TTag> tags) {
            if (tags == null) {
                return this;
            }

            TTags tmp = new TTags();
            tmp.getTag().addAll(tags);
            return addTags(tmp);
        }

        public Builder addTags(TTag tags) {
            if (tags == null) {
                return this;
            }

            TTags tmp = new TTags();
            tmp.getTag().add(tags);
            return addTags(tmp);
        }

        @Override
        public Builder self() {
            return this;
        }

        public TServiceTemplate build() {
            return new TServiceTemplate(this);
        }
    }
}
