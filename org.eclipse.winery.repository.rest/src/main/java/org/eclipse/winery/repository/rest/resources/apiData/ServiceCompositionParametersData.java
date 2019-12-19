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

package org.eclipse.winery.repository.rest.resources.apiData;

import java.io.Serializable;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.winery.model.tosca.TParameter;

@XmlRootElement(name = "Parameters")
public class ServiceCompositionParametersData implements Serializable {

    @XmlElementWrapper(name = "InputParameters")
    @XmlElement(name = "InputParameter")
    public List<TParameter> inputParameters;

    @XmlElementWrapper(name = "OutputParameters")
    @XmlElement(name = "OutputParameter")
    public List<TParameter> outputParameters;

    public List<TParameter> getInputParameters() { return this.inputParameters; }

    public void setInputParameters(List<TParameter> inputParameters) { this.inputParameters = inputParameters; }

    public List<TParameter> getOutputParameters() { return this.outputParameters; }

    public void setOutputParameters(List<TParameter> outputParameters) { this.outputParameters = outputParameters; }
}
