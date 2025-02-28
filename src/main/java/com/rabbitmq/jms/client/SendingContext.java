// This Source Code Form is subject to the terms of the Mozilla Public
// License, v. 2.0. If a copy of the MPL was not distributed with this
// file, You can obtain one at https://mozilla.org/MPL/2.0/.
//
// Copyright (c) 2018-2020 VMware, Inc. or its affiliates. All rights reserved.

package com.rabbitmq.jms.client;

import jakarta.jms.Destination;
import jakarta.jms.Message;

/**
 * Context when sending message.
 *
 * @see com.rabbitmq.jms.admin.RMQConnectionFactory#setSendingContextConsumer(SendingContextConsumer)
 * @see SendingContextConsumer
 * @since 1.11.0
 */
public class SendingContext {

    private final Message message;

    private final Destination destination;

    public SendingContext(Destination destination, Message message) {
        this.destination = destination;
        this.message = message;
    }

    public Destination getDestination() {
        return destination;
    }

    public Message getMessage() {
        return message;
    }
}
