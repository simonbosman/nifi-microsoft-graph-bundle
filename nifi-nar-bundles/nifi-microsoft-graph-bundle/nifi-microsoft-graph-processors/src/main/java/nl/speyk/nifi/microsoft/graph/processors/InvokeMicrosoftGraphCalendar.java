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
import com.microsoft.graph.models.BodyType;
import com.microsoft.graph.models.Event;
import com.microsoft.graph.models.FreeBusyStatus;
import com.microsoft.graph.requests.EventCollectionPage;
import com.microsoft.graph.requests.EventCollectionRequestBuilder;
import com.microsoft.graph.requests.GraphServiceClient;
import nl.speyk.nifi.microsoft.graph.services.api.MicrosoftGraphCredentialService;
import okhttp3.Request;
import org.apache.commons.io.IOUtils;
import org.apache.nifi.annotation.behavior.ReadsAttributes;
import org.apache.nifi.annotation.behavior.WritesAttribute;
import org.apache.nifi.annotation.behavior.WritesAttributes;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.SeeAlso;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnScheduled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.distributed.cache.client.Deserializer;
import org.apache.nifi.distributed.cache.client.DistributedMapCacheClient;
import org.apache.nifi.distributed.cache.client.Serializer;
import org.apache.nifi.distributed.cache.client.exception.DeserializationException;
import org.apache.nifi.distributed.cache.client.exception.SerializationException;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.*;
import org.apache.nifi.processor.exception.ProcessException;
import org.apache.nifi.processor.util.StandardValidators;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Entities;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static nl.speyk.nifi.microsoft.graph.processors.utils.CalendarAttributes.EXCEPTION_CLASS;
import static nl.speyk.nifi.microsoft.graph.processors.utils.CalendarAttributes.EXCEPTION_MESSAGE;
import static nl.speyk.nifi.microsoft.graph.processors.utils.CalendarAttributes.GRAPH_HTTP_GATEWAY_TIMEOUT;
import static nl.speyk.nifi.microsoft.graph.processors.utils.CalendarAttributes.GRAPH_HTTP_SERVICE_UNAVAILABLE;
import static nl.speyk.nifi.microsoft.graph.processors.utils.CalendarAttributes.GRAPH_HTTP_TO_MANY_REQUESTS;
import static nl.speyk.nifi.microsoft.graph.processors.utils.CalendarAttributes.GRAPH_MAILBOX_CONCURRENCY_LIMIT;
import static nl.speyk.nifi.microsoft.graph.processors.utils.CalendarAttributes.GRAPH_WEEKS_IN_ADVANCE;

@Tags({"Speyk", "Microsoft", "Graph", "Calendar", "Client", "Processor"})
@CapabilityDescription("Automate appointment organization and calendaring with  Microsoft Graph REST API.")
@SeeAlso({})
@ReadsAttributes({})
@WritesAttributes({
        @WritesAttribute(attribute = "invokeMSGraph.java.exception.class", description = "The Java exception class raised when the processor fails"),
        @WritesAttribute(attribute = "invokeMSGraph.java.exception.message", description = "The Java exception message raised when the processor fails")})
public class InvokeMicrosoftGraphCalendar extends AbstractProcessor {

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

    public static final PropertyDescriptor GRAPH_USER_ID = new PropertyDescriptor.Builder()
            .name("mg-cs-user-id")
            .displayName("User Id")
            .description("The user id to be used for Microsoft Graph api calls.")
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
            .defaultValue("${'upn-name'}")
            .required(true)
            .build();

    public static final PropertyDescriptor GRAPH_IS_UPDATE = new PropertyDescriptor.Builder()
            .name("mg-cs-is-update")
            .displayName("Is Update")
            .description("Attribute to use for isUpdate value")
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
            .defaultValue("${'is-update'}")
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
    private final AtomicReference<GraphServiceClient<Request>> msGraphClientAtomicRef = new AtomicReference<>();
    private final Serializer<String> keySerializer = new StringSerializer();
    private final Serializer<byte[]> valueSerializer = new CacheValueSerializer();
    private final Deserializer<byte[]> valueDeserializer = new CacheValueDeserializer();
    private List<PropertyDescriptor> descriptors;
    private Set<Relationship> relationships;

