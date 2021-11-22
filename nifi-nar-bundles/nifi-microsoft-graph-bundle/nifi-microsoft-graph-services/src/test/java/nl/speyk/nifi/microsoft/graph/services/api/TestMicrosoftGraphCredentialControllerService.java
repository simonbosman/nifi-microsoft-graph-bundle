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
package nl.speyk.nifi.microsoft.graph.services.api;

import nl.speyk.nifi.microsoft.graph.services.MicrosoftGraphCredentialControllerService;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.NoOpProcessor;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Test;

public class TestMicrosoftGraphCredentialControllerService {

    public static final String CREDENTIALS_SERVICE_IDENTIFIER = "credentials-service";

    private static final String AUTH_TENANT_ID = "4d9d7628-507b-45a9-b659-03d1f09d16bf";
    private static final String AUTH_CLIENT_ID = "4dcf450b-1c52-4ab4-8c21-2650e1563435";
    private static final String AUTH_CLIENT_SECRET = "OfV7Q~DVbygzsmG8aP.fyjSc0L0pLhlecCIzR";
    private static final String AUTH_GRANT_TYPE = "client_credentials";
    private static final String AUTH_SCOPE = "https://graph.microsoft.com/.default";

    private TestRunner runner;
    private MicrosoftGraphCredentialControllerService credentialService;

    @Before
    public void Setup() throws InitializationException {
        runner = TestRunners.newTestRunner(NoOpProcessor.class);
        credentialService = new MicrosoftGraphCredentialControllerService();
        runner.addControllerService(CREDENTIALS_SERVICE_IDENTIFIER, credentialService);
    }

    @Test
    public void testNotValidBecauseTenantIdIsMissing() {
        configureTenantId();
        runner.assertNotValid(credentialService);
    }

    @Test
    public void testNotValidBecauseClientIdIsMissing() {
        configureClientId();
        runner.assertNotValid(credentialService);
    }

    @Test
    public void testNotValidBecauseClientSecretIsMissing() {
        configureClientSecret();
        runner.assertNotValid(credentialService);
    }

    @Test
    public void testNotValidBecauseGrantTypeIsMissing() {
        configureGrantType();
        runner.assertNotValid(credentialService);
    }

    @Test
    public void testNotValidBecauseScopeTypeIsMissing() {
        configureScope();
        runner.assertNotValid(credentialService);
    }

    private void configureTenantId() {
        runner.setProperty(credentialService, MicrosoftGraphCredentialService.AUTH_TENANT_ID, AUTH_TENANT_ID);
    }

    private void configureClientId() {
        runner.setProperty(credentialService, MicrosoftGraphCredentialService.AUTH_CLIENT_ID, AUTH_CLIENT_ID);
    }

    private void configureClientSecret() {
        runner.setProperty(credentialService, MicrosoftGraphCredentialService.AUTH_CLIENT_SECRET, AUTH_CLIENT_SECRET);
    }

    private void configureGrantType() {
        runner.setProperty(credentialService, MicrosoftGraphCredentialService.AUTH_GRANT_TYPE, AUTH_GRANT_TYPE);
    }

    private void configureScope() {
        runner.setProperty(credentialService, MicrosoftGraphCredentialService.AUTH_SCOPE, AUTH_SCOPE);
    }
}
