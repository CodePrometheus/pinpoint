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
package com.pinpoint.test.plugin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author messi-gao
 */
@SpringBootApplication
public class RocketMQOriginalApp {
    /**
     -javaagent:/Users/zhouzixin/agent-framework/pinpoint/agent-module/agent/target/pinpoint-agent-3.1.0-SNAPSHOT/pinpoint-bootstrap-3.1.0-SNAPSHOT.jar
     -Dpinpoint.agentId=rocketmq-agentId
     -Dpinpoint.applicationName=rocketmq-name
     -Dpinpoint.profiler.profiles.active=local
     -Dprofiler.rocketmq.producer.enable=true
     -Dprofiler.rocketmq.consumer.enable=true
     -Dprofiler.rocketmq.basePackage=com.pinpoint.test.plugin
     */
    public static void main(String[] args) {
        SpringApplication.run(RocketMQOriginalApp.class, args);
    }
}
