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
import com.microsoft.graph.serializer.ISerializer;
import org.apache.commons.io.IOUtils;
import org.apache.nifi.distributed.cache.client.DistributedMapCacheClient;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.exception.ProcessException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Entities;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.*;
import java.util.function.Consumer;

import static nl.speyk.nifi.microsoft.graph.processors.utils.CalendarAttributes.*;
import static nl.speyk.nifi.microsoft.graph.processors.utils.CalendarUtils.*;

public class InvokeMicrosoftGraphCalendar extends AbstractMicrosoftGraphCalendar {

    //Patch events for user if event has changed
    private void patchGraphEvents(String userId,
                                  List<Event> eventsSource,
                                  List<Event> eventsGraph,
                                  DistributedMapCacheClient cache,
                                  ProcessSession session,
                                  boolean isUpdate)
            throws IOException, NoSuchAlgorithmException, ParseException {

        patchEvents(userId, eventsSource, eventsGraph, cache, session);
        undoEvents(userId, eventsGraph, cache, session);

        //If we are updating, we stop here
        if (!isUpdate) {
            deleteEvents(userId, eventsSource, eventsGraph, cache, session);
        }
    }

    //Put a hash of the events in a distributed map cache, so we can detect changes
    //We also put the original flow of an appointment in the map cache
    private void putEventsMapCache(List<Event> events, DistributedMapCacheClient cache) throws IOException, NoSuchAlgorithmException, ParseException {
       for (Event evt : events) {
           putEventMapCache(evt, cache);
       }
    }

    private void putBatchGraphEvents(final ProcessContext context,
                                     final ProcessSession session,
                                     List<Event> events, String userId,
                                     boolean isUpdate,
                                     final FlowFile requestFlowFile)
            throws ClientException {
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
                //Put the event in a hashtable for future reference
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
                        Event createdEvent = batchResponseStep.getDeserializedBody(Event.class);
                        getLogger().info("Event for user {} with transactionId {} has been created. Event: {}",
                                createdEvent.organizer.emailAddress.address, createdEvent.transactionId, eventToString(createdEvent, false));
                    } else if (Arrays.stream(errorCodes).anyMatch(e -> e == batchResponseStep.status)) {
                        //In case of the following error codes (429, 503, 504)
                        //put the event from the hashtable in a flow file and route to retry
                        final Event[] evtRetry = {idEvent.get(batchResponseStep.id)};

                        String json = Objects.requireNonNull(msGraphClientAtomicRef.get().getSerializer()).serializeObject(evtRetry);

                        FlowFile retryFLowFile = session.create();
                        attributes.put("upn-name", userId);
                        attributes.put("is-update", Boolean.toString(isUpdate));
                        retryFLowFile = session.putAllAttributes(retryFLowFile, attributes);

                        //Use the RetryFLow processor for setting the max retries
                        final String attrNumRetries = requestFlowFile.getAttribute("flowfile.retries");
                        final String numRetries = (attrNumRetries != null) ? attrNumRetries : "1";

                        retryFLowFile = session.putAttribute(retryFLowFile, "flowfile.retries", numRetries);

                        session.write(retryFLowFile, out -> IOUtils.write(json, out, StandardCharsets.UTF_8));
                        session.transfer(retryFLowFile, REL_RETRY);
                    } else if (batchResponseStep.status == HttpResponseCode.HTTP_CLIENT_ERROR) {
                        //In case of an existing transactionId we want to route to failure and continue
                        routeToError(session, batchResponseStep.body);
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
            //We expect a flow file with a JsonArray as content
            //Every JsonObject in the array is an event raw source type
            //@see <a href="https://docs.microsoft.com/en-us/graph/api/resources/event?view=graph-rest-1.0"</a>
            final InputStream inputStream = session.read(requestFlowFile);
            final String eventsJson;
            final Event[] events;

            eventsJson = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            inputStream.close();

            //Source set is empty, we don't want to
            //set all the events on tentative, so stop with an error
            if ("[{}]".equals(eventsJson.replaceAll(" ", ""))) {
                logger.error("The source dataset is empty.");
                requestFlowFile = session.penalize(requestFlowFile);
                session.transfer(requestFlowFile, REL_FAILURE);
                return;
            }

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

            final List<Event> eventsSource = Arrays.asList(events);
            final List<Event> eventsGraph = getGraphEvents(userId);

            //Only synchronize events that are not already in the graph
            final List<Event> eventsToGraph = eventsDiff(eventsSource, eventsGraph);

            //Put the events in batches in the Microsoft Graph
            putBatchGraphEvents(context, session, eventsToGraph, userId, isUpdate, requestFlowFile);

            //Put the events in a map cache
            putEventsMapCache(eventsSource, cache);

            //Are there any events that have changed?
            //If so patch them in the graph
            //Also undo changed graph events and delete events
            patchGraphEvents(userId, eventsSource, eventsGraph, cache, session, isUpdate);

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
}

