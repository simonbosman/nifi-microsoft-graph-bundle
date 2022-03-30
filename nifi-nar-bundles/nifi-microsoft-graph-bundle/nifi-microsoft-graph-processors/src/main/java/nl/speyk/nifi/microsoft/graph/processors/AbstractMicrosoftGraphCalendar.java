package nl.speyk.nifi.microsoft.graph.processors;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.microsoft.graph.content.BatchRequestContent;
import com.microsoft.graph.content.BatchResponseContent;
import com.microsoft.graph.content.BatchResponseStep;
import com.microsoft.graph.core.ClientException;
import com.microsoft.graph.http.HttpMethod;
import com.microsoft.graph.http.HttpResponseCode;
import com.microsoft.graph.models.BodyType;
import com.microsoft.graph.models.Event;
import com.microsoft.graph.models.FreeBusyStatus;
import com.microsoft.graph.requests.EventCollectionPage;
import com.microsoft.graph.requests.EventCollectionRequestBuilder;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.serializer.ISerializer;
import nl.speyk.nifi.microsoft.graph.processors.utils.CalendarUtils;
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
import org.apache.nifi.flowfile.FlowFile;
import org.apache.nifi.logging.ComponentLog;
import org.apache.nifi.processor.*;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Entities;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static nl.speyk.nifi.microsoft.graph.processors.utils.CalendarAttributes.*;
import static nl.speyk.nifi.microsoft.graph.processors.utils.CalendarUtils.*;

@Tags({"Speyk", "Microsoft", "Graph", "Calendar", "Client", "Processor"})
@CapabilityDescription("Automate appointment organization and calendaring with  Microsoft Graph REST API.")
@SeeAlso({})
@ReadsAttributes({})
@WritesAttributes({
        @WritesAttribute(attribute = "invokeMSGraph.java.exception.class", description = "The Java exception class raised when the processor fails"),
        @WritesAttribute(attribute = "invokeMSGraph.java.exception.message", description = "The Java exception message raised when the processor fails")})
public abstract class AbstractMicrosoftGraphCalendar extends AbstractProcessor {

    protected final AtomicReference<GraphServiceClient<Request>> msGraphClientAtomicRef = new AtomicReference<>();
    protected final Serializer<String> keySerializer = new CalendarUtils.StringSerializer();
    protected final Serializer<byte[]> valueSerializer = new CalendarUtils.CacheValueSerializer();
    protected Rooster rooster = Rooster.UNKNOWN;
    private final Deserializer<byte[]> valueDeserializer = new CalendarUtils.CacheValueDeserializer();
    private List<PropertyDescriptor> descriptors;
    private Set<Relationship> relationships;

    private void routeToSuccess(final ProcessSession session, Object content) {
        // Attributes for success and retry flow files
        final Map<String, String> attributes = new Hashtable<>();
        attributes.put("Content-Type", "application/json; charset=utf-8");
        attributes.put("mime.type", "application/json");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(content);

        //If event is created, put the response in the success flow file
        FlowFile succesFlowFile = session.create();
        succesFlowFile = session.putAllAttributes(succesFlowFile, attributes);
        session.write(succesFlowFile, out -> IOUtils.write(json, out, StandardCharsets.UTF_8));
        session.transfer(succesFlowFile, REL_SUCCESS);
    }

    private void routeToError(final ProcessSession session, Object content) {
        // Attributes for success and retry flow files
        final Map<String, String> attributes = new Hashtable<>();
        attributes.put("Content-Type", "application/json; charset=utf-8");
        attributes.put("mime.type", "application/json");

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(content);

        //If an error occurred put the error in flow file and route to failure
        FlowFile errorFlowFile = session.create();
        errorFlowFile = session.putAllAttributes(errorFlowFile, attributes);
        session.write(errorFlowFile, out -> IOUtils.write(json, out, StandardCharsets.UTF_8));
        session.transfer(errorFlowFile, REL_FAILURE);
    }

