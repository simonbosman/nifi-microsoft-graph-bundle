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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Tags({"Speyk", "Microsoft", "Graph", "Calendar", "Client", "Processor"})
@CapabilityDescription("Automate appointment organization and calendaring with  Microsoft Graph REST API.")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute = "i", description = "The Java exception class raised when the processor fails")})
@WritesAttributes({@WritesAttribute(attribute = "invokeMSGraph.java.exception.message", description = "he Java exception message raised when the processor fails")})
public class InvokeMicrosoftGraphCalendar extends AbstractProcessor {

    public static final String GRAPH_METHOD_GET = "GET";
    public static final String GRAPH_METHOD_POST = "POST";
    public static final String GRAPH_METHOD_PATCH = "PATCH";
    public static final String GRAPH_METHOD_DELETE = "DELETE";
    public final static String EXCEPTION_CLASS = "invokeMSGraph.java.exception.class";
    public final static String EXCEPTION_MESSAGE = "invokeMSGraph.java.exception.message";


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
    public static final Relationship REL_ORIGINAL = new Relationship.Builder()
            .name("original")
            .description(
                    "The original appointments are transferred to this relationship")
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
        this.relationships = Set.of(REL_SUCCESS, REL_FAILURE, REL_ORIGINAL);
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

        FlowFile requestFlowFile = session.get();

        if (!httpMethod.equals(GRAPH_METHOD_GET) & requestFlowFile == null) {
            return;
        }

        if (msGraphClientAtomicRef.get() == null) {
            logger.error("Microsoft Graph Client is not available.");
            requestFlowFile = session.penalize(requestFlowFile);
            session.transfer(requestFlowFile, REL_FAILURE);
            return;
        }

        try {
            ISerializer serializer = Objects.requireNonNull(msGraphClientAtomicRef.get()
                    .getHttpProvider(), "Microsoft Graph Client has no HTTP provider.")
                    .getSerializer();

            final String eventsJson;
            final InputStream inputStream = session.read(requestFlowFile);

            eventsJson = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            inputStream.close();

            Gson gson = new Gson();
            Event[] events = gson.fromJson(eventsJson, Event[].class);

            final Event eventCreated;
            eventCreated = Objects.requireNonNull(msGraphClientAtomicRef.get()
                    .users(events[0].organizer.emailAddress.address)
                    .events()
                    .buildRequest(), "Could not make Microsoft Graph buildRequest.")
                    .post(events[0]);

            final FlowFile succesFlowFile = session.create();
            String json = toPrettyFormat(serializer.serializeObject(eventCreated));

            session.write(succesFlowFile, out -> IOUtils.write(json, out, StandardCharsets.UTF_8));
            session.transfer(succesFlowFile, REL_SUCCESS);

            session.transfer(requestFlowFile, REL_ORIGINAL);

        } catch (final Exception e) {
            // penalize or yield
            if (requestFlowFile != null) {
                logger.error("Routing to {} due to exception: {}", new Object[]{REL_FAILURE.getName(), e}, e);
                requestFlowFile = session.penalize(requestFlowFile);
                requestFlowFile = session.putAttribute(requestFlowFile, EXCEPTION_CLASS, e.getClass().getName());
                requestFlowFile = session.putAttribute(requestFlowFile, EXCEPTION_MESSAGE, e.getMessage());
                // transfer original to failure
                session.transfer(requestFlowFile, REL_FAILURE);
            } else {
                logger.error("Yielding processor due to exception encountered as a source processor: {}", e);
                context.yield();
            }
        }
    }
}