    private void routeToFailure(FlowFile requestFlowFile, final ComponentLog logger,
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

    private void routeToSuccess(final ProcessSession session, Object content) {
        // Attributes for success and retry flow files
        final Map<String, String> attributes = new Hashtable<>();
        attributes.put("Content-Type", "application/json; charset=utf-8");
        attributes.put("mime.type", "application/json");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(content);

        //If event is created, put the response in the succes flow file
        FlowFile succesFlowFile = session.create();
        succesFlowFile = session.putAllAttributes(succesFlowFile, attributes);
        session.write(succesFlowFile, out -> IOUtils.write(json, out, StandardCharsets.UTF_8));
        session.transfer(succesFlowFile, REL_SUCCESS);
    }

    private List<Event> getGraphEvents(String userId) {
        //Get all events for the next tree full working weeks
        final LocalDate dateEnd = LocalDate.now();
        final LocalDate dateStartInitial = LocalDate.now().plusWeeks(GRAPH_WEEKS_IN_ADVANCE);
        final LocalDate dateStart = dateStartInitial.plusDays(7 - dateStartInitial.getDayOfWeek().getValue());

        EventCollectionPage eventCollectionPage = Objects.requireNonNull(msGraphClientAtomicRef.get()
                        .users(userId)
                        .events()
                        .buildRequest())
                .select("id, transactionId, subject, body, bodyPreview, start, end, location, showAs")
                .filter(String.format("end/dateTime ge '%s' and start/dateTime lt '%s'", dateEnd, dateStart))
                .get();

        //Loop trough available pages and fill list of events
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
            if (evt.transactionId == null)
                continue;
            setSource.add(evt.transactionId);
        }

        Set<String> setGraph = new HashSet<>();
        for (Event evt : eventsDest) {
            if (evt.transactionId == null)
                continue;
            setGraph.add(evt.transactionId);
        }

        Set<String> difference = new HashSet<>(setSource);
        difference.removeAll(setGraph);

        return eventsSource.stream().filter(event -> difference.contains(event.transactionId)).collect(Collectors.toList());
    }

