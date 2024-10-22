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

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.GraphServiceException;
import com.microsoft.graph.models.Event;
import nl.speyk.nifi.microsoft.graph.services.api.MicrosoftGraphCredentialService;
import org.apache.commons.io.IOUtils;
import org.apache.nifi.distributed.cache.client.DistributedMapCacheClient;
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.ProcessContext;
import org.apache.nifi.processor.ProcessSession;
import org.apache.nifi.processor.exception.ProcessException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static nl.speyk.nifi.microsoft.graph.processors.utils.CalendarUtils.*;

public class InvokeMicrosoftGraphCalendar extends AbstractMicrosoftGraphCalendar {

    @Override
    public void onTrigger(final ProcessContext context, final ProcessSession session) throws ProcessException {

        // Here we do the actual work
        FlowFile requestFlowFile = session.get();
        if (requestFlowFile == null) {
            return;
        }
        final ComponentLog logger = getLogger();
        rooster = getRooster(context.getProperty(GRAPH_RS).getValue());
        weeks_in_advance = context.getProperty(GRAPH_WEEKS_IN_ADVANCE).getValue();
        final String groupToFilter = context.getProperty(GRAPH_GROUP_TO_FILTER).getValue();
        AtomicReference<Set<String>> allowedEntities = new AtomicReference<>();
        final boolean rebuildMapCache = context.getProperty(GRAPH_REBUILD_MAP_CACHE).asBoolean();
        final String userId = context.getProperty(GRAPH_USER_ID).evaluateAttributeExpressions(requestFlowFile)
                .getValue();
        final boolean isUpdate = Boolean.parseBoolean(
                context.getProperty(GRAPH_IS_UPDATE).evaluateAttributeExpressions(requestFlowFile).getValue());

        if (msGraphClientAtomicRef.get() == null) {
            logger.info("Microsoft Graph Client is not yet available.");
            MicrosoftGraphCredentialService microsoftGraphCredentialService = context.getProperty(GRAPH_CONTROLLER_ID)
                    .asControllerService(MicrosoftGraphCredentialService.class);
            msGraphClientAtomicRef.set(microsoftGraphCredentialService.getGraphClient());
        }

        // Optional Entara ID group filter for entity synchronization.
        // Omits filtering if no group is specified.
        if ((groupToFilter != null && !groupToFilter.isEmpty())) {
            if (allowedEntities.get() == null || allowedEntities.get().isEmpty()) {
                allowedEntities.set(getAllowedEntities(groupToFilter));
            }
            if (!allowedEntities.get().contains(userId)) {
                session.transfer(requestFlowFile, REL_FILTERED);
                return;
            }
        }

        // the cache client used to interact with the distributed cache
        final DistributedMapCacheClient cache = context.getProperty(GRAPH_DISTRIBUTED_MAPCACHE)
                .asControllerService(DistributedMapCacheClient.class);

        try {
            // We expect a flow file with a JsonArray as content
            // Every JsonObject in the array is an event raw source type
            // @see <a
            // href="https://docs.microsoft.com/en-us/graph/api/resources/event?view=graph-rest-1.0"</a>
            final InputStream inputStream = session.read(requestFlowFile);
            final String eventsJson;
            final Event[] events;

            eventsJson = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
            inputStream.close();

            // Source set is empty, we don't want to
            // set all the events on tentative, so stop with an error
            if ("[{}]".equals(eventsJson.replaceAll(" ", ""))) {
                logger.error("The source dataset is empty.");
                requestFlowFile = session.penalize(requestFlowFile);
                session.transfer(requestFlowFile, REL_FAILURE);
                return;
            }

            JsonElement jsonElement = JsonParser.parseString(eventsJson);

            // Make sure we always have an array of events.
            if (jsonElement.isJsonArray()) {
                events = Objects.requireNonNull(msGraphClientAtomicRef.get().getSerializer())
                        .deserializeObject(eventsJson, Event[].class);
            } else if (jsonElement.isJsonObject()) {
                events = new Event[] { Objects.requireNonNull(msGraphClientAtomicRef.get().getSerializer())
                        .deserializeObject(eventsJson, Event.class) };
            } else {
                logger.error("Not valid JSON or empty.");
                requestFlowFile = session.penalize(requestFlowFile);
                session.transfer(requestFlowFile, REL_FAILURE);
                return;
            }

            final List<Event> eventsSource = Arrays.asList(events);
            // If chosen, rebuild the map cache and exit
            if (rebuildMapCache) {
                logger.info("Rebuilding mapcache and stop.");
                for (Event event : eventsSource) {
                    putEventMapCache(event, cache);
                }
                session.transfer(requestFlowFile, REL_ORIGINAL);
                return;
            }
            final List<Event> eventsGraph = getGraphEvents(userId);

            // Only synchronize events that are not already in the graph
            final List<Event> eventsToGraph = eventsDiff(eventsSource, eventsGraph);

            // Put the events in batches in the Microsoft Graph
            putBatchGraphEvents(context, session, eventsToGraph, userId, isUpdate, requestFlowFile, cache);

            // Are there any events that have changed?
            // If so patch them in the graph
            // Also undo changed graph events and delete events
            undoEvents(userId, eventsGraph, cache, session);
            patchEvents(context, userId, eventsSource, eventsGraph, cache, session);

            // If we are updating, we stop here
            if (!isUpdate) {
                // Do we want to delete an event, or put it on tentative?
                if (context.getProperty(GRAPH_IS_DELETE).asBoolean()) {
                    deleteEventsForReal(userId, eventsSource, eventsGraph, cache, session);
                } else {
                    deleteEvents(userId, eventsSource, eventsGraph, cache, session);
                }
            }

            // The original flow file hasn't changed
            session.transfer(requestFlowFile, REL_ORIGINAL);

        } catch (GraphServiceException | IOException | NoSuchAlgorithmException | IllegalArgumentException e) {
            // Error in performing request to Microsoft Graph
            // We can't recover from this so route to failure handler
            routeToFailure(requestFlowFile, logger, session, context, e);
        }
        // sometimes JSON batching causes socket timeouts; catch them and retry
        catch (ClientException exceptionToRetry) {
            logger.error(
                    "Failed to create an event in Microsoft Graph due network problems. {}. More detailed information may be available in "
                            + "the NiFi logs.",
                    new Object[] { exceptionToRetry.getLocalizedMessage() }, exceptionToRetry);
            // transfer original to retry
            session.transfer(requestFlowFile, REL_RETRY);
            session.penalize(requestFlowFile);
            context.yield();
        }
    }
}
