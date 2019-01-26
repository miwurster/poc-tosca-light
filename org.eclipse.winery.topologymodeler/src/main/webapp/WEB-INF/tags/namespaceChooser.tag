<%--~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
  ~ Copyright (c) 2012-2017 Contributors to the Eclipse Foundation
  ~
  ~ See the NOTICE file(s) distributed with this work for additional
  ~ information regarding copyright ownership.
  ~
  ~ This program and the accompanying materials are made available under the
  ~ terms of the Eclipse Public License 2.0 which is available at
  ~ http://www.eclipse.org/legal/epl-2.0, or the Apache Software License 2.0
  ~ which is available at https://www.apache.org/licenses/LICENSE-2.0.
  ~
  ~ SPDX-License-Identifier: EPL-2.0 OR Apache-2.0
  ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~--%>
<%@tag description="places a bootstrap form control to chooose a namespace. A new namespace can be created"
       pageEncoding="UTF-8" %>

<!--
This tag is shared at repository and topologytemplate.
Both versions differ from each other.
In the repository, ns.decoded is used.
In the topology modeler only "ns" is used:
In other words: The topology modeler passes a Collection<String>, whereas repository passes Collection<Namespace>
-->

<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<%@attribute name="allNamespaces" required="true" type="java.util.Collection"
             description="All known namespaces as strings (because the topology modeler currently doesn't provide that as list of winery namespace objects)" %>
<%@attribute name="idOfInput" required="true"
             description="The id if the input field storing the namespace. Also used as name" %>
<%@attribute name="nameOfInput" required="false"
             description="The name if the input field storing the namespace. If not provided, ifOfInput is used" %>
<%@attribute name="selected" description="The currently selected namespace (optional)" %>

<c:if test="${empty nameOfInput}"><c:set var="nameOfInput" value="${idOfInput}"></c:set></c:if>

<!-- createArtifactTemplate class is required for artifactcreationdialog -->
<div class="form-group createArtifactTemplate">
    <label for="${idOfInput}" class="control-label">Namespace</label>
    <input type="text" class="form-control" name="${nameOfInput}" id="${idOfInput}"></input>
</div>

<script>
    // we have to use data as select2 does not allow "createSearchChoice" when using <select> as underlying html element
    require(["bootstrap3-typeahead"], function () {

        $("#${idOfInput}").typeahead({

            source: [
                <c:forEach var="ns" items="${allNamespaces}" varStatus="loop">
                {id: "${ns}", name: "${ns}"}<c:if test="${!loop.last}">, </c:if>
                </c:forEach>
            ],
            autoSelect: true,
            showHintOnFocus: true
        });
        $("#${idOfInput}").val("${selected}");
        $("#${idOfInput}").typeahead("lookup", "${selected}");
    })
</script>