    private byte[] createHashedEvent(Event evt, @NotNull Boolean isGraphEvent) throws NoSuchAlgorithmException {
        StringBuilder sb = new StringBuilder();
        //A graph event has a different datetime format, hence we convert it.
        if (isGraphEvent) {
            DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").withZone(ZoneId.of("Europe/Berlin"));
            ZonedDateTime dtStart = LocalDateTime.parse(evt.start.dateTime).atZone(ZoneId.of("UTC"));
            sb.append(dtStart.format(dtFormatter));
            ZonedDateTime dtEnd = LocalDateTime.parse(evt.end.dateTime).atZone(ZoneId.of("UTC"));
            sb.append(dtEnd.format(dtFormatter));
         } else {
            if (evt.start != null) sb.append(evt.start.dateTime);
            if (evt.end != null) sb.append(evt.end.dateTime);
        }
        sb.append(evt.subject);
        if (evt.showAs != null) sb.append(evt.showAs.name());
        if (evt.location != null) sb.append(evt.location.displayName);
        else if (evt.locations != null) {
            String joinedLocations = evt.locations.stream().map((loc) -> {
                return loc.displayName;
            }).collect(Collectors.joining("; "));
            sb.append(joinedLocations);
        }
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        return messageDigest.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    private byte[] createHashedEvent(Event evt) throws NoSuchAlgorithmException, ParseException {
        return createHashedEvent(evt, false);
    }

    //Patch events for user if event has changed
    private void patchGraphEvents(String userId,
                                  List<Event> eventsSource,
                                  List<Event> eventsGraph,
                                  DistributedMapCacheClient cache,
                                  ProcessSession session,
                                  boolean isUpdate)
            throws IOException, NoSuchAlgorithmException, ParseException {
        //Are there any changes in the source event?
        //Patch the graph with the changed event
        //Update the cache with the changed event
        for (Event evt : eventsSource) {
            try {
                //Sanitize body content
                if (evt.body != null && evt.body.content != null) {
                    evt.body.contentType = BodyType.HTML;
                    evt.body.content = Entities.unescape(Jsoup.parse(evt.body.content).html());
                }
                byte[] hashedEvt = createHashedEvent(evt);
                if (evt.transactionId == null) {
                    throw new IllegalArgumentException("TransactionId can't be empty");
                }
                byte[] hashedCachedEvt = cache.get(evt.transactionId, keySerializer, valueDeserializer);

                if (hashedCachedEvt != null & !Arrays.equals(hashedEvt, hashedCachedEvt)) {
                    final Event eventToPatch = eventsGraph.stream()
                            .filter(event -> event.transactionId != null)
                            .filter(event -> event.transactionId.equals(evt.transactionId))
                            .findAny().get();
                    routeToSuccess(session, msGraphClientAtomicRef.get()
                            .users(userId)
                            .events(Objects.requireNonNull(eventToPatch.id))
                            .buildRequest()
                            .patch(evt));
                    //cache.put(evt.transactionId, hashedEvt, keySerializer, valueSerializer);
                }
            } catch (NoSuchElementException e) {
                getLogger().error(String.format("Source event with transactionId %s couldn't be patched. " +
                        "Most likely the event has just been created", evt.transactionId));
            }
        }
        //If we are updating, we stop here
        if (isUpdate) return;

        //Are there any changes in the graph event?
        //Patch the graph event with the original event
        //from the list of source events
        //Effectually restoring manual changes to the graph
        for (Event evt : eventsGraph) {
            //Event not managed by DIS, so continue
            if (evt.transactionId == null) continue;
            try {
                byte[] hashedCashedEvt = cache.get(evt.transactionId, keySerializer, valueDeserializer);
                //Graph event isn't an event managed by DIS, so skip
                if (hashedCashedEvt == null)
                    continue;
                byte[] hashedEvt = createHashedEvent(evt, true);
                //Graph event has changed, so fix this
                if (!Arrays.equals(hashedEvt, hashedCashedEvt)) {
                    //Is the event available in the source dataset? If not it's deleted.
                    final Event eventPatchVal = eventsSource.stream()
                            .filter(event -> {
                                return Objects.equals(event.transactionId, evt.transactionId);
                            }).findAny().orElseThrow();

                    Object content = msGraphClientAtomicRef.get()
                            .users(userId)
                            .events(Objects.requireNonNull(evt.id))
                            .buildRequest()
                            .patch(eventPatchVal);
                    routeToSuccess(session, content);
                }
            } catch (NoSuchElementException e) {
                String unknown = "unknown";
                getLogger().info(String.format("Graph event with transactionId %s with status %s and subject %s couldn't be patched. " +
                        "Most likely the event is deleted from the source dataset.", evt.transactionId, evt.showAs != null ? evt.showAs.name() : unknown, evt.subject));
            }
        }

        //Is the set of events greater in the graph than in source?
        //Make event in the graph tentative
        List<Event> eventsToDelete = eventsDiff(eventsGraph, eventsSource);
        for (Event evt : eventsToDelete) {
            //Is the event managed by DIS? If not skip.
            if (cache.get(evt.transactionId, keySerializer, valueDeserializer) == null)
                continue;
            //Is the event already tentative?
            if (Objects.equals(evt.showAs, FreeBusyStatus.TENTATIVE))
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
                routeToSuccess(session, msGraphClientAtomicRef.get()
                        .users(userId)
                        .events(Objects.requireNonNull(evt.id))
                        .buildRequest()
                        .patch(eventPatchVal));
            } catch (NoSuchElementException e) {
                getLogger().error(String.format("Graph event with transactionId %s couldn't be patched to state tentative.", evt.transactionId));
            }
        }
    }

