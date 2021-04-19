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
package nl.speyk.nifi.microsoft.graph.auth;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.authentication.IAuthenticationProvider;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.requests.GraphServiceClient;
import com.microsoft.graph.core.ClientException;
import okhttp3.Request;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.annotation.lifecycle.OnDisabled;
import org.apache.nifi.annotation.lifecycle.OnEnabled;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ConfigurationContext;
import org.apache.nifi.reporting.InitializationException;
import com.azure.identity.ClientSecretCredential;
import org.jetbrains.annotations.NotNull;

@Tags({ "Speyk", "Microsoft", "Graph", "Authentication", "Service", "Controller"})
@CapabilityDescription("Speyk Microsoft Graph ControllerService implementation of GraphAuthService.")
public class GraphAuthControllerClientService extends AbstractControllerService implements GraphAuthClientService {

    private static final List<PropertyDescriptor> properties;

    private GraphServiceClient<Request> graphClient;

    static {
        final List<PropertyDescriptor> props = new ArrayList<>();
        props.add(AUTH_GRANT_TYPE);
        props.add(AUTH_CLIENT_ID);
        props.add(AUTH_CLIENT_SECRET);
        props.add(AUTH_TENANT_ID);
        props.add(AUTH_SCOPE);
        properties = Collections.unmodifiableList(props);
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return properties;
    }

    @OnEnabled
    public void onEnabled(@NotNull final ConfigurationContext context) throws InitializationException {

        try {
            setupGraphClient(context);
        }
        catch (Exception ex) {
            getLogger().error("Could not initialize Microsoft Graph Client.", ex);
            throw new InitializationException(ex);
        }
    }

    @OnDisabled
    public void shutdown() {

    }

    private void setupGraphClient(ConfigurationContext context) throws InitializationException {
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
        try {
            this.graphClient = GraphServiceClient
                .builder()
                .authenticationProvider(authProvider)
                .buildClient();
        }
        catch (ClientException e) {
            getLogger().error("Error building Microsoft Graph Client from the supplied configuration.", e);
            throw new InitializationException(e);
        }
    }

    @Override
    public GraphServiceClient<Request> getGraphClient() {
        return graphClient;
    }
}
