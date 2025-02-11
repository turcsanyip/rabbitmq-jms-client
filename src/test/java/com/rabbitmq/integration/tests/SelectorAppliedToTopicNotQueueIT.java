// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
//
// Copyright (c) 2018-2020 VMware, Inc. or its affiliates. All rights reserved.
package com.rabbitmq.integration.tests;

import com.rabbitmq.jms.admin.RMQConnectionFactory;
import com.rabbitmq.jms.client.message.RMQTextMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.jms.DeliveryMode;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.Queue;
import jakarta.jms.QueueConnection;
import jakarta.jms.QueueSession;
import jakarta.jms.Session;
import jakarta.jms.TextMessage;
import jakarta.jms.Topic;
import jakarta.jms.TopicConnection;
import jakarta.jms.TopicPublisher;
import jakarta.jms.TopicSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * To make sure Session#createConsumer works for at least topic when a
 * selector is provided.
 *
 * See https://github.com/rabbitmq/rabbitmq-jms-client/issues/52
 */
public class SelectorAppliedToTopicNotQueueIT {

    private static final String TOPIC_NAME = "test.topic." + SelectorAppliedToTopicNotQueueIT.class.getCanonicalName();
    private static final String QUEUE_NAME = "test.queue." + SimpleQueueMessageIT.class.getCanonicalName();
    private static final String MESSAGE1 = "Hello " + SelectorAppliedToTopicNotQueueIT.class.getName();

    RMQConnectionFactory connFactory;
    TopicConnection topicConn;
    QueueConnection queueConn;

    @BeforeEach
    public void beforeTests() throws Exception {
        this.connFactory =
            (RMQConnectionFactory) AbstractTestConnectionFactory.getTestConnectionFactory()
                .getConnectionFactory();
        this.topicConn = connFactory.createTopicConnection();
        this.queueConn = connFactory.createQueueConnection();
    }

    @AfterEach
    public void afterTests() throws Exception {
        if (this.topicConn != null)
            this.topicConn.close();
        if (queueConn != null)
            queueConn.close();
    }

    @Test
    public void sendAndReceiveSupportedOnTopic() throws Exception {
        topicConn.start();
        TopicSession topicSession = topicConn.createTopicSession(false, Session.DUPS_OK_ACKNOWLEDGE);
        Topic topic = topicSession.createTopic(TOPIC_NAME);
        MessageConsumer receiver = topicSession.createConsumer(topic, "boolProp");
        sendAndReceive(topicSession, topic, receiver);
    }

    @Test
    public void sendAndReceiveSupportedOnTopicNoLocal() throws Exception {
        topicConn.start();
        TopicSession topicSession = topicConn.createTopicSession(false, Session.DUPS_OK_ACKNOWLEDGE);
        Topic topic = topicSession.createTopic(TOPIC_NAME);
        MessageConsumer receiver = topicSession.createConsumer(topic, "boolProp", false);
        sendAndReceive(topicSession, topic, receiver);
    }

    private void sendAndReceive(TopicSession topicSession, Topic topic, MessageConsumer receiver) throws JMSException {
        TopicPublisher sender = topicSession.createPublisher(topic);
        sender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
        TextMessage message = topicSession.createTextMessage(MESSAGE1);
        message.setBooleanProperty("boolProp", true);
        sender.send(message);
        RMQTextMessage tmsg = (RMQTextMessage) receiver.receive();
        String t = tmsg.getText();
        assertEquals(MESSAGE1, t);
    }

    @Test
    public void sendAndReceiveNotSupportedOnQueue() throws Exception {
        queueConn.start();
        QueueSession queueSession = queueConn.createQueueSession(false, Session.DUPS_OK_ACKNOWLEDGE);
        Queue queue = queueSession.createQueue(QUEUE_NAME);
        assertThrows(UnsupportedOperationException.class, () -> queueSession.createConsumer(queue, "boolProp"));
    }

    @Test
    public void sendAndReceiveNotSupportedOnQueueNoLocal() throws Exception {
        queueConn.start();
        QueueSession queueSession = queueConn.createQueueSession(false, Session.DUPS_OK_ACKNOWLEDGE);
        Queue queue = queueSession.createQueue(QUEUE_NAME);
        assertThrows(UnsupportedOperationException.class, () -> queueSession.createConsumer(queue, "boolProp", false));
    }
}
