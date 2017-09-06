/*******************************************************************************
 * Copyright (c) 2017 University of Stuttgart.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and the Apache License 2.0 which both accompany this distribution,
 * and are available at http://www.eclipse.org/legal/epl-v10.html
 * and http://www.apache.org/licenses/LICENSE-2.0
 *
 * Contributors:
 *     Karoline Saatkamp - initial API and implementation
 *******************************************************************************/
package org.eclipse.winery.repository.rest.datatypes;

public class SplitServiceTemplateInformation {
	
	private final String id;
	private final String url;
	private final String documentation;

	public SplitServiceTemplateInformation(String id, String url, String documentation) {
		this.id = id;
		this.url = url;
		this.documentation = documentation;
	}

	public String getId() {
		return id;
	}

	/**
	 * @return the URL of the service template
	 */
	public String getUrl() {
		return url;
	}

	public String getDocumentation() {
		return documentation;
	}
}
