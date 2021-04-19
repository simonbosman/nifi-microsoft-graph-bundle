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

import com.microsoft.graph.requests.GraphServiceClient;
import okhttp3.Request;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.ControllerService;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.processor.util.StandardValidators;

public interface GraphAuthClientService extends ControllerService {

    public final static PropertyDescriptor AUTH_CLIENT_ID = new PropertyDescriptor
            .Builder()
            .name("mg-cs-auth-client-id")
            .displayName("Auth Client Id")
            .description("Specifies the Oauth2 client id")
            .required(true)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
            .sensitive(false)
            .build();

    public final static PropertyDescriptor AUTH_TENANT_ID = new PropertyDescriptor
            .Builder()
            .name("mg-cs-auth-tenant-id")
            .displayName("Auth Tenant Id")
            .description( "Specifies the Oauth2 client API Key")
            .required(true)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .addValidator(  StandardValidators.NON_BLANK_VALIDATOR)
            .sensitive(false)
            .build();

    public final static PropertyDescriptor AUTH_CLIENT_SECRET = new PropertyDescriptor
            .Builder()
            .name("mg-cs-auth-client-secret")
            .displayName("Auth Client Secret")
            .description("Specifies the Oauth2 client Secret")
            .required(true)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .sensitive(true)
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
            .build();

    public final static PropertyDescriptor AUTH_GRANT_TYPE = new PropertyDescriptor
            .Builder()
            .name("mg-cs-auth-grant-type")
            .displayName("Auth Grant Type")
            .description("Specifies the Oauth2 Grant Type")
            .required(true)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .defaultValue("client_credentials")
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
            .build();

    public final static PropertyDescriptor AUTH_SCOPE = new PropertyDescriptor
            .Builder()
            .name("mg-cs-auth-scope")
            .displayName("Auth Scope")
            .description("Specifies the Oauth2 client Scope")
            .required(true)
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .defaultValue("https://graph.microsoft.com/.default")
            .addValidator(StandardValidators.NON_BLANK_VALIDATOR)
            .build();

    /**
     * Builds a Microsoft Graph Client
     *
     * @return GraphClient object
     */
    GraphServiceClient<Request> getGraphClient();

}