    private String eventToString(Event evt, @NotNull Boolean isGraphEvent) {
        StringBuilder sb = new StringBuilder();
        //A graph event has a different datetime format, hence we convert it.
        if (isGraphEvent) {
            DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").withZone(ZoneId.of("Europe/Berlin"));
            ZonedDateTime dtStart = LocalDateTime.parse(evt.start.dateTime).atZone(ZoneId.of("UTC"));
            sb.append(dtStart.format(dtFormatter));
            ZonedDateTime dtEnd = LocalDateTime.parse(evt.end.dateTime).atZone(ZoneId.of("UTC"));
            sb.append(dtEnd.format(dtFormatter));
        }
        else {
            if (evt.start != null) sb.append(evt.start.dateTime);
            if (evt.end != null) sb.append(evt.end.dateTime);
        }
        sb.append(evt.subject);
        if (evt.showAs != null) sb.append(evt.showAs.name());
        if (evt.location != null && evt.location.displayName != null && !evt.location.displayName.isEmpty()) sb.append(evt.location.displayName);
        else if (evt.locations != null && evt.locations.size() > 0) {
            String joinedLocations = evt.locations.stream().map((loc) -> loc.displayName).sorted().collect(Collectors.joining("; "));
            sb.append(joinedLocations);
        }
        return sb.toString();
    }

