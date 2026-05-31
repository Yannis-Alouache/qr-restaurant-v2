package com.qrrestaurant.shared.infrastructure.events;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

import java.util.ArrayList;
import java.util.List;

public class RecordingMessageChannel implements MessageChannel {
    private final List<PublishedMessage> publishedMessages = new ArrayList<>();

    @Override
    public boolean send(Message<?> message) {
        return send(message, 0);
    }

    @Override
    public boolean send(Message<?> message, long timeout) {
        publishedMessages.add(new PublishedMessage(
                SimpMessageHeaderAccessor.getDestination(message.getHeaders()),
                message.getPayload()));
        return true;
    }

    public List<PublishedMessage> publishedMessages() {
        return List.copyOf(publishedMessages);
    }
}
