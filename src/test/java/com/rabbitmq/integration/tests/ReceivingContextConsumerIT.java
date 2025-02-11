// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
//
// Copyright (c) 2018-2020 VMware, Inc. or its affiliates. All rights reserved.
package com.rabbitmq.integration.tests;

import com.rabbitmq.TestUtils;
import com.rabbitmq.jms.admin.RMQConnectionFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.jms.Connection;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static com.rabbitmq.TestUtils.waitUntil;
import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
public class ReceivingContextConsumerIT {

    private static final String QUEUE_NAME = "test.queue." + ReceivingContextConsumerIT.class.getCanonicalName();
    final AtomicInteger receivedCount = new AtomicInteger(0);
    Connection connection;

    protected static void drainQueue(Session session, Queue queue) throws Exception {
        MessageConsumer receiver = session.createConsumer(queue);
        Message msg = receiver.receiveNoWait();
        while (msg != null) {
            msg = receiver.receiveNoWait();
        }
    }

    @BeforeEach
    public void init() throws Exception {
        RMQConnectionFactory connectionFactory = (RMQConnectionFactory) AbstractTestConnectionFactory.getTestConnectionFactory()
            .getConnectionFactory();
        connectionFactory.setReceivingContextConsumer(ctx -> receivedCount.incrementAndGet());
        connection = connectionFactory.createConnection();
        connection.start();
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
        }
        com.rabbitmq.client.ConnectionFactory cf = new com.rabbitmq.client.ConnectionFactory();
        try (com.rabbitmq.client.Connection c = cf.newConnection()) {
            c.createChannel().queueDelete(QUEUE_NAME);
        }
    }

    @Test
    public void sendingContextConsumerShouldBeCalledWhenSendingMessage() throws Exception {
        Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        Queue queue = session.createQueue(QUEUE_NAME);
        MessageConsumer consumer = session.createConsumer(queue);
        consumer.setMessageListener(msg -> {
        });

        int initialCount = receivedCount.get();
        TextMessage message = session.createTextMessage("hello");
        MessageProducer producer = session.createProducer(queue);
        producer.send(message);
        assertThat(waitUntil(Duration.ofSeconds(5), () -> receivedCount.get() == initialCount + 1)).isTrue();
        producer.send(message);
        assertThat(waitUntil(Duration.ofSeconds(5), () -> receivedCount.get() == initialCount + 2)).isTrue();
    }
}