    //Put a hash of the events in a distributed map cache, so we can detect changes
    private void putEventsMapCache(List<Event> events, DistributedMapCacheClient cache) throws IOException, NoSuchAlgorithmException, ParseException {
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

        // Partition the list in sub lists of 4 each
        for (List<Event> eventList : Lists.partition(events, GRAPH_MAILBOX_CONCURRENCY_LIMIT)) {
            // JSON batch request
            // @see <a href>"https://docs.microsoft.com/en-us/graph/json-batching"</a>
            final Map<String, Event> idEvent = new Hashtable<>();
            final BatchRequestContent batchRequestContent = new BatchRequestContent();

            // Four requests per batch
            for (Event event : eventList) {
                //Sanitize body content
                if (event.body != null && event.body.content != null) {
                    event.body.contentType = BodyType.HTML;
                    event.body.content = Entities.unescape(Jsoup.parse(event.body.content).html());
                }
                idEvent.put(batchRequestContent.addBatchRequestStep(Objects.requireNonNull(msGraphClientAtomicRef.get()
                        .users(userId)
                        .events()
                        .buildRequest()), HttpMethod.POST, event), event);
            }

            final BatchResponseContent batchResponseContent = msGraphClientAtomicRef.get()
                    .batch()
                    .buildRequest()
                    .post(batchRequestContent);

            if (batchResponseContent != null && batchResponseContent.responses != null) {
                for (BatchResponseStep<JsonElement> batchResponseStep : batchResponseContent.responses) {
                    if (batchResponseStep.status == HttpResponseCode.HTTP_CREATED) {
                        //If event is created, put the response in the success flow file
                        routeToSuccess(session, batchResponseStep.body);
                    } else if (Arrays.stream(errorCodes).anyMatch(e -> e == batchResponseStep.status)) {
                        //In case of the following error codes (429, 503, 504)
                        //put the event from the hashtable in a flow file and route to retry
                        final Event[] evtRetry = {idEvent.get(batchResponseStep.id)};

                        String json = Objects.requireNonNull(msGraphClientAtomicRef.get().getSerializer()).serializeObject(evtRetry);

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
            } else {
                getLogger().error("Batch request resulted in an error: ", batchResponseContent);
            }
        }
    }

    @Override
    protected void init(final ProcessorInitializationContext context) {
        this.relationships = Set.of(REL_SUCCESS, REL_RETRY, REL_FAILURE, REL_ORIGINAL);
        this.descriptors = List.of(GRAPH_CONTROLLER_ID, GRAPH_DISTRIBUTED_MAPCACHE, GRAPH_USER_ID, GRAPH_IS_UPDATE);
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
        // Get the controller-service responsible for an authenticated GraphClient
        // We will reuse the build GraphClient, hence we put it in a an atomic reference
        MicrosoftGraphCredentialService microsoftGraphCredentialService = context.getProperty(GRAPH_CONTROLLER_ID)
                .asControllerService(MicrosoftGraphCredentialService.class);
        msGraphClientAtomicRef.set(microsoftGraphCredentialService.getGraphClient());
    }

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {
        //Here we do the actual work
        FlowFile requestFlowFile = session.get();
        if (requestFlowFile == null) {
            return;
        }

        final ComponentLog logger = getLogger();
        final String userId = context.getProperty(GRAPH_USER_ID).evaluateAttributeExpressions(requestFlowFile).getValue();
        final boolean isUpdate = Boolean.parseBoolean(context.getProperty(GRAPH_IS_UPDATE).evaluateAttributeExpressions(requestFlowFile).getValue());

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

            JsonElement jsonElement = JsonParser.parseString(eventsJson);

            // Make sure we always have an array of events.
            if (jsonElement.isJsonArray()) {
                events = Objects.requireNonNull(msGraphClientAtomicRef.get().getSerializer()).deserializeObject(eventsJson, Event[].class);
            } else if (jsonElement.isJsonObject()) {
                events = new Event[]{Objects.requireNonNull(msGraphClientAtomicRef.get().getSerializer()).deserializeObject(eventsJson, Event.class)};
            } else {
                logger.error("Not valid JSON or empty.");
                events = new Event[]{};
            }

            //Source set is empty, we don't want to
            //set all the events on tentative, so stop with an error
            if (events == null || events.length == 0) {
                throw new ProcessException("The source dataset is empty.");
            }

            final List<Event> eventsSource = Arrays.asList(events);
            final List<Event> eventsGraph = getGraphEvents(userId);

            //Only synchronize events that are not already in the graph
            final List<Event> eventsToGraph = eventsDiff(eventsSource, eventsGraph);

            //Put the events in batches in the Microsoft Graph
            putBatchGraphEvents(context, session, eventsToGraph, userId, requestFlowFile);

            //Are there any events that have changed?
            //If so patch them in the graph
            patchGraphEvents(userId, eventsSource, eventsGraph, cache, session, isUpdate);

            //Put the events in a map cache
            putEventsMapCache(eventsSource, cache);

            // The original flow file hasn't changed
            session.transfer(requestFlowFile, REL_ORIGINAL);

        } catch (GraphServiceException | IOException | NoSuchAlgorithmException | IllegalArgumentException | ParseException e) {
            //Error in performing request to Microsoft Graph
            //We can't recover from this so route to failure handler
            routeToFailure(requestFlowFile, logger, session, context, e);
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

