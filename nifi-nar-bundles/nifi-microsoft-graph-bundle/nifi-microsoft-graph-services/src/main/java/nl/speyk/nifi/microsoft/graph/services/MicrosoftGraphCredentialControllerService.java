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
package nl.speyk.nifi.microsoft.graph.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.httpcore.HttpClients;
import com.microsoft.graph.requests.GraphServiceClient;
import nl.speyk.nifi.microsoft.graph.services.api.MicrosoftGraphCredentialService;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import com.azure.identity.ClientSecretCredential;

@Tags({"Speyk", "Microsoft", "Graph", "Authentication", "Service", "Controller", "Cloud", "Credentials"})
@CapabilityDescription("Defines credentials for Microsoft Graph Processors.")
public class MicrosoftGraphCredentialControllerService extends AbstractControllerService implements MicrosoftGraphCredentialService {

    private static final List<PropertyDescriptor> properties;

    static {
        final List<PropertyDescriptor> props = new ArrayList<>();
        props.add(AUTH_GRANT_TYPE);
        props.add(AUTH_CLIENT_ID);
        props.add(AUTH_CLIENT_SECRET);
        props.add(AUTH_TENANT_ID);
        props.add(AUTH_SCOPE);
        properties = Collections.unmodifiableList(props);
    }

    private ConfigurationContext context;

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return properties;
    }

    @OnEnabled
    public void onEnabled(ConfigurationContext context) {
        this.context = context;
    }

    private GraphServiceClient setupGraphClient(ConfigurationContext context) {

        final String clientId = context.getProperty(AUTH_CLIENT_ID).getValue();
        final String tenantId = context.getProperty(AUTH_TENANT_ID).getValue();
        final String clientSecret = context.getProperty(AUTH_CLIENT_SECRET).getValue();
        final List<String> scopes = Arrays.asList(context.getProperty(AUTH_SCOPE).getValue());

        final ClientSecretCredential defaultCredential = new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tenantId(tenantId)
                .build();

        final IAuthenticationProvider authProvider = new TokenCredentialAuthProvider(scopes, defaultCredential);

        //We have a lot of connections to the graph api, hence we make a large connectionpool
        final OkHttpClient httpClient = HttpClients.createDefault(authProvider)
                .newBuilder()
                .followSslRedirects(false)
                .connectionPool( new ConnectionPool(256, 5, TimeUnit.MINUTES))
                .connectTimeout(30L, TimeUnit.SECONDS)
                .build();

        final GraphServiceClient graphClient = GraphServiceClient
                .builder()
                .httpClient(httpClient)
                .buildClient();

        return graphClient;
    }

    @Override
    public GraphServiceClient<Request> getGraphClient() {
        return setupGraphClient(this.context);
    }
}
