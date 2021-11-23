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

import nl.speyk.nifi.microsoft.graph.services.MicrosoftGraphCredentialControllerService;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.distributed.cache.client.CommsSession;
import org.apache.nifi.distributed.cache.client.DistributedMapCacheClientService;
import org.apache.nifi.distributed.cache.client.DistributedSetCacheClientService;
import org.apache.nifi.distributed.cache.client.StandardCommsSession;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.MockConfigurationContext;
import org.apache.nifi.util.MockControllerServiceInitializationContext;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static nl.speyk.nifi.microsoft.graph.services.MicrosoftGraphCredentialControllerService.AUTH_TENANT_ID;
import static nl.speyk.nifi.microsoft.graph.services.MicrosoftGraphCredentialControllerService.AUTH_CLIENT_ID;
import static nl.speyk.nifi.microsoft.graph.services.MicrosoftGraphCredentialControllerService.AUTH_CLIENT_SECRET;
import static nl.speyk.nifi.microsoft.graph.services.MicrosoftGraphCredentialControllerService.AUTH_SCOPE;
import static nl.speyk.nifi.microsoft.graph.services.MicrosoftGraphCredentialControllerService.AUTH_GRANT_TYPE;
import static org.apache.nifi.distributed.cache.client.DistributedMapCacheClientService.SSL_CONTEXT_SERVICE;
import static org.apache.nifi.distributed.cache.client.DistributedMapCacheClientService.HOSTNAME;
import static org.apache.nifi.distributed.cache.client.DistributedMapCacheClientService.COMMUNICATIONS_TIMEOUT;
import static org.apache.nifi.distributed.cache.client.DistributedMapCacheClientService.PORT;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class TestAbstractMicrosoftGraphCalendar {

    private TestRunner runner;

    @Before
    public void setUp() throws InitializationException {
        runner = TestRunners.newTestRunner(InvokeMicrosoftGraphCalendar.class);

        MicrosoftGraphCredentialControllerService credentialService = mock(MicrosoftGraphCredentialControllerService.class);
        when(credentialService.getIdentifier()).thenReturn("credentials_service");
        runner.addControllerService("credentials_service", credentialService);
        runner.setProperty(AUTH_TENANT_ID, "");
        runner.setProperty(AUTH_CLIENT_ID, "");
        runner.setProperty(AUTH_CLIENT_SECRET, "");
        runner.setProperty(AUTH_SCOPE, "");
        runner.setProperty(AUTH_GRANT_TYPE, "");
        runner.enableControllerService(credentialService);

       DistributedMapCacheClientService mapCacheClientService = mock(DistributedMapCacheClientService.class);
       when(mapCacheClientService.getIdentifier()).thenReturn("map_cache_client_service");
       runner.addControllerService("map_cache_client_service", mapCacheClientService);
       // runner.setProperty(null, SSL_CONTEXT_SERVICE, "");
       runner.setProperty(HOSTNAME, "localhost");
       runner.setProperty(COMMUNICATIONS_TIMEOUT, "30");
       runner.setProperty(PORT, "4557");
       runner.enableControllerService(mapCacheClientService);
    }

    @Test
    public void testValid() {
        runner.assertValid();
    }
}
