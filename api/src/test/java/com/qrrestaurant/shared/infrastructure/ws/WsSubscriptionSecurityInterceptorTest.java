package com.qrrestaurant.shared.infrastructure.ws;

import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.infrastructure.persistence.restaurant.InMemoryRestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class WsSubscriptionSecurityInterceptorTest {

    private static final String WS_PATH = "/orders";

    private final InMemoryRestaurantRepository restaurantRepository = new InMemoryRestaurantRepository();
    private final WsSubscriptionSecurityInterceptor interceptor = new WsSubscriptionSecurityInterceptor(restaurantRepository);

    private UUID restaurantId;
    private UUID ownerId;

    @BeforeEach
    void seedRestaurant() {
        restaurantId = UUID.randomUUID();
        ownerId = UUID.randomUUID();
        restaurantRepository.save(Restaurant.from(
                restaurantId, ownerId, "Naia Burger", "naia-burger", null, null, "classique", null, null));
    }

    @Test
    void ownerCanSubscribeToRestaurantTopic() {
        Message<byte[]> message = subscribe("/topic/restaurants/" + restaurantId + WS_PATH, ownerId);
        assertDoesNotThrow(() -> interceptor.preSend(message, null));
    }

    @Test
    void anonymousUserCannotSubscribeToRestaurantTopic() {
        Message<byte[]> message = subscribe("/topic/restaurants/" + restaurantId + WS_PATH, null);
        assertThrows(MessagingException.class, () -> interceptor.preSend(message, null));
    }

    @Test
    void authenticatedUserOwningAnotherRestaurantCannotSubscribe() {
        UUID otherUserId = UUID.randomUUID();
        restaurantRepository.save(Restaurant.from(
                UUID.randomUUID(), otherUserId, "Autre", "autre", null, null, "classique", null, null));
        Message<byte[]> message = subscribe("/topic/restaurants/" + restaurantId + WS_PATH, otherUserId);
        assertThrows(MessagingException.class, () -> interceptor.preSend(message, null));
    }

    @Test
    void unknownUserAttributeCannotSubscribeToRestaurantTopic() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination("/topic/restaurants/" + restaurantId + WS_PATH);
        accessor.setSessionAttributes(Map.of("unrelated", "value"));
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        assertThrows(MessagingException.class, () -> interceptor.preSend(message, null));
    }

    @Test
    void guestCanSubscribeToItsOrderTopicWithoutAccount() {
        Message<byte[]> message = subscribe("/topic/orders/" + UUID.randomUUID(), null);
        assertDoesNotThrow(() -> interceptor.preSend(message, null));
    }

    @Test
    void anyOtherDestinationIsDenied() {
        assertThrows(MessagingException.class,
                () -> interceptor.preSend(subscribe("/topic/everything", ownerId), null));
        assertThrows(MessagingException.class,
                () -> interceptor.preSend(subscribe("/queue/jobs", ownerId), null));
    }

    @Test
    void nonSubscribeMessagesPassThrough() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionAttributes(Map.of(WsAuthHandshakeInterceptor.USER_ID_ATTRIBUTE, ownerId));
        Message<byte[]> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
        assertDoesNotThrow(() -> interceptor.preSend(message, null));
    }

    private Message<byte[]> subscribe(String destination, UUID userId) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setSessionId("session-1");
        if (userId != null) {
            accessor.setSessionAttributes(Map.of(WsAuthHandshakeInterceptor.USER_ID_ATTRIBUTE, userId));
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
