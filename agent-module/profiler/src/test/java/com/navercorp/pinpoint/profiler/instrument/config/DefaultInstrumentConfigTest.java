/*
 * Copyright 2021 NAVER Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.navercorp.pinpoint.profiler.instrument.config;

import com.navercorp.pinpoint.bootstrap.config.Filter;
import com.navercorp.pinpoint.common.config.util.ValueAnnotationProcessor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Properties;

public class DefaultInstrumentConfigTest {
    private final ValueAnnotationProcessor valueAnnotationProcessor = new ValueAnnotationProcessor();

    @Test
    public void defaultProfilableClassFilter() {
        InstrumentConfig instrumentConfig = new DefaultInstrumentConfig();
        Filter<String> profilableClassFilter = instrumentConfig.getProfilableClassFilter();
        Assertions.assertFalse(profilableClassFilter.filter("net/spider/king/wang/Jjang"));
    }

    @Test
    public void getCallStackMaxDepth() {
        Properties properties = new Properties();
        properties.setProperty("profiler.callstack.max.depth", "64");

        // Read
        InstrumentConfig instrumentConfig = new DefaultInstrumentConfig();
        valueAnnotationProcessor.process(instrumentConfig, properties::getProperty);

        Assertions.assertEquals(64, instrumentConfig.getCallStackMaxDepth());

        // Unlimited
        properties.setProperty("profiler.callstack.max.depth", "-1");
        instrumentConfig = new DefaultInstrumentConfig();
        valueAnnotationProcessor.process(instrumentConfig, properties::getProperty);
        Assertions.assertEquals(-1, instrumentConfig.getCallStackMaxDepth());

        // Minimum calibration
        properties.setProperty("profiler.callstack.max.depth", "0");
        instrumentConfig = new DefaultInstrumentConfig();
        valueAnnotationProcessor.process(instrumentConfig, properties::getProperty);

        Assertions.assertEquals(2, instrumentConfig.getCallStackMaxDepth());
    }

    @Test
    public void getCallStackMaxSequence() {
        Properties properties = new Properties();
        properties.setProperty("profiler.callstack.max.sequence", "5000");

        // Read
        InstrumentConfig instrumentConfig = new DefaultInstrumentConfig();
        valueAnnotationProcessor.process(instrumentConfig, properties::getProperty);

        Assertions.assertEquals(5000, instrumentConfig.getCallStackMaxSequence());

        // Unlimited
        properties.setProperty("profiler.callstack.max.sequence", "-1");
        instrumentConfig = new DefaultInstrumentConfig();
        valueAnnotationProcessor.process(instrumentConfig, properties::getProperty);
        Assertions.assertEquals(32767, instrumentConfig.getCallStackMaxSequence());

        properties.setProperty("profiler.callstack.max.sequence", "-73");
        instrumentConfig = new DefaultInstrumentConfig();
        valueAnnotationProcessor.process(instrumentConfig, properties::getProperty);
        Assertions.assertEquals(32767, instrumentConfig.getCallStackMaxSequence());

        // short overflow
        properties.setProperty("profiler.callstack.max.sequence", "1048576");
        instrumentConfig = new DefaultInstrumentConfig();
        valueAnnotationProcessor.process(instrumentConfig, properties::getProperty);
        Assertions.assertEquals(32767, instrumentConfig.getCallStackMaxSequence());

        // Minimum calibration
        properties.setProperty("profiler.callstack.max.sequence", "0");
        instrumentConfig = new DefaultInstrumentConfig();
        valueAnnotationProcessor.process(instrumentConfig, properties::getProperty);

        Assertions.assertEquals(4, instrumentConfig.getCallStackMaxSequence());
    }

}