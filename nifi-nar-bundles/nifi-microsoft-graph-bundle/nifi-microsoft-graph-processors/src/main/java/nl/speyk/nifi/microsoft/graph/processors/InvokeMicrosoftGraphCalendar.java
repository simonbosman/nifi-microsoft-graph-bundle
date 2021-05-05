/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.speyk.nifi.microsoft.graph.processors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.models.Event;
import com.microsoft.graph.serializer.*;
import com.microsoft.graph.requests.GraphServiceClient;
import nl.speyk.nifi.microsoft.graph.services.api.MicrosoftGraphCredentialService;
import okhttp3.Request;
import org.apache.commons.io.IOUtils;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.annotation.behavior.ReadsAttribute;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.AbstractProcessor;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.ProcessorInitializationContext;
import org.apache.nifi.processor.Relationship;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Tags({"Speyk", "Microsoft", "Graph", "Calendar", "Client", "Processor"})
@CapabilityDescription("Automate appointment organization and calendaring with  Microsoft Graph REST API.")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute = "", description = "")})
@WritesAttributes({@WritesAttribute(attribute = "", description = "")})
public class InvokeMicrosoftGraphCalendar extends AbstractProcessor {

    public static final String GRAPH_METHOD_GET = "GET";
    public static final String GRAPH_METHOD_POST = "POST";
    public static final String GRAPH_METHOD_PATCH = "PATCH";
    public static final String GRAPH_METHOD_DELETE = "DELETE";

    public final static PropertyDescriptor GRAPH_CONTROLLER_ID = new PropertyDescriptor.Builder()
            .name("mg-cs-auth-controller-id")
            .displayName("Graph Controller Service")
            .description("Graph Controller Service")
            .required(true)
            .identifiesControllerService(MicrosoftGraphCredentialService.class)
            .build();

    public static final PropertyDescriptor GRAPH_PROP_METHOD = new PropertyDescriptor.Builder()
            .name("mg-cs-graph-prop-method")
            .displayName("HTTP Method")
            .description("HTTP request method (GET, POST, PUT, PATCH, DELETE)")
            .required(true)
            .defaultValue(GRAPH_METHOD_POST)
            .allowableValues(GRAPH_METHOD_GET, GRAPH_METHOD_POST, GRAPH_METHOD_PATCH, GRAPH_METHOD_DELETE)
            .build();


    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description(
                    "Appointments that have been successfully written to Microsoft Graph are transferred to this relationship")
            .build();
    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description(
                    "Appointments that could not be written to Microsoft Graph for some reason are transferred to this relationship")
            .build();


    private List<PropertyDescriptor> descriptors;
    private Set<Relationship> relationships;
    private final AtomicReference<GraphServiceClient<Request>> msGraphClientAtomicRef = new AtomicReference<>();

    private String toPrettyFormat(String jsonString) {

        JsonParser parser = new JsonParser();
        JsonObject json = parser.parse(jsonString).getAsJsonObject();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String prettyJson = gson.toJson(json);

        return prettyJson;
    }

    @Override
    protected void init(final ProcessorInitializationContext context) {

        this.relationships = Set.of(REL_SUCCESS, REL_FAILURE);
        this.descriptors = List.of(GRAPH_CONTROLLER_ID, GRAPH_PROP_METHOD);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {

        if (msGraphClientAtomicRef.get() != null) {
            return;
        }

        MicrosoftGraphCredentialService microsoftGraphCredentialService = context.getProperty(GRAPH_CONTROLLER_ID)
                .asControllerService(MicrosoftGraphCredentialService.class);

        msGraphClientAtomicRef.set(microsoftGraphCredentialService.getGraphClient());
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {

        final ComponentLog logger = getLogger();
        final String httpMethod = context.getProperty(GRAPH_PROP_METHOD).getValue();
        /*
        TODO: Implement JSON BATCHING
        https://docs.microsoft.com/en-us/graph/sdks/batch-requests?tabs=java
        */
        FlowFile flowFile = session.get();
        if (!httpMethod.equals(GRAPH_METHOD_GET) & flowFile == null) {
            return;
        }

        if (msGraphClientAtomicRef.get() == null) {
            logger.error("Microsoft Graph Client is not available.");
            flowFile = session.penalize(flowFile);
            session.transfer(flowFile, REL_FAILURE);
            return;
        }

        try {
            ISerializer serializer = Objects.requireNonNull(msGraphClientAtomicRef.get()
                    .getHttpProvider(), "Microsoft Graph Client has no HTTP provider.")
                    .getSerializer();

            //TODO: Put this in a proerty and use expression language
            final String upnName = flowFile.getAttribute("upn-name");
            final Event event = serializer.deserializeObject(session.read(flowFile), Event.class);
            //TODO: Put this in a proerty and use expression language
            event.transactionId = flowFile.getAttribute("content_SHA3-512");

            final Event eventCreated;
            eventCreated = Objects.requireNonNull(msGraphClientAtomicRef.get()
                    .users(upnName)
                    .events()
                    .buildRequest(), "Could not make Microsoft Graph buildRequest.")
                    .post(event);


            String header = "\n\nRESPONSE FROM MICROSOFT GRAPH\n\n";
            String json = header + toPrettyFormat(serializer.serializeObject(eventCreated));

            session.append(flowFile, out -> IOUtils.write(json, out, StandardCharsets.UTF_8));
            session.transfer(flowFile, REL_SUCCESS);

        } catch (ClientException ex) {
            logger.error("Failed to make a Microsoft Graph request.", ex.getMessage());
            flowFile = session.penalize(flowFile);
            session.transfer(flowFile, REL_FAILURE);
            throw new ProcessException(ex);
        }
    }
}

