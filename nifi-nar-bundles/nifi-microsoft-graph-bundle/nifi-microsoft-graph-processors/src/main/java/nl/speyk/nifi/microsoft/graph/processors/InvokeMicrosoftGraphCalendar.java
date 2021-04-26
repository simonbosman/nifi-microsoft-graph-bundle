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

import com.microsoft.graph.serializer.*;
import com.microsoft.graph.models.User;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.requests.UserCollectionPage;
import com.microsoft.graph.requests.UserCollectionRequest;
import nl.speyk.nifi.microsoft.graph.services.api.MicrosoftGraphCredentialService;
import okhttp3.Request;
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

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Tags({ "Speyk", "Microsoft", "Graph", "Calendar", "Client", "Processor"})
@CapabilityDescription("Automate appointment organization and calendaring with  Microsoft Graph REST API.")
@SeeAlso({})
@ReadsAttributes({@ReadsAttribute(attribute="", description="")})
@WritesAttributes({@WritesAttribute(attribute="", description="")})
public class InvokeMicrosoftGraphCalendar extends AbstractProcessor {

    public static final String GRAPH_METHOD_GET = "GET";
    public static final String GRAPH_METHOD_POST= "POST";
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


    public static final Relationship MY_RELATIONSHIP = new Relationship.Builder()
            .name("MY_RELATIONSHIP")
            .description("Example relationship")
            .build();

    private List<PropertyDescriptor> descriptors;
    private Set<Relationship> relationships;
    private final AtomicReference<GraphServiceClient<Request>> msGraphClientAtomicRef = new AtomicReference<>();

    @Override
    protected void init(final ProcessorInitializationContext context) {

        final List<PropertyDescriptor> descriptors = new ArrayList<>();
        descriptors.add(GRAPH_CONTROLLER_ID);
        descriptors.add(GRAPH_PROP_METHOD);

        final Set<Relationship> relationships = new HashSet<>();
        relationships.add(MY_RELATIONSHIP);
        this.relationships = Collections.unmodifiableSet(relationships);
        this.descriptors = Collections.unmodifiableList(descriptors);
    }

    @Override
    public Set<Relationship> getRelationships() {
        return this.relationships;
    }

    @Override
    public final List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return descriptors;
    }

    //TODO: put in abstract base class
    @OnScheduled
    public void onScheduled(final ProcessContext context) {

        final ComponentLog log = getLogger();

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

        FlowFile flowFile = session.get();
        if ( flowFile == null ) {
            return;
        }

       try {
           UserCollectionPage users =  msGraphClientAtomicRef.get()
                   .users()
                   .buildRequest()
                   .get();

           List<User> usersList = users.getCurrentPage();

           //TODO: Do a case switch on given param GET, POST, PATCH Look for inspiration at InvokeHTTP
           UserCollectionRequest userCollectionRequest = msGraphClientAtomicRef.get().users().buildRequest();

           UserCollectionPage userCollectionPage = (UserCollectionPage) UserCollectionRequest.class
                   .getMethod("GET".toLowerCase())
                   .invoke(userCollectionRequest);

           ISerializer serializer = Objects.requireNonNull(msGraphClientAtomicRef.get()
                   .getHttpProvider(), "Microsoft Graph Client has no HTTP provider.")
                   .getSerializer();

           User usr = msGraphClientAtomicRef.get()
                   .users("simon@speykintegrations.onmicrosoft.com")
                   .buildRequest()
                   .get();

           String json = serializer.serializeObject(usersList.get(0)).toString();
           String json2 = serializer.serializeObject(users);

           String s = "wait";
       } catch (Exception ex) {
           logger.error("Getting users from Microsoft Graph failed.");
           throw new ProcessException(ex);
       }
    }
}

