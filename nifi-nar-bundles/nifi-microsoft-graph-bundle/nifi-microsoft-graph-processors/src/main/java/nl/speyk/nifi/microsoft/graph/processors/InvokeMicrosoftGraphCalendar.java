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

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.microsoft.graph.content.BatchRequestContent;
import com.microsoft.graph.content.BatchResponseContent;
import com.microsoft.graph.content.BatchResponseStep;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.http.HttpMethod;
import com.microsoft.graph.http.HttpResponseCode;
import com.microsoft.graph.models.Event;
import com.microsoft.graph.models.FreeBusyStatus;
import com.microsoft.graph.requests.EventCollectionPage;
import com.microsoft.graph.requests.EventCollectionRequestBuilder;
import com.microsoft.graph.requests.GraphServiceClient;
import nl.speyk.nifi.microsoft.graph.services.api.MicrosoftGraphCredentialService;
import okhttp3.Request;
import org.apache.commons.io.IOUtils;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.distributed.cache.client.Deserializer;
import org.apache.nifi.distributed.cache.client.DistributedMapCacheClient;
import org.apache.nifi.distributed.cache.client.Serializer;
import org.apache.nifi.distributed.cache.client.exception.DeserializationException;
import org.apache.nifi.distributed.cache.client.exception.SerializationException;
import org.apache.nifi.expression.ExpressionLanguageScope;
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
import org.apache.nifi.processor.util.StandardValidators;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.*;
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

    //For how many full working weeks we will synchronize
    //the appointments in advance
    public final static int GRAPH_WEEKS_IN_ADVANCE = 3;

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
            .description("Graph Controller Service used for creating a connection to the Graph")
            .required(true)
            .identifiesControllerService(MicrosoftGraphCredentialService.class)
            .build();

    public final static PropertyDescriptor GRAPH_DISTRIBUTED_MAPCACHE = new PropertyDescriptor.Builder()
            .name("mg-cs-mapcache-id")
            .displayName("Distributed mapcache client")
            .description("Distributed mapcache client used for detecting changes")
            .required(true)
            .identifiesControllerService(DistributedMapCacheClient.class)
            .build();

    public static final PropertyDescriptor GRAPH_PROP_METHOD = new PropertyDescriptor.Builder()
            .name("mg-cs-graph-prop-method")
            .displayName("HTTP Method")
            .description("HTTP request method (GET, POST, PUT, PATCH, DELETE)")
            .required(true)
            .defaultValue(GRAPH_METHOD_POST)
            .allowableValues(GRAPH_METHOD_GET, GRAPH_METHOD_POST, GRAPH_METHOD_PATCH, GRAPH_METHOD_DELETE)
            .build();

    public static final PropertyDescriptor GRAPH_USER_ID = new PropertyDescriptor.Builder()
            .name("mg-cs-user-id")
            .displayName("User Id")
            .description("The user id to be used for Microsoft Graph api calls.")
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
            .defaultValue("${'upn-name'}")
            .required(true)
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
    private final Serializer<String> keySerializer = new StringSerializer();
    private final Serializer<byte[]> valueSerializer = new CacheValueSerializer();
    private final Deserializer<byte[]> valueDeserializer = new CacheValueDeserializer();

    private void routeToFaillure(FlowFile requestFlowFile, final ComponentLog logger,
                                 final ProcessSession session, final ProcessContext context, final Exception e) {
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

    private List<Event> getGraphEvents(String userId) {
        //Get all events for the next tree full working weeks
        final LocalDate dateEnd = LocalDate.now();
        final LocalDate dateStartInitial = LocalDate.now().plusWeeks(GRAPH_WEEKS_IN_ADVANCE);
        final LocalDate dateStart = dateStartInitial.plusDays(7 - dateStartInitial.getDayOfWeek().getValue());

        EventCollectionPage eventCollectionPage = msGraphClientAtomicRef.get()
                .users(userId)
                .events()
                .buildRequest()
                .select("id, transactionId, subject, Body, start, end, location, showAs")
                .filter(String.format("end/dateTime ge '%s' and start/dateTime lt '%s'", dateEnd.toString(), dateStart.toString()))
                .get();

        //Loop trrough available pages and fill list of events
        List<Event> events = new LinkedList<>();
        while (eventCollectionPage != null) {
            events.addAll(eventCollectionPage.getCurrentPage());
            final EventCollectionRequestBuilder nextPage = eventCollectionPage.getNextPage();
            if (nextPage == null) {
                break;
            } else {
                eventCollectionPage = nextPage.buildRequest().get();
            }
        }
        return events;
    }

    //Get the difference of the given lists of events.
    private List<Event> eventsDiff(List<Event> eventsSource, List<Event> eventsDest) {
        Set<String> setSource = new HashSet<>();
        for (Event evt : eventsSource) {
            setSource.add(evt.transactionId);
        }

        Set<String> setGraph = new HashSet<>();
        for (Event evt : eventsDest) {
            setGraph.add(evt.transactionId);
        }

        Set<String> difference = new HashSet<>(setSource);
        difference.removeAll(setGraph);

        List<Event> diffEvents = eventsSource.stream().filter(event -> difference.contains(event.transactionId)).collect(Collectors.toList());
        return diffEvents;
    }

    private byte[] createHashedEvent(Event evt) throws NoSuchAlgorithmException {
        StringBuilder sb = new StringBuilder();
        if (evt.start != null) sb.append(evt.start.dateTime);
        if (evt.end != null) sb.append(evt.end.dateTime);
        sb.append(evt.subject);
        if (evt.body != null) sb.append(evt.body.content);
        if (evt.location != null) sb.append(evt.location.displayName);
        sb.append(evt.showAs);

        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        byte[] hashedEvent = messageDigest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));

        return hashedEvent;
    }

    //Patch events for user if event has changed
    private void patchGraphEvents(String userId, List<Event> eventsSource, List<Event> eventsGraph, DistributedMapCacheClient cache)
            throws IOException, NoSuchAlgorithmException {
        //Are there any changes in the source event?
        //Patch the graph with the changed event
        //Update the cache with the changed event
        for (Event evt : eventsSource) {
            byte[] hashedEvt = createHashedEvent(evt);
            byte[] hashedCachedEvt = cache.get(evt.transactionId, keySerializer, valueDeserializer);

            if (hashedCachedEvt != null & !Arrays.equals(hashedEvt, hashedCachedEvt)) {
                try {
                    final Event eventToPatch = eventsGraph.stream().filter(event -> event.transactionId.equals(evt.transactionId)).findAny().get();
                    msGraphClientAtomicRef.get()
                            .users(userId)
                            .events(eventToPatch.id)
                            .buildRequest()
                            .patch(evt);
                    cache.put(evt.transactionId, hashedEvt, keySerializer, valueSerializer);
                } catch (NoSuchElementException e) {
                    getLogger().error(String.format("Event with transactionId %s couldn't be patched.", evt.transactionId));
                }
            }
        }
        //Are there any changes in the graph event?
        //Patch the graph event with the original event
        //from the list of source events
        //Effectually restoring manual changes to the graph
        for (Event evt : eventsGraph) {
            byte[] hashedEvt = createHashedEvent(evt);
            byte[] hashedCashedEvt = cache.get(evt.transactionId, keySerializer, valueDeserializer);

            if (hashedCashedEvt != null & !Arrays.equals(hashedEvt, hashedCashedEvt)) {
                try {
                    final Event eventPatchVal = eventsSource.stream().filter(event -> event.transactionId.equals(evt.transactionId)).findAny().get();
                    eventPatchVal.showAs = FreeBusyStatus.BUSY;
                    msGraphClientAtomicRef.get()
                            .users(userId)
                            .events(evt.id)
                            .buildRequest()
                            .patch(eventPatchVal);
                } catch (NoSuchElementException e) {
                    getLogger().error(String.format("Event with transactionId %s couldn't be patched.", evt.transactionId));
                }
            }
        }

        //Is the set of events greater in the graph than in source?
        //Make event in the graph tentative
        List<Event> eventsToDelete = eventsDiff(eventsGraph, eventsSource);
        for (Event evt : eventsToDelete) {
            //Is the event managed by DIS? If not skip.
            if (cache.get(evt.transactionId, keySerializer, valueDeserializer) == null)
                continue;
            Event eventPatchVal = new Event();
            eventPatchVal.start = evt.start;
            eventPatchVal.end = evt.end;
            eventPatchVal.transactionId = evt.transactionId;
            eventPatchVal.subject = evt.subject;
            eventPatchVal.body = evt.body;
            eventPatchVal.location = evt.location;
            eventPatchVal.showAs = FreeBusyStatus.TENTATIVE;

            try {
                msGraphClientAtomicRef.get()
                        .users(userId)
                        .events(evt.id)
                        .buildRequest()
                        .patch(eventPatchVal);
            } catch (NoSuchElementException e) {
                getLogger().error(String.format("Event with transactionId %s couldn't be patched.", evt.transactionId));
            }
        }
    }

    //Put a hash of the events in a distributed mapcache so we can detect changes
    private void putEventsMapCache(List<Event> events, DistributedMapCacheClient cache) throws IOException, NoSuchAlgorithmException {
        for (Event evt : events) {
            byte[] hashedEvt = createHashedEvent(evt);
            cache.put(evt.transactionId, hashedEvt, keySerializer, valueSerializer);
        }
    }

    private void putBatchGraphEvents(final ProcessContext context, final ProcessSession session, List<Event> events, String userId, final FlowFile requestFlowFile) throws ClientException {
        // Attributes for success and retry flow files
        final Map<String, String> attributes = new Hashtable<>();
        attributes.put("Content-Type", "application/json; charset=utf-8");
        attributes.put("mime.type", "application/json");

        // error codes for retry
        final int[] errorCodes = {GRAPH_HTTP_TO_MANY_REQUESTS, GRAPH_HTTP_SERVICE_UNAVAILABLE, GRAPH_HTTP_GATEWAY_TIMEOUT};
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // Partition the list in sublists of 4 each
        for (List<Event> eventList : Lists.partition(events, GRAPH_MAILBOX_CONCURRENCY_LIMIT)) {
            // JSON batch request
            // @see <a href>"https://docs.microsoft.com/en-us/graph/json-batching"</a>
            final Map<String, Event> idEvent = new Hashtable<>();
            final BatchRequestContent batchRequestContent = new BatchRequestContent();

            // Four requests per batch
            for (Event event : eventList) {
                idEvent.put(batchRequestContent.addBatchRequestStep(msGraphClientAtomicRef.get()
                        .users(userId)
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
                    //In case of the following error codes (429, 503, 504)
                    //put the event from the hashtable in a flow file and route to retry
                    final Event[] evtRetry = {idEvent.get(batchResponseStep.id)};
                    String json = gson.toJson(evtRetry, Event[].class);

                    FlowFile retryFLowFile = session.create();
                    attributes.put("upn-name", userId);
                    retryFLowFile = session.putAllAttributes(retryFLowFile, attributes);

                    //Use the RetryFLow processor for setting the max retries
                    final String attrNumRetries = requestFlowFile.getAttribute("flowfile.retries");
                    final String numRetries = (attrNumRetries != null) ? attrNumRetries : "1";

                    retryFLowFile = session.putAttribute(retryFLowFile, "flowfile.retries", numRetries);

                    session.write(retryFLowFile, out -> IOUtils.write(json, out, StandardCharsets.UTF_8));
                    session.transfer(retryFLowFile, REL_RETRY);
                } else {
                    //This will throw a GraphServiceException or return Event
                    batchResponseStep.getDeserializedBody(Event.class);
                }
            }
        }
    }

    @Override
    protected void init(final ProcessorInitializationContext context) {
        this.relationships = Set.of(REL_SUCCESS, REL_RETRY, REL_FAILURE, REL_ORIGINAL);
        this.descriptors = List.of(GRAPH_CONTROLLER_ID, GRAPH_DISTRIBUTED_MAPCACHE, GRAPH_PROP_METHOD, GRAPH_USER_ID);
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
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        FlowFile requestFlowFile = session.get();
        if (requestFlowFile == null) {
            return;
        }

        final ComponentLog logger = getLogger();
        final String httpMethod = context.getProperty(GRAPH_PROP_METHOD).getValue();
        final String userId = context.getProperty(GRAPH_USER_ID).evaluateAttributeExpressions(requestFlowFile).getValue();

        if (msGraphClientAtomicRef.get() == null) {
            logger.error("Microsoft Graph Client is not available.");
            requestFlowFile = session.penalize(requestFlowFile);
            session.transfer(requestFlowFile, REL_FAILURE);
            return;
        }

        // the cache client used to interact with the distributed cache
        final DistributedMapCacheClient cache = context.getProperty(GRAPH_DISTRIBUTED_MAPCACHE).asControllerService(DistributedMapCacheClient.class);

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

            final List<Event> eventsSource = Arrays.asList(events);
            final List<Event> eventsGraph = getGraphEvents(userId);

            //Are there any events that have changed?
            //If so patch them in the graph
            patchGraphEvents(userId, eventsSource, eventsGraph, cache);

            //Only synchronize events that are not already in the graph
            final List<Event> eventsToGraph = eventsDiff(eventsSource, eventsGraph);

            //Put the events in batches in the Microsoft Graph
            putBatchGraphEvents(context, session, eventsToGraph, userId, requestFlowFile);

            //Put the events in a mapcache
            putEventsMapCache(eventsToGraph, cache);

            // The original flowfile hasn't changed
            session.transfer(requestFlowFile, REL_ORIGINAL);

        } catch (GraphServiceException | IOException | NoSuchAlgorithmException e) {
            //Error in performing request to Microsoft Graph
            //We can't recover from this so route to failure handler
            routeToFaillure(requestFlowFile, logger, session, context, e);
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
    }

    public static class CacheValueSerializer implements Serializer<byte[]> {

        @Override
        public void serialize(final byte[] bytes, final OutputStream out) throws SerializationException, IOException {
            out.write(bytes);
        }
    }

    public static class CacheValueDeserializer implements Deserializer<byte[]> {

        @Override
        public byte[] deserialize(final byte[] input) throws DeserializationException, IOException {
            if (input == null || input.length == 0) {
                return null;
            }
            return input;
        }
    }

    //Simple string serializer, used for serializing the cache key
    public static class StringSerializer implements Serializer<String> {

        @Override
        public void serialize(final String value, final OutputStream out) throws SerializationException, IOException {
            out.write(value.getBytes(StandardCharsets.UTF_8));
        }
    }
}

