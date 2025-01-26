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

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author messi-gao
 */
@RestController
public class RocketMQOriginalProducer {
    private final Logger logger = LogManager.getLogger(this.getClass());

    // public static void main(String[] args) {
    //     SpringApplication.run(RocketMQOriginalProducer.class, args);
    // }

    @Value("${namesrvAddr}")
    private String namesrvAddr;

    private DefaultMQProducer producer;

    @PostConstruct
    public void init() throws MQClientException {

        // Instantiate with specified consumer group name.
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("your_consumer_group_name");

        // Specify name server addresses.
        consumer.setNamesrvAddr(namesrvAddr);

        // Subscribe one more more topics to consume.
        consumer.subscribe("TopicTest", "*");
        // Register callback to execute on arrival of messages fetched from brokers.
        consumer.registerMessageListener(new MessageListenerConcurrently() {

            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                                                            ConsumeConcurrentlyContext context) {
                logger.info("Receive New Messages: {} {}", Thread.currentThread().getName(), msgs);
                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        //Launch the consumer instance.
        consumer.start();

        logger.info("my|Consumer Started.%n");

        producer = new
            DefaultMQProducer("test");
        // Specify name server addresses.
        producer.setNamesrvAddr(namesrvAddr);
        //Launch the instance.
        producer.start();
        logger.info("my|Producer Started.%n");
    }

    // @Bean
    // public DefaultMQProducer producer() throws MQClientException {
    //     DefaultMQProducer producer = new
    //             DefaultMQProducer("test");
    //     // Specify name server addresses.
    //     producer.setNamesrvAddr(namesrvAddr);
    //     //Launch the instance.
    //     producer.start();
    //     return producer;
    // }

    // @Autowired
    // private DefaultMQProducer producer;

    @GetMapping("/original/send")
    public String send()
        throws UnsupportedEncodingException, RemotingException, MQClientException, InterruptedException,
        MQBrokerException {
        Message msg = new Message(
            "TopicTest",
            "TagA",
            "OrderID188",
            "Hello world".getBytes(RemotingHelper.DEFAULT_CHARSET)
        );
        SendResult send = producer.send(msg);
        logger.info(send.getMsgId());
        return "success";
    }

    @GetMapping("/original/sendAsync")
    public String sendAsync()
        throws UnsupportedEncodingException, RemotingException, MQClientException, InterruptedException {
        Message msg = new Message(
            "TopicTest",
            "TagA",
            "OrderID188",
            "Hello world".getBytes(RemotingHelper.DEFAULT_CHARSET)
        );
        producer.send(
            msg, new SendCallback() {
                @Override
                public void onSuccess(SendResult sendResult) {
                    logger.info(sendResult.getMsgId());
                }

                @Override
                public void onException(Throwable e) {
                    logger.info("Exception", e);
                }
            }
        );
        return "success";
    }

    @GetMapping("/testcase")
    public String testcase() {
        try {
            // start producer
            DefaultMQProducer producer = new DefaultMQProducer("please_rename_unique_group_name");
            producer.setNamesrvAddr(namesrvAddr);
            producer.start();
            System.out.printf("Provider Started.%n");

            // send msg
            Message msg = new Message(
                "TopicTest",
                ("Hello RocketMQ sendMsg " + new Date()).getBytes(RemotingHelper.DEFAULT_CHARSET)
            );
            msg.setTags("TagA");
            msg.setKeys("KeyA");
            SendResult sendResult = producer.send(msg);
            System.out.printf("%s send msg: %s%n", new Date(), sendResult);

            // start consumer
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("please_rename_unique_group_name");
                        consumer.setNamesrvAddr(namesrvAddr);
                        consumer.subscribe("TopicTest", "*");
                        consumer.registerMessageListener(new MessageListenerConcurrently() {
                            @Override
                            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                                                                            ConsumeConcurrentlyContext context) {
                                System.out.printf(
                                    "%s Receive New Messages: %s %n", Thread.currentThread().getName(),
                                    new String(msgs.get(0).getBody(), StandardCharsets.UTF_8)
                                );
                                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                            }
                        });
                        consumer.start();
                        System.out.printf("Consumer Started.%n");
                    } catch (Exception e) {
                        logger.error("consumer start error", e);
                    }
                }
            });
            thread.start();
        } catch (Exception e) {
            logger.error("testcase error", e);
        }
        return "SUCCESS";
    }
}
