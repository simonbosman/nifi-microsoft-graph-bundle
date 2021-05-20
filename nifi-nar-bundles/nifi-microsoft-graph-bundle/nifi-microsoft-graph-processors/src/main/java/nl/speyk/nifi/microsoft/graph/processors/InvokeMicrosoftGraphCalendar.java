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

import com.google.common.collect.Lists;import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.microsoft.graph.content.BatchRequestContent;
import com.microsoft.graph.content.BatchResponseContent;
import com.microsoft.graph.content.BatchResponseStep;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.GraphErrorResponse;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.http.HttpMethod;
import com.microsoft.graph.http.HttpResponseCode;
import com.microsoft.graph.models.Event;
import com.microsoft.graph.requests.GraphServiceClient;
import nl.speyk.nifi.microsoft.graph.services.api.MicrosoftGraphCredentialService;
import okhttp3.Request;
import org.apache.commons.io.IOUtils;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.flowfile.FlowFile;
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
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Tags({"Speyk", "Microsoft", "Graph", "Calendar", "Client", "Processor"})
@CapabilityDescription("Automate appointment organization and calendaring with  Microsoft Graph REST API.")
@SeeAlso({})
@ReadsAttributes({})
@WritesAttributes({
        @WritesAttribute(attribute = "invokeMSGraph.java.exception.class", description = "The Java exception class raised when the processor fails"),
        @WritesAttribute(attribute = "invokeMSGraph.java.exception.message", description = "The Java exception message raised when the processor fails")})
public class InvokeMicrosoftGraphCalendar extends AbstractProcessor {
    // Allowed http methods
    public final static String GRAPH_METHOD_GET = "GET";
    public final static String GRAPH_METHOD_POST = "POST";
    public final static String GRAPH_METHOD_PATCH = "PATCH";
    public final static String GRAPH_METHOD_DELETE = "DELETE";

    // Microsoft allows max 4 concurent tasks on a mailbox
    public final static int GRAPH_MAILBOX_CONCURRENCY_LIMIT = 4;

    // We want to retry in case of the following errors
    public final static int GRAPH_HTTP_TO_MANY_REQUESTS = 429;
    public final static int GRAPH_HTTP_SERVICE_UNAVAILABLE = 503;
    public final static int GRAPH_HTTP_GATEWAY_TIMEOUT = 504;

    // flowfile attribute keys returned after reading the response
    public final static String EXCEPTION_CLASS = "invokeMSGraph.java.exception.class";
    public final static String EXCEPTION_MESSAGE = "invokeMSGraph.java.exception.message";

    // Properties
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

    // relationships
    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description(
                    "Appointments that have been successfully written to Microsoft Graph are transferred to this relationship")
            .build();
    public static final Relationship REL_RETRY = new Relationship.Builder()
            .name("retry")
            .description(
                    "Appointments for retrying are transferred to this relationship")
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

