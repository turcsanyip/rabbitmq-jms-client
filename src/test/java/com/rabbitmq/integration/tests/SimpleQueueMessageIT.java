// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
//
// Copyright (c) 2013-2020 VMware, Inc. or its affiliates. All rights reserved.
package com.rabbitmq.integration.tests;

import java.io.Serializable;

import jakarta.jms.DeliveryMode;
import jakarta.jms.Queue;
import jakarta.jms.QueueReceiver;
import jakarta.jms.QueueSender;
import jakarta.jms.QueueSession;
import jakarta.jms.Session;

import org.junit.jupiter.api.Test;

/**
 * Integration test for simple browsing of a queue.
 */
public class SimpleQueueMessageIT extends AbstractITQueue {

    private static final String QUEUE_NAME = "test.queue."+SimpleQueueMessageIT.class.getCanonicalName();
    private static final long TEST_RECEIVE_TIMEOUT = 1000; // one second

    private void messageTestBase(MessageTestType mtt) throws Exception {
        try {
            queueConn.start();
            QueueSession queueSession = queueConn.createQueueSession(false, Session.DUPS_OK_ACKNOWLEDGE);
            Queue queue = queueSession.createQueue(QUEUE_NAME);

            drainQueue(queueSession, queue);

            QueueSender queueSender = queueSession.createSender(queue);
            queueSender.setDeliveryMode(DeliveryMode.NON_PERSISTENT);
            queueSender.send(mtt.gen(queueSession, (Serializable)queue));
        } finally {
            reconnect();
        }

        queueConn.start();
        QueueSession queueSession = queueConn.createQueueSession(false, Session.DUPS_OK_ACKNOWLEDGE);
        Queue queue = queueSession.createQueue(QUEUE_NAME);
        QueueReceiver queueReceiver = queueSession.createReceiver(queue);
        mtt.check(queueReceiver.receive(TEST_RECEIVE_TIMEOUT), (Serializable)queue);
    }

    @Test
    public void testSendAndReceiveLongTextMessage() throws Exception {
        messageTestBase(MessageTestType.LONG_TEXT);
    }

    @Test
    public void testSendAndReceiveTextMessage() throws Exception {
        messageTestBase(MessageTestType.TEXT);
    }

    @Test
    public void testSendAndReceiveBytesMessage() throws Exception {
        messageTestBase(MessageTestType.BYTES);
    }

    @Test
    public void testSendAndReceiveLongBytesMessage() throws Exception {
        messageTestBase(MessageTestType.LONG_BYTES);
    }

    @Test
    public void testSendAndReceiveMapMessage() throws Exception {
        messageTestBase(MessageTestType.MAP);
    }

    @Test
    public void testSendAndReceiveStreamMessage() throws Exception {
        messageTestBase(MessageTestType.STREAM);
    }

    @Test
    public void testSendAndReceiveObjectMessage() throws Exception {
        messageTestBase(MessageTestType.OBJECT);
    }
}