    private byte[] createHashedEvent(Event evt, @NotNull Boolean isGraphEvent) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        return messageDigest.digest(eventToString(evt, isGraphEvent).getBytes(StandardCharsets.UTF_8));
    }

    private byte[] createHashedEvent(Event evt) throws NoSuchAlgorithmException {
        return createHashedEvent(evt, false);
    }

    protected void routeToFailure(FlowFile requestFlowFile, final ComponentLog logger,
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

    protected List<Event> getGraphEvents(String userId) {
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
    protected List<Event> eventsDiff(List<Event> eventsSource, List<Event> eventsDest) {
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

    protected void putEventMapCache(Event evt, DistributedMapCacheClient cache) throws IOException, NoSuchAlgorithmException{
        final Consumer<String> logError = (transactionId) -> {
            getLogger().error("Could not put hashed/full event: %s in the distributed map cache.", transactionId);
        };

        byte[] hashedEvt = createHashedEvent(evt);
        cache.put(evt.transactionId, hashedEvt, keySerializer, valueSerializer);

        final ISerializer serializer = Objects.requireNonNull(msGraphClientAtomicRef.get()).getSerializer();
        if (serializer == null) {
            logError.accept(evt.transactionId);
            return;
        }

        final String cacheValue = serializer.serializeObject(evt);
        if (cacheValue == null) {
            logError.accept(evt.transactionId);
            return;
        }

        cache.put(PARTITION_KEY + evt.transactionId, cacheValue.getBytes(StandardCharsets.UTF_8), keySerializer, valueSerializer);
    }

    protected void putBatchGraphEvents(final ProcessSession session, List<Event> events, String userId, boolean isUpdate, final FlowFile requestFlowFile, final DistributedMapCacheClient cache) throws ClientException, IOException, NoSuchAlgorithmException {
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
                //Adjust timezones for Zermelo
                if (rooster == Rooster.ZERMELO) {
                    DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").withZone(ZoneId.of("Europe/Berlin"));
                    ZonedDateTime dtStart = LocalDateTime.parse(event.start.dateTime, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")).atZone(ZoneId.of("UTC"));
                    event.start.dateTime = dtStart.format(dtFormatter);
                    ZonedDateTime dtEnd = LocalDateTime.parse(event.end.dateTime, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")).atZone(ZoneId.of("UTC"));
                    event.end.dateTime = dtEnd.format(dtFormatter);
                }
                //Sort the locations
                if (event.locations != null) {
                    event.locations.sort(Comparator.comparing((loc) -> loc.displayName));
                }
                //Sanitize body content
                if (event.body != null && event.body.content != null && event.body.content.length() > 0) {
                    //Mark the event if there has been a change
                    if (rooster == Rooster.ZERMELO) {
                        event.subject += " [!]";
                    }
                    event.body.contentType = BodyType.HTML;
                    event.body.content = Entities.unescape(Jsoup.parse(event.body.content).html());
                }
                //Put the event in a hashtable for future reference
                idEvent.put(batchRequestContent.addBatchRequestStep(Objects.requireNonNull(msGraphClientAtomicRef.get().users(userId).events().buildRequest()), HttpMethod.POST, event), event);
            }

            final BatchResponseContent batchResponseContent = msGraphClientAtomicRef.get().batch().buildRequest().post(batchRequestContent);

            if (batchResponseContent != null && batchResponseContent.responses != null) {
                for (BatchResponseStep<JsonElement> batchResponseStep : batchResponseContent.responses) {
                    if (batchResponseStep.status == HttpResponseCode.HTTP_CREATED) {
                        //If event is created, put the response in the success flow file
                        routeToSuccess(session, batchResponseStep.body);
                        Event createdEvent = batchResponseStep.getDeserializedBody(Event.class);
                        getLogger().info("Event for user {} with transactionId {} has been created. Event: {}", createdEvent.organizer.emailAddress.address, createdEvent.transactionId, eventToString(createdEvent, false));
                        //Put the event in the distributed map cache
                        putEventMapCache(idEvent.get(batchResponseStep.id), cache);

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


    protected void patchEvents(String userId, List<Event> eventsSource, List<Event> eventsGraph,
                               DistributedMapCacheClient cache, ProcessSession session)
            throws NoSuchAlgorithmException, IOException {

        //Are there any changes in the source event?
        //Patch the graph with the changed event
        //Update the cache with the changed event
        for (Event evt : eventsSource) {
            try {
                //Adjust timezones for Zermelo
                if (rooster == Rooster.ZERMELO) {
                    DateTimeFormatter dtFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").withZone(ZoneId.of("Europe/Berlin"));
                    ZonedDateTime dtStart = LocalDateTime.parse(evt.start.dateTime, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")).atZone(ZoneId.of("UTC"));
                    evt.start.dateTime = dtStart.format(dtFormatter);
                    ZonedDateTime dtEnd = LocalDateTime.parse(evt.end.dateTime, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")).atZone(ZoneId.of("UTC"));
                    evt.end.dateTime = dtEnd.format(dtFormatter);
                }
                //Sort the locations
                if (evt.locations != null) {
                    evt.locations.sort(Comparator.comparing((loc) -> loc.displayName));
                }

                //Mark the event if there has been a notification
                if (rooster == Rooster.ZERMELO && evt.body != null && evt.body.content != null && !evt.body.content.isEmpty()) {
                    evt.subject += " [!]";
                }

                //Compare hashes
                byte[] hashedEvt = createHashedEvent(evt);

                if (evt.transactionId == null) {
                    throw new IllegalArgumentException("TransactionId can't be empty");
                }
                byte[] hashedCachedEvt = cache.get(evt.transactionId, keySerializer, valueDeserializer);

                if (hashedCachedEvt != null & !Arrays.equals(hashedEvt, hashedCachedEvt)) {
                    //Sanitize body content
                    if (evt.body != null && evt.body.content != null) {
                        evt.body.contentType = BodyType.HTML;
                        evt.body.content = Entities.unescape(Jsoup.parse(evt.body.content).html());
                    }
                    final Event eventToPatch = eventsGraph.stream()
                            .filter(event -> event.transactionId != null)
                            .filter(event -> event.transactionId.equals(evt.transactionId))
                            .findAny().get();
                    //TODO: When a team link has been created, preserve this link
                    routeToSuccess(session, msGraphClientAtomicRef.get()
                            .users(userId)
                            .events(Objects.requireNonNull(eventToPatch.id))
                            .buildRequest()
                            .patch(evt));
                    getLogger().info("Event for user {} with transactionId {} has been updated. " +
                                    "Event Source: {}; Event Graph: {}",
                            userId,
                            evt.transactionId,
                            eventToString(evt, false),
                            eventToString(eventToPatch, true));
                    putEventMapCache(evt, cache);
                }
            } catch (NoSuchElementException e) {
                getLogger().error(String.format("Source event with transactionId %s couldn't be patched. " +
                        "Most likely the event has just been created", evt.transactionId));
            }
        }
    }

    protected void undoEvents(String userId, List<Event> undoEvents, DistributedMapCacheClient cache, ProcessSession session)
            throws NoSuchAlgorithmException, IOException {

        //Are there any changes in the graph event?
        //Patch the graph event with the original event
        //from the source events in the map cache
        //Effectually restoring manual changes to the graph
        for (Event evt : undoEvents) {
            //Event not managed by DIS, so continue
            if (evt.transactionId == null) continue;

            //We don't care about tentative events
            assert evt.showAs != null;
            if (evt.showAs.name().equals("TENTATIVE")) continue;;

            try {
                byte[] hashedCashedEvt = cache.get(evt.transactionId, keySerializer, valueDeserializer);
                //Graph event isn't an event managed by DIS, so skip
                if (hashedCashedEvt == null)
                    continue;
                byte[] hashedEvt = createHashedEvent(evt, true);
                //Graph event has changed, so fix this
                if (!Arrays.equals(hashedEvt, hashedCashedEvt)) {
                    //Is the event in the distributed map cache?
                    byte[] cacheValue = cache.get(PARTITION_KEY + evt.transactionId, keySerializer, valueDeserializer);
                    if (cacheValue == null) {
                        throw new NoSuchElementException();
                    }

                    final Event eventPatchVal =
                            Objects.requireNonNull(msGraphClientAtomicRef
                                            .get()
                                            .getSerializer())
                                    .deserializeObject(new String(cacheValue, StandardCharsets.UTF_8), Event.class);
                    if (eventPatchVal == null) {
                        throw new NoSuchElementException();
                    }

                    //We don't want to override the body context
                    eventPatchVal.body = evt.body;

                    Object content = msGraphClientAtomicRef.get()
                            .users(userId)
                            .events(Objects.requireNonNull(evt.id))
                            .buildRequest()
                            .patch(eventPatchVal);
                    routeToSuccess(session, content);
                    getLogger().info("Event for user {} with transactionId {} has been undone. " +
                                    "Event Source: {}; Event Graph: {}",
                            userId,
                            evt.transactionId,
                            eventToString(eventPatchVal, false),
                            eventToString(evt, true));
                    //update the distributed map cache
                    putEventMapCache(eventPatchVal, cache);
                }
            } catch (NoSuchElementException e) {
                String unknown = "unknown";
                getLogger().info(String.format("Graph event with transactionId %s with status %s and subject %s couldn't be patched. " +
                        "Most likely the event is deleted from the source dataset.", evt.transactionId, evt.showAs != null ? evt.showAs.name() : unknown, evt.subject));
            }
        }

    }

    protected void deleteEvents(String userId, List<Event> eventsSource, List<Event> eventsGraph, DistributedMapCacheClient cache, ProcessSession session)
            throws NoSuchAlgorithmException, IOException {
        //Is the set of events greater in the graph than in source?
        //Make event in the graph tentative
        List<Event> eventsToDelete = eventsDiff(eventsGraph, eventsSource);
        for (Event evt : eventsToDelete) {
            //Is the event managed by DIS? If not skip.
            if (cache.get(evt.transactionId, keySerializer, valueDeserializer) == null)
                continue;
            //Is the event already tentative?
            assert evt.showAs != null;
            if (Objects.equals(evt.showAs.name(), "TENTATIVE"))
                continue;

            //Create the event for patching
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

                getLogger().info("Event for user {} with transactionId {} has been deleted. Event Source: {}; Event Graph: {}",
                        userId, evt.transactionId, eventToString(evt, false), eventToString(eventPatchVal, false));

                //update the distributed map cache
                putEventMapCache(eventPatchVal, cache);

            } catch (NoSuchElementException e) {
                getLogger().error(String.format("Graph event with transactionId %s couldn't be patched to state tentative.", evt.transactionId));
            }
        }
    }

    @OnScheduled
    public void onScheduled(final ProcessContext context) {
        if (msGraphClientAtomicRef.get() != null) {
            return;
        }
        // Get the controller-service responsible for an authenticated GraphClient
        // We will reuse the build GraphClient, hence we put it in an atomic reference
        MicrosoftGraphCredentialService microsoftGraphCredentialService = context.getProperty(GRAPH_CONTROLLER_ID)
                .asControllerService(MicrosoftGraphCredentialService.class);
        msGraphClientAtomicRef.set(microsoftGraphCredentialService.getGraphClient());
    }

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final Set<Relationship> _relationships = new HashSet<>();
        _relationships.add(REL_SUCCESS);
        _relationships.add(REL_FAILURE);
        _relationships.add(REL_RETRY);
        _relationships.add(REL_ORIGINAL);
        this.relationships = Collections.unmodifiableSet(_relationships);
        this.descriptors = Collections.unmodifiableList(Arrays.asList(
                GRAPH_CONTROLLER_ID,
                GRAPH_DISTRIBUTED_MAPCACHE,
                GRAPH_RS,
                GRAPH_USER_ID,
                GRAPH_IS_UPDATE,
                GRAPH_REBUILD_MAP_CACHE));
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return this.descriptors;
    }
}
