/*******************************************************************************
 * Copyright (c) 2019 Contributors to the Eclipse Foundation
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSchemaType;
import javax.xml.bind.annotation.XmlType;
import javax.xml.namespace.QName;

import org.eclipse.winery.model.tosca.visitor.Visitor;

import org.eclipse.jdt.annotation.NonNull;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "tServiceCompositionModel")
public class TServiceCompositionModel  extends HasId implements HasName, HasTargetNamespace {
    
    @XmlAttribute
    protected String name;

    @XmlAttribute(name = "targetNamespace")
    @XmlSchemaType(name = "anyURI")
    protected String targetNamespace;

    @XmlElement(name = "InputParameters")
    protected InputParameters inputParameters;

    @XmlElement(name = "OutputParameters", required = true)
    protected OutputParameters outputParameters;

    @XmlElement(name = "Services", required = true)
    protected Services services;

    @XmlElement(name = "DataLinks", required = true)
    protected DataLinks dataLinks;

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String value) {
        this.name = value;
    }

    @Override
    public String getTargetNamespace() {
        return targetNamespace;
    }

    @Override
    public void setTargetNamespace(String value) {
        targetNamespace = value;
    }

    public InputParameters getInputParameters() {
        return inputParameters;
    }

    public void setInputParameters(InputParameters value) {
        this.inputParameters = value;
    }

    public OutputParameters getOutputParameters() {
        return outputParameters;
    }

    public void setOutputParameters(OutputParameters value) {
        this.outputParameters = value;
    }

    public Services getServices() { return services; }

    public void setServices(Services value) {
        this.services = value;
    }

    @Override
    public void accept(Visitor visitor) {
        visitor.visit(this);
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "inputParameter"
    })
    public static class InputParameters implements Serializable {

        @XmlElement(name = "InputParameter", required = true)
        protected List<TParameter> inputParameter;

        @NonNull
        public List<TParameter> getInputParameter() {
            if (inputParameter == null) {
                inputParameter = new ArrayList<TParameter>();
            }
            return this.inputParameter;
        }

        public void setInputParameter(List<TParameter> inputParameter) {
            this.inputParameter = inputParameter;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InputParameters that = (InputParameters) o;
            return Objects.equals(inputParameter, that.inputParameter);
        }

        @Override
        public int hashCode() {
            return Objects.hash(inputParameter);
        }
    }
    
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
        "outputParameter"
    })
    public static class OutputParameters implements Serializable {

        @XmlElement(name = "OutputParameter", required = true)
        protected List<TParameter> outputParameter;

        @NonNull
        public List<TParameter> getOutputParameter() {
            if (outputParameter == null) {
                outputParameter = new ArrayList<TParameter>();
            }
            return this.outputParameter;
        }

        public void setOutputParameter(List<TParameter> outputParameter) {
            this.outputParameter = outputParameter;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            OutputParameters that = (OutputParameters) o;
            return Objects.equals(outputParameter, that.outputParameter);
        }

        @Override
        public int hashCode() {
            return Objects.hash(outputParameter);
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class Services implements Serializable {

        @XmlElement(name = "Service", required = true)
        protected List<Service> services;

        @NonNull
        public List<Service> getServices() {
            if (services == null) {
                services = new ArrayList<Service>();
            }
            return this.services;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Services that = (Services) o;
            return Objects.equals(services, that.services);
        }

        @Override
        public int hashCode() {
            return Objects.hash(services);
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class Service implements Serializable {

        @XmlAttribute(name = "id", required = true)
        protected QName id;

        public QName getId() {
            return id;
        }

        public void setId(@NonNull QName value) {
            this.id = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Service that = (Service) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class DataLinks implements Serializable {

        @XmlElement(name = "DataLink", required = true)
        protected List<DataLink> dataLinks;

        @NonNull
        public List<DataLink> getDataLinks() {
            if (dataLinks == null) {
                dataLinks = new ArrayList<DataLink>();
            }
            return this.dataLinks;
        }
        
        public void setDataLinks(List<DataLink> dataLinks) { this.dataLinks = dataLinks; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DataLinks that = (DataLinks) o;
            return Objects.equals(dataLinks, that.dataLinks);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dataLinks);
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class DataLink implements Serializable {

        @XmlElement(name = "Source", required = true)
        protected DataLinkSourceOrTarget source;

        @XmlElement(name = "Target", required = true)
        protected DataLinkSourceOrTarget target;

        public DataLinkSourceOrTarget getSource() { return source; }

        public void setSource(DataLinkSourceOrTarget source) { this.source = source; }

        public DataLinkSourceOrTarget getTarget() { return target; }

        public void setTarget(DataLinkSourceOrTarget target) { this.target = target; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DataLink that = (DataLink) o;
            return Objects.equals(source, that.source) && Objects.equals(target, that.target);
        }

        @Override
        public int hashCode() {
            return Objects.hash(source);
        }
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    public static class DataLinkSourceOrTarget implements Serializable {

        @XmlAttribute(name = "serviceTemplateId")
        protected QName serviceTemplateId;
        
        @XmlAttribute(name = "interfaceName")
        protected String interfaceName;

        @XmlAttribute(name = "operationName")
        protected String operationName;

        @XmlAttribute(name = "parameterName", required = true)
        protected String parameterName;
        
        public QName getServiceTemplateId() { return serviceTemplateId; }

        public void setServiceTemplateId(QName serviceTemplateId) { this.serviceTemplateId = serviceTemplateId; }

        public String getInterfaceName() { return interfaceName; }

        public void setInterfaceName(String interfaceName) { this.interfaceName = interfaceName; }

        public String getOperationName() { return operationName; }

        public void setOperationName(String operationName) { this.operationName = operationName; }
        
        public String getParameterName() { return parameterName; }

        public void setParameterName(String parameterName) { this.parameterName = parameterName; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DataLinkSourceOrTarget that = (DataLinkSourceOrTarget) o;
            return Objects.equals(serviceTemplateId, that.serviceTemplateId) 
                && Objects.equals(interfaceName, that.interfaceName)
                && Objects.equals(operationName, that.operationName)
                && Objects.equals(parameterName, that.parameterName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(serviceTemplateId, interfaceName, operationName, parameterName);
        }
    }
}
