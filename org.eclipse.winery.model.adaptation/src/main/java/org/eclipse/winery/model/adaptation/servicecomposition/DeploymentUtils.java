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

package org.eclipse.winery.model.adaptation.servicecomposition;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.xml.namespace.QName;

import org.eclipse.winery.common.Util;
import org.eclipse.winery.common.configuration.Environments;
import org.eclipse.winery.common.ids.definitions.ServiceTemplateId;
import org.eclipse.winery.model.tosca.TNodeTemplate;
import org.eclipse.winery.model.tosca.TPropertyMapping;
import org.eclipse.winery.model.tosca.TServiceCompositionModel;
import org.eclipse.winery.model.tosca.TServiceTemplate;
import org.eclipse.winery.repository.backend.BackendUtils;
import org.eclipse.winery.repository.backend.IRepository;
import org.eclipse.winery.repository.backend.RepositoryFactory;

import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class exposes utility functions to deploy service compositions. Therefore, it is in charge to deploy the 
 * contained ServiceTemplates and create corresponding service instances and to deploy the workflow model into the 
 * workflow engine.
 */
public class DeploymentUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeploymentUtils.class);

    /**
     * Deploys the given service composition with the workflow and all  required services.
     * 
     * @param serviceCompositionModel the model representing the service composition
     * @param containerURL URL to deploy the services on an OpenTOSCA Container
     * @param odeUrl URL to deploy the workflow on an Apache ODE
     */
    public static void deployServiceComposition(TServiceCompositionModel serviceCompositionModel, URL containerURL,
                                                URL odeUrl) {
        LOGGER.debug("Starting deployment for service composition: {}", serviceCompositionModel.getName());
        LOGGER.debug("Using OpenTOSCA Container endpoint: {} and Apache ODE endpoint: {}", containerURL, odeUrl);
        
        if (Objects.isNull(serviceCompositionModel.getServices()) || serviceCompositionModel.getServices().getServices().isEmpty()) {
            LOGGER.error("No services defined for the service composition!");
            return;
        }

        IRepository repo = RepositoryFactory.getRepository();
        
        // retrieve the ServiceTemplates corresponding to the services of the composition
        List<TServiceTemplate> serviceTemplates = new ArrayList<>();
        List<TServiceCompositionModel.Service> services = serviceCompositionModel.getServices().getServices();
        LOGGER.debug("Trying to retrieve ServiceTemplates for {} services.", services.size());
        for (TServiceCompositionModel.Service service : services) {
            LOGGER.debug("Retrieving ServiceTemplate for QName: {}", service.getId());
            
            // abort in case the required ServiceTemplate is not available in the repository
            ServiceTemplateId serviceId = BackendUtils.getDefinitionsChildId(ServiceTemplateId.class, service.getId());
            if (!repo.exists(serviceId)) {
                LOGGER.error("ServiceTemplate for QName {} is missing in the repository!", service.getId());
                return;
            }

            serviceTemplates.add(repo.getElement(serviceId));
        }

        LOGGER.debug("Successfully retrieved all required ServiceTemplates!");
        
        // upload the CSARs to the OpenTOSCA Container and create an instance of each service
        HashMap<QName, URI> endpointsMap = new HashMap<>();
        for (TServiceTemplate serviceTemplate : serviceTemplates) {
            LOGGER.debug("Creating instance for ServiceTemplate {}...", serviceTemplate.getId());
            URI serviceEndpoint = deployServiceTemplate(serviceTemplate, containerURL);
            if (Objects.isNull(serviceEndpoint)) {
                LOGGER.error("Error while provisioning service instance for ServiceTemplate: {}", serviceTemplate.getId());
                return;
            }
            LOGGER.debug("Deployed service on endpoint: {}", serviceEndpoint);
            endpointsMap.put(new QName(serviceTemplate.getTargetNamespace(), serviceTemplate.getId()), serviceEndpoint);
        }
        
        URL workflowEndpoint = deployWorkflow(serviceCompositionModel, serviceTemplates, endpointsMap, odeUrl);
        if (Objects.nonNull(workflowEndpoint)) {
            LOGGER.debug("Successfully deployed workflow. It can be accessed at the following endpoint: {}", workflowEndpoint);
        } else {
            LOGGER.error("Deployment of the workflow failed!");
        }
    }

    /**
     * Deploy the CSAR of the given ServiceTemplate, create an instance, and return the endpoint of the service 
     * instance.
     * 
     * @param serviceTemplate the ServiceTemplate to deploy
     * @param containerURL the URL of the OpenTOSCA Container for the deployment
     * @return the URL of the deployed service or <code>null</code> if the deployment fails
     */
    private static URI deployServiceTemplate(TServiceTemplate serviceTemplate, URL containerURL) {
        
        if (!isCSARAlreadyDeployed(serviceTemplate, containerURL)) {
            LOGGER.debug("CSAR for ServiceTemplate with Id {} is not available and has to be uploaded.",
                serviceTemplate.getId());
            if (uploadCSARToContainer(serviceTemplate, containerURL)) {
                LOGGER.error("Failed to upload CSAR for ServiceTemplate with Id: {}", serviceTemplate.getId());
                return null;
            }
        }

        LOGGER.debug("CSAR is deployed at the target Container. Creating instance of the service...");
        return createServiceInstance(serviceTemplate, containerURL);
    }

    /**
     * Check if the CSAR for the given ServiceTemplate is already deployed in the Container.
     * 
     * @param serviceTemplate the ServiceTemplate to check for deployment
     * @param containerURL the URL to the Container
     * @return <code>true</code> if the CSAR is already deployed, <code>false</code> otherwise
     */
    private static boolean isCSARAlreadyDeployed(TServiceTemplate serviceTemplate, URL containerURL) {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            LOGGER.debug("Checking for availability of CSAR with name: {}.csar", serviceTemplate.getId());

            // retrieve all deployed CSARs
            JSONObject jsonObject = returnJsonFromGet(httpclient, String.valueOf(containerURL));
            JSONArray csars = (JSONArray) jsonObject.get("csars");
            
            for (Object csar : csars.toArray()) {
                JSONObject csarJSON = (JSONObject) csar;
                if (csarJSON.get("id").equals(serviceTemplate.getId() + ".csar")) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            LOGGER.error("Exception while checking for availability of CSAR: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Deploy the CSAR for the given ServiceTemplate to the Container
     * 
     * @param serviceTemplate the ServiceTemplate to deploy
     * @param containerURL the URL to the Container
     * @return <code>true</code> if upload is successful, <code>false</code> otherwise
     */
    private static boolean uploadCSARToContainer(TServiceTemplate serviceTemplate, URL containerURL) {
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            ServiceTemplateId serviceId = BackendUtils.getDefinitionsChildId(ServiceTemplateId.class,
                new QName(serviceTemplate.getTargetNamespace(), serviceTemplate.getId()));
            
            CsarUploadRequest uploadRequest = new CsarUploadRequest();
            uploadRequest.setName(serviceTemplate.getId());
            uploadRequest.setUrl(Environments.getUiConfig().getEndpoints().get("repositoryApiUrl") + "/" + Util.getUrlPath(serviceId) + "?csar");
            uploadRequest.setEnrich("false");

            HttpPost post = new HttpPost(String.valueOf(containerURL));
            post.setEntity(new StringEntity(new Gson().toJson(uploadRequest)));
            post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            HttpResponse response = httpclient.execute(post);
            
            LOGGER.debug("Retrieved status code {} for the upload request.", response.getStatusLine().getStatusCode());
            return response.getStatusLine().getStatusCode() != 201;
        } catch (Exception e) {
            LOGGER.error("Exception while uploading CSAR: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Provision an instance of the given ServiceTemplate and return the endpoint of the created service.
     * 
     * @param serviceTemplate the ServiceTemplate to create an instance of
     * @param containerURL the URL to the Container
     * @return the endpoint of the provisioned service instance as URI or <code>null</code> if the deployment fails
     */
    private static URI createServiceInstance(TServiceTemplate serviceTemplate, URL containerURL) {
        QName serviceTemplateQName = new QName(serviceTemplate.getTargetNamespace(), serviceTemplate.getId());
        
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            LOGGER.debug("Creating service instance for ServiceTemplate: {}", serviceTemplate.getId());
            
            // retrieve all service templates contained in the csar
            String serviceTemplatesUrl = containerURL + "/" + serviceTemplate.getId() + ".csar/servicetemplates";
            JSONObject jsonObject = returnJsonFromGet(httpclient, serviceTemplatesUrl);
            JSONArray serviceTemplatesArray = (JSONArray) jsonObject.get("service_templates");

            // search for the required service template
            String targetServiceTemplateUrl = null;
            for (Object availableServiceTemplate : serviceTemplatesArray.toArray()) {
                JSONObject serviceTemplateJson = (JSONObject) availableServiceTemplate;
                if (serviceTemplateJson.get("id").equals(serviceTemplateQName.toString())) {
                    JSONObject links = (JSONObject) serviceTemplateJson.get("_links");
                    JSONObject self = (JSONObject) links.get("self");
                    targetServiceTemplateUrl = self.get("href").toString();
                    break;
                }
            }
            
            if (Objects.isNull(targetServiceTemplateUrl)) {
                LOGGER.error("Unable to find required ServiceTemplate in the CSAR!");
                return null;
            }
            
            // get the link to the build plan of the service template
            String buildPlansUrl = targetServiceTemplateUrl + "/buildplans";
            jsonObject = returnJsonFromGet(httpclient, buildPlansUrl);
            JSONArray plans = ((JSONArray) jsonObject.get("plans"));
            if (plans.size() == 0) {
                LOGGER.error("No build plans contained for ServiceTemplate: {}", serviceTemplate.getId());
                return null;
            }
            
            // use the first available build plan
            JSONObject plan = (JSONObject) plans.get(0);
            JSONArray inputParams = (JSONArray) plan.get("input_parameters");
            JSONObject links = (JSONObject) plan.get("_links");
            JSONObject instances = (JSONObject) links.get("instances");
            String buildPlanUrl = instances.get("href").toString();
            LOGGER.debug("Invoking build plan on Url: {}", buildPlanUrl);

            // create the service instance
            HttpPost post = new HttpPost(buildPlanUrl);
            post.setEntity(new StringEntity(inputParams.toString())); // we expect no required user input parameters
            post.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
            HttpResponse response = httpclient.execute(post);
            
            if (response.getStatusLine().getStatusCode() != 201) {
                LOGGER.error("Received invalid status code when invoking build plan: {}", response.getStatusLine().getStatusCode());
                return null;
            }
            LOGGER.debug("Build plan instance is running. Waiting for termination...");
            
            // wait for the plan create the instance in the instance database
            Thread.sleep(10000);
            
            if (Objects.isNull(serviceTemplate.getBoundaryDefinitions()) 
                || Objects.isNull(serviceTemplate.getBoundaryDefinitions().getProperties()) 
                || Objects.isNull(serviceTemplate.getBoundaryDefinitions().getProperties().getPropertyMappings())
                || Objects.isNull(serviceTemplate.getBoundaryDefinitions().getProperties().getPropertyMappings().getPropertyMapping())) {
                LOGGER.error("ServiceTemplate has no boundary definitons specifying the interface and endpoint of the" +
                    " service!");
                return null;
            }

            TPropertyMapping endpointMapping = 
                serviceTemplate.getBoundaryDefinitions().getProperties().getPropertyMappings().getPropertyMapping()
                    .stream()
                    .filter(propertyMapping -> propertyMapping.getServiceTemplatePropertyRef().equals("ServiceEndpoint"))
                    .findFirst().orElse(null);
            
            if (Objects.isNull(endpointMapping)) {
                LOGGER.error("ServiceTemplate can not be used in service composition as it does not specify a " +
                    "property containing the endpoint of the deployed service in its boundary definition!");
                return null;
            }
            
            if (!(endpointMapping.getTargetObjectRef() instanceof TNodeTemplate)) {
                LOGGER.error("Endpoint does not reference a NodeTemplate!");
                return null;
            }
            TNodeTemplate targetNodeTemplate = (TNodeTemplate) endpointMapping.getTargetObjectRef();

            // wait for plan termination and retrieve endpoint 
            return getEndpointForServiceInstance(httpclient, buildPlanUrl, targetServiceTemplateUrl, 
                EntityUtils.toString(response.getEntity()) , targetNodeTemplate.getId(),
                endpointMapping.getTargetPropertyRef());
        } catch (Exception e) {
            LOGGER.error("Exception while creating service instance: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Wait for the ServiceTemplate instance to be created and retrieve the endpoint of the service.
     * 
     * @param httpclient the client to perform the requests (has to be closed by the caller)
     * @param buildPlanUrl the url to the build plan instances of the ServiceTemplate
     * @param targetServiceTemplateUrl  the url to the ServiceTemplate
     * @param planCorrelationId the correlation id of the build plan related to the ServiceTemplate
     * @param endpointNodeTemplate the name of the NodeTemplate with the property containing the endpoint of the 
     *                             created service
     * @param endpointPropertyName the name of the property containing the endpoint of the created service
     * @return the URI if the ServiceTemplate instance is created successfully, <code>null</code> if a timeout or 
     * error occurs
     * @throws IOException
     * @throws ParseException
     * @throws InterruptedException
     */
    private static URI getEndpointForServiceInstance(CloseableHttpClient httpclient, String buildPlanUrl,
                                                     String targetServiceTemplateUrl, String planCorrelationId,
                                                     String endpointNodeTemplate, String endpointPropertyName)
        throws IOException, ParseException, InterruptedException, URISyntaxException {
        // search service template instance corresponding to the triggered build plan
        JSONObject planInstances = returnJsonFromGet(httpclient, buildPlanUrl);
        JSONArray planInstancesArray = (JSONArray) planInstances.get("plan_instances");
        String relatedServiceTemplateUrl = null;
        for (Object planInstance : planInstancesArray.toArray()) {
            JSONObject planInstanceJson = (JSONObject) planInstance;

            if (planInstanceJson.get("correlation_id").equals(planCorrelationId)) {
                JSONObject planInstanceLinks = (JSONObject) planInstanceJson.get("_links");
                relatedServiceTemplateUrl =
                    ((JSONObject) planInstanceLinks.get("service_template_instance")).get("href").toString();
                break;
            }
        }

        if (Objects.isNull(relatedServiceTemplateUrl)) {
            LOGGER.error("Unable to retrieve service template instance related to triggered build plan!");
            return null;
        }

        // wait for the termination
        String serviceTemplateState = "INITIAL";
        int count = 1;
        while (!serviceTemplateState.equals("CREATED") && count <= 100) {
            LOGGER.debug("Waiting for termination of build plan ({}/100).", count);
            HttpGet get = new HttpGet(relatedServiceTemplateUrl + "/state");
            serviceTemplateState = EntityUtils.toString(httpclient.execute(get).getEntity());
            count++;
            Thread.sleep(10000);
        }

        if (!serviceTemplateState.equals("CREATED")) {
            LOGGER.debug("");
            return null;
        }
        LOGGER.debug("ServiceTemplate instance created successfully.");
        
        LOGGER.debug("Endpoint is retrieved on NodeTemplate {} from property {}.", endpointNodeTemplate,
            endpointPropertyName);
        String serviceTemplateId = relatedServiceTemplateUrl.substring(relatedServiceTemplateUrl.lastIndexOf("/") + 1);
        String endpointUrl = targetServiceTemplateUrl + "/nodetemplates/" + endpointNodeTemplate + "/instances";
        
        // search for NodeTemplate instance related to the ServiceTemplate instance
        JSONObject nodeTemplateInstances = returnJsonFromGet(httpclient, endpointUrl);
        JSONArray nodeTemplateInstancesArray = (JSONArray) nodeTemplateInstances.get("node_template_instances");
        String endpointPropertyUrl = null;
        for (Object nodeTemplateInstance : nodeTemplateInstancesArray.toArray()) {
            JSONObject nodeTemplateInstanceJSON = (JSONObject) nodeTemplateInstance;
            if (Integer.parseInt(nodeTemplateInstanceJSON.get("service_template_instance_id").toString()) 
                == Integer.parseInt(serviceTemplateId)) {
                JSONObject links = (JSONObject) nodeTemplateInstanceJSON.get("_links");
                JSONObject href = (JSONObject) links.get("self");
                endpointPropertyUrl = href.get("href").toString();
                break;
            }
        }

        if (Objects.isNull(endpointPropertyUrl)) {
            LOGGER.error("Unable to find NodeTemplate instance containing endpoint information!");
            return null;
        }
        endpointPropertyUrl += "/properties";
        LOGGER.debug("Retrieving endpoint from properties at Url: {}", endpointPropertyUrl);

        // retrieve endpoint property
        JSONObject properties = returnJsonFromGet(httpclient, endpointPropertyUrl);
        String propertyName = endpointPropertyName.substring(endpointPropertyName.lastIndexOf("=") + 1);
        propertyName = propertyName.split("'")[1];
        Object endpoint = properties.get(propertyName);
        
        if (Objects.isNull(endpoint)) {
            LOGGER.debug("Unable to retrieve endpoint variable!");
            return null;
        }

        return new URI(endpoint.toString());
    }

    /**
     * Perform a Http Get request on the given URL and return the result as a JSONObject
     * 
     * @param httpclient the client to perform the request (has to be closed by the caller)
     * @param url the URL to perform the get on
     * @return the JSONObject of the response
     * @throws IOException 
     * @throws ParseException
     */
    private static JSONObject returnJsonFromGet(CloseableHttpClient httpclient, String url) throws IOException, ParseException {
        HttpGet get = new HttpGet(String.valueOf(url));
        get.setHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON);
        HttpResponse response = httpclient.execute(get);
        
        String json = EntityUtils.toString(response.getEntity());
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(json);
        return jsonObject;
    }

    /**
     * Create the workflow implementing the service composition, add the corresponding endpoints to the service 
     * calls, and deploy the workflow to Apache ODE.
     * 
     * @param serviceCompositionModel the service composition model
     * @param serviceTemplates the ServiceTemplates that are contained in the service composition
     * @param endpointsMap the endpoints of the deployed service instances
     * @param odeURL the URL of the Apache ODE for the deployment
     * @return the URL of the deployed workflow or <code>null</code> if the deployment fails
     */
    private static URL deployWorkflow(TServiceCompositionModel serviceCompositionModel,
                                      List<TServiceTemplate> serviceTemplates, HashMap<QName, URI> endpointsMap,
                                      URL odeURL) {
        // TODO: generate and deploy workflow
        return null;
    }
}
