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

import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Before;
import org.junit.Test;

public class TestGraphAuthControllerClientService {

    @Before
    public void init() {

    }

    @Test
    public void testService() throws InitializationException {
        final TestRunner runner = TestRunners.newTestRunner(TestProcessor.class);
        final GraphAuthControllerClientService service = new GraphAuthControllerClientService();
        runner.addControllerService("test-good", service);

        runner.setProperty(service, GraphAuthControllerClientService.AUTH_CLIENT_ID, "test-value");
        runner.setProperty(service, GraphAuthControllerClientService.AUTH_TENANT_ID, "test-value");
        runner.setProperty(service, GraphAuthControllerClientService.AUTH_CLIENT_SECRET, "test-value");
        runner.setProperty(service, GraphAuthControllerClientService.AUTH_SCOPE, "test-value");
        runner.setProperty(service, GraphAuthControllerClientService.AUTH_GRANT_TYPE, "test-value");

        runner.enableControllerService(service);

        runner.assertValid(service);
    }

}
