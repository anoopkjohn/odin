/*
 * Copyright (c) 2017 cFactor Computing Pvt. Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package in.cfcomputing.odin.core.services.oicollector.config;

import com.codahale.metrics.graphite.Graphite;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static org.junit.Assert.assertNotNull;

/**
 * Created by gibugeorge on 08/02/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = {OiCollectorConfiguration.class})
public class OiCollectorConfigurationTest {

    @Autowired(required = false)
    private Graphite graphite;

    @BeforeClass
    public static void setup() {
        System.setProperty("operational-intelligence.graphite.enabled", "true");
        System.setProperty("operational-intelligence.graphite.host", "localhost");
        System.setProperty("operational-intelligence.graphite.port", "2003");
        System.setProperty("operational-intelligence.graphite.amount-of-time-between-polls", "1000");
    }

    @Test
    public void whenGraphiteEnabledBeanIsAvailable() {
        assertNotNull(graphite);
    }
}