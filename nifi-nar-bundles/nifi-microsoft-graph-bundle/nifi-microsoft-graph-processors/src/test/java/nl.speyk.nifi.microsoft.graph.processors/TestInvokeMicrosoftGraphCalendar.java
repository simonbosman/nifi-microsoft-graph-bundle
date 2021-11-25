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
import org.apache.nifi.distributed.cache.client.DistributedMapCacheClientService;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static nl.speyk.nifi.microsoft.graph.services.MicrosoftGraphCredentialControllerService.*;
import static org.apache.nifi.distributed.cache.client.DistributedMapCacheClientService.*;
import static nl.speyk.nifi.microsoft.graph.processors.utils.CalendarUtils.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class TestInvokeMicrosoftGraphCalendar {

    private TestRunner runner;

    private static final Logger LOGGER;

    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "ERROR");
        System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
        LOGGER = LoggerFactory.getLogger(InvokeMicrosoftGraphCalendar.class);
    }
    @Before
    public void setUp() throws InitializationException {
        runner = TestRunners.newTestRunner(InvokeMicrosoftGraphCalendar.class);

        MicrosoftGraphCredentialControllerService credentialService = mock(MicrosoftGraphCredentialControllerService.class);
        when(credentialService.getIdentifier()).thenReturn("credentials_service");
        runner.addControllerService("credentials_service", credentialService);
        runner.setProperty(credentialService, AUTH_TENANT_ID, "4d9d7628-507b-45a9-b659-03d1f09d16bf");
        runner.setProperty(credentialService, AUTH_CLIENT_ID, "4dcf450b-1c52-4ab4-8c21-2650e1563435");
        runner.setProperty(credentialService, AUTH_CLIENT_SECRET, "OfV7Q~DVbygzsmG8aP.fyjSc0L0pLhlecCIzR");
        runner.setProperty(credentialService, AUTH_SCOPE, "https://graph.microsoft.com/.default");
        runner.setProperty(credentialService, AUTH_GRANT_TYPE, "client_credentials");
        runner.enableControllerService(credentialService);

        DistributedMapCacheClientService mapCacheClientService = mock(DistributedMapCacheClientService.class);
        when(mapCacheClientService.getIdentifier()).thenReturn("map_cache_client_service");
        runner.addControllerService("map_cache_client_service", mapCacheClientService);
        runner.removeProperty(mapCacheClientService, SSL_CONTEXT_SERVICE);
        runner.setProperty(mapCacheClientService, HOSTNAME, "localhost");
        runner.setProperty(mapCacheClientService, COMMUNICATIONS_TIMEOUT, "30 secs");
        runner.setProperty(mapCacheClientService, PORT, "4557");
        runner.enableControllerService(mapCacheClientService);

        runner.setProperty(GRAPH_CONTROLLER_ID, "credentials_service");
        runner.setProperty(GRAPH_DISTRIBUTED_MAPCACHE, "map_cache_client_service");
        runner.setProperty(GRAPH_RS, "Zermelo");
        runner.setProperty(GRAPH_USER_ID, "${'upn-name'}");
        runner.setProperty(GRAPH_IS_UPDATE, "${'is-update'}");
    }

    @Test
    public void testValid() {
        runner.assertValid();
    }

    @Test
    public void testEmptyValidJsonContent() {
        runner.enqueue("[{}]");
        runner.run();
        runner.assertQueueEmpty();
        List<MockFlowFile> results = runner.getFlowFilesForRelationship(REL_FAILURE);
        assertEquals("Empty json content should trigger a failure", 1, results.size());
        MockFlowFile result = results.get(0);
        String resultValue = new String(runner.getContentAsByteArray(result));
        LOGGER.info("Json content is empty:  " + resultValue);
    }
}
