package nl.speyk.nifi.microsoft.graph.processors.utils;

import nl.speyk.nifi.microsoft.graph.services.api.MicrosoftGraphCredentialService;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.distributed.cache.client.Deserializer;
import org.apache.nifi.distributed.cache.client.DistributedMapCacheClient;
import org.apache.nifi.distributed.cache.client.Serializer;
import org.apache.nifi.distributed.cache.client.exception.DeserializationException;
import org.apache.nifi.distributed.cache.client.exception.SerializationException;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.processor.Relationship;
import org.apache.nifi.processor.util.StandardValidators;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public final class CalendarUtils {

    // Properties
    public static final PropertyDescriptor GRAPH_CONTROLLER_ID = new PropertyDescriptor.Builder()
            .name("mg-cs-auth-controller-id")
            .displayName("Graph Controller Service")
            .description("Graph Controller Service used for creating a connection to the Graph")
            .required(true)
            .identifiesControllerService(MicrosoftGraphCredentialService.class)
            .build();

    public static final PropertyDescriptor GRAPH_DISTRIBUTED_MAPCACHE = new PropertyDescriptor.Builder()
            .name("mg-cs-mapcache-id")
            .displayName("Distributed mapcache client")
            .description("Distributed mapcache client used for detecting changes")
            .required(true)
            .identifiesControllerService(DistributedMapCacheClient.class)
            .build();

    public static final PropertyDescriptor GRAPH_RS = new PropertyDescriptor.Builder()
            .name("ms-cs-rs")
            .displayName("Rooster System")
            .description("The name of the rooster system")
            .allowableValues("Magister", "Zermelo")
            .defaultValue("Magister")
            .required(true)
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

    public static final PropertyDescriptor GRAPH_REBUILD_MAP_CACHE = new PropertyDescriptor.Builder()
            .name("mg-cs-is-rebuild")
            .displayName("Rebuild the map cache")
            .description("Set to true for rebuilding the map cache, synchronization will not work")
            .allowableValues("true", "false")
            .defaultValue("false")
            .required(true)
            .build();

    public static final PropertyDescriptor GRAPH_ZERMELO_URL = new PropertyDescriptor.Builder()
            .name("mg-cs-zermelo-url")
            .displayName("Zermelo rest api endpoint")
            .description("Rest api endpoint used for setting the teams link of an appointment")
            .defaultValue("https://{naam scbool}.zportal.nl//api/v3/appointments/")
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
            .required(false)
            .build();

    public static final PropertyDescriptor GRAPH_ZERMELO_TOKEN = new PropertyDescriptor.Builder()
            .name("mg-cs-zermelo-token")
            .displayName("Zermelo oauth token")
            .description("Rest api bearer token")
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
            .required(false)
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

    private CalendarUtils() {
        //Do not instantiate
    }

    public enum Rooster {
        MAGISTER,
        ZERMELO,
        UNKNOWN;

        @Override
        public String toString() {
            switch (this) {
                case MAGISTER: return "magister";
                case ZERMELO: return "zermelo";
                default: return "";
            }
        }
    }

    public static Rooster getRooster(String roosterSystem) {
        return (roosterSystem.equalsIgnoreCase(Rooster.MAGISTER.toString()) ? Rooster.MAGISTER :
                roosterSystem.equalsIgnoreCase(Rooster.ZERMELO.toString()) ? Rooster.ZERMELO :
                        Rooster.UNKNOWN);
    }

    //Serializers needed for the distributed map cache
    public static class CacheValueSerializer implements Serializer<byte[]> {

        @Override
        public void serialize(final byte[] bytes, final OutputStream out) throws SerializationException, IOException {
            out.write(bytes);
        }
    }

    public static class CacheValueDeserializer implements Deserializer<byte[]> {

        @Override
        public byte[] deserialize(final byte[] input) throws DeserializationException {
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
