package com.qrrestaurant.shared.infrastructure.ws;

import com.qrrestaurant.restaurant.domain.RestaurantRepository;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Autorise chaque souscription STOMP en fonction de sa destination :
 * <ul>
 *   <li>{@code /topic/restaurants/{id}/**} — réservé au propriétaire du
 *       restaurant (même vérification que {@code UpdateOrderStatusUseCase}) ;
 *       sans elle, n'importe quelle connexion anonyme peut observer l'activité
 *       des commandes d'un restaurant dont l'identifiant est public ;</li>
 *   <li>{@code /topic/orders/{orderId}} — ouvert : le sujet qui suit sa
 *       commande n'a pas de compte, l'orderId (UUID v4 non devinable) joue le
 *       rôle de capacité ;</li>
 *   <li>toute autre destination est refusée.</li>
 * </ul>
 */
@Component
public class WsSubscriptionSecurityInterceptor implements ChannelInterceptor {

    private static final Pattern RESTAURANT_TOPIC =
            Pattern.compile("^/topic/restaurants/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})(/.*)?$");
    private static final Pattern ORDER_TOPIC =
            Pattern.compile("^/topic/orders/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})$");

    private final RestaurantRepository restaurantRepository;

    public WsSubscriptionSecurityInterceptor(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() != StompCommand.SUBSCRIBE) {
            return message;
        }

        String destination = accessor.getDestination();
        if (destination != null && ORDER_TOPIC.matcher(destination).matches()) {
            return message;
        }

        if (destination != null) {
            var matcher = RESTAURANT_TOPIC.matcher(destination);
            if (matcher.matches() && isRestaurantOwner(accessor, UUID.fromString(matcher.group(1)))) {
                return message;
            }
        }

        throw new MessagingException(
                destination == null ? "Destination manquante" : "Souscription non autorisée à " + destination);
    }

    private boolean isRestaurantOwner(StompHeaderAccessor accessor, UUID restaurantId) {
        UUID userId = authenticatedUserId(accessor);
        if (userId == null) {
            return false;
        }
        return restaurantRepository.findByUserId(userId)
                .map(restaurant -> restaurant.getId().equals(restaurantId))
                .orElse(false);
    }

    private UUID authenticatedUserId(StompHeaderAccessor accessor) {
        Object attribute = accessor.getSessionAttributes() == null
                ? null
                : accessor.getSessionAttributes().get(WsAuthHandshakeInterceptor.USER_ID_ATTRIBUTE);
        return attribute instanceof UUID userId ? userId : null;
    }
}