    @Override
    protected void init(final ProcessorInitializationContext context) {
        this.relationships = Set.of(REL_SUCCESS, REL_RETRY, REL_FAILURE, REL_ORIGINAL);
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

        // Get the controllerservice responsible for an authenticated GraphClient
        // We will reuse the build GraphClient, hence we put it in a an atomic reference
        MicrosoftGraphCredentialService microsoftGraphCredentialService = context.getProperty(GRAPH_CONTROLLER_ID)
                .asControllerService(MicrosoftGraphCredentialService.class);
        msGraphClientAtomicRef.set(microsoftGraphCredentialService.getGraphClient());
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException, GraphServiceException {
        final ComponentLog logger = getLogger();
        final String httpMethod = context.getProperty(GRAPH_PROP_METHOD).getValue();

        FlowFile requestFlowFile = session.get();
        if (requestFlowFile == null) {
            return;
        }

        if (msGraphClientAtomicRef.get() == null) {
            logger.error("Microsoft Graph Client is not available.");
            requestFlowFile = session.penalize(requestFlowFile);
            session.transfer(requestFlowFile, REL_FAILURE);
            return;
        }

        try {
            /**
             * We expect a flow file with a JsonArray as content
             * Every JsonObject in the array is an event rawsource type
             * @see <a href="https://docs.microsoft.com/en-us/graph/api/resources/event?view=graph-rest-1.0"</a>
             */
            final InputStream inputStream = session.read(requestFlowFile);
            final String eventsJson;
            final Event[] events;

            eventsJson = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            inputStream.close();

            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonElement jsonElement = JsonParser.parseString(eventsJson);

            // Make sure we always have an array of events.
            if (jsonElement.isJsonArray()) {
                events = gson.fromJson(eventsJson, Event[].class);
            } else if (jsonElement.isJsonObject()) {
                events = new Event[]{gson.fromJson(eventsJson, Event.class)};
            } else {
                logger.error("Not valid JSON or empty.");
                events = new Event[]{};
            }

            // Attributes for success and retry flow files
            final Map<String, String> attributes = new Hashtable<>();
            attributes.put("Content-Type", "application/json; charset=utf-8");
            attributes.put("mime.type", "application/json");

            // error codes for retry
            final int[] errorCodes = {GRAPH_HTTP_TO_MANY_REQUESTS, GRAPH_HTTP_SERVICE_UNAVAILABLE, GRAPH_HTTP_GATEWAY_TIMEOUT};

            // Partition the list in sublists of 4 each
            for (List<Event> eventList : Lists.partition(Arrays.asList(events), GRAPH_MAILBOX_CONCURRENCY_LIMIT)) {
                // JSON batch request
                // @see <a href>"https://docs.microsoft.com/en-us/graph/json-batching"</a>
                final Map<String, Event> idEvent = new Hashtable<>();
                final BatchRequestContent batchRequestContent = new BatchRequestContent();

                // Four requests per batch
                for (Event event : eventList) {
                    idEvent.put(batchRequestContent.addBatchRequestStep(msGraphClientAtomicRef.get()
                            .users(event.organizer.emailAddress.address)
                            .events()
                            .buildRequest(), HttpMethod.POST, event), event);
                }

                final BatchResponseContent batchResponseContent = msGraphClientAtomicRef.get()
                        .batch()
                        .buildRequest()
                        .post(batchRequestContent);

                // Route responses
                for (BatchResponseStep batchResponseStep : batchResponseContent.responses) {
                    if (batchResponseStep.status == HttpResponseCode.HTTP_CREATED) {
                        //If event is created, put the response in the succes flow file
                        FlowFile succesFlowFile = session.create();
                        succesFlowFile = session.putAllAttributes(succesFlowFile, attributes);
                        session.write(succesFlowFile, out -> IOUtils.write(gson.toJson(batchResponseStep.body), out, StandardCharsets.UTF_8));
                        session.transfer(succesFlowFile, REL_SUCCESS);
                    } else if (Arrays.stream(errorCodes).anyMatch(e -> e == batchResponseStep.status)) {
                        //In case of the following error codes (429, 503, 503)
                        //put the event from the hashtable in a flow file and route to retry
                        final Event[] evtRetry = {idEvent.get(batchResponseStep.id)};
                        String json = gson.toJson(evtRetry, Event[].class);

                        FlowFile retryFLowFile = session.create();
                        retryFLowFile = session.putAllAttributes(retryFLowFile, attributes);

                        //Use the RetryFLow processor for setting the max retries
                        final String attrNumRetries = requestFlowFile.getAttribute("flowfile.retries");
                        final String numRetries = (attrNumRetries != null) ? attrNumRetries : "1";

                        retryFLowFile = session.putAttribute(retryFLowFile, "flowfile.retries", numRetries);

                        session.write(retryFLowFile, out -> IOUtils.write(json, out, StandardCharsets.UTF_8));
                        session.transfer(retryFLowFile, REL_RETRY);
                    } else {
                        //This will throw a GraphServiceException or just return null
                        batchResponseStep.getDeserializedBody(GraphErrorResponse.class);
                    }
                }
            }
            // The original flowfile hasn't changed
            session.transfer(requestFlowFile, REL_ORIGINAL);

        }
        catch (GraphServiceException exceptionGraph) {
            //Error in performing request to Microsoft Graph
            //We can't recover from this so route to failure handler
            routToFaillure(requestFlowFile, logger, session, context, exceptionGraph);
        }
        // sometimes JSON batching causes socket timeouts; catch them and retry
        catch (ClientException exceptionToRetry) {
            logger.error("Failed to create an event in Microsoft Graph due network problems. {}. More detailed information may be available in " +
                            "the NiFi logs.",
                    new Object[]{exceptionToRetry.getLocalizedMessage()}, exceptionToRetry);
            // transfer original to retry
            session.transfer(requestFlowFile, REL_RETRY);
            context.yield();
        }
        catch (final Exception e) {
            routToFaillure(requestFlowFile, logger, session, context, e);
        }
    }

    private void routToFaillure(FlowFile requestFlowFile, final ComponentLog logger,
                                final ProcessSession session, final ProcessContext context, final Exception e) {
        // penalize or yield
        if (requestFlowFile != null) {
            logger.error("Routing to {} due to exception: {}", new Object[]{REL_FAILURE.getName(), e}, e);
            requestFlowFile = session.penalize(requestFlowFile);
            requestFlowFile = session.putAttribute(requestFlowFile, EXCEPTION_CLASS, e.getClass().getName());
            requestFlowFile = session.putAttribute(requestFlowFile, EXCEPTION_MESSAGE, e.getMessage() +
                    Arrays.stream(e.getStackTrace()).collect(Collectors.toList()));
            // transfer original to failure
            session.transfer(requestFlowFile, REL_FAILURE);
        } else {
            logger.error("Yielding processor due to exception encountered as a source processor: {}", e);
            context.yield();
        }
    }
}

