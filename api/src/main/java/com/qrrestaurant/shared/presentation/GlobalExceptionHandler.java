package com.qrrestaurant.shared.presentation;

import com.qrrestaurant.auth.application.AuthService;
import com.qrrestaurant.order.application.CreateOrderUseCase;
import com.qrrestaurant.order.domain.OrderPricingPolicy;
import com.qrrestaurant.payment.application.CreateCheckoutSessionUseCase;
import com.qrrestaurant.payment.domain.PaymentGateway;
import com.qrrestaurant.payment.presentation.StripeWebhookController;
import com.qrrestaurant.restaurant.application.RestaurantSlugGenerator;
import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantTheme;
import com.qrrestaurant.auth.domain.PasswordPolicy;
import com.qrrestaurant.shared.domain.StorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
            com.qrrestaurant.menu.application.GetMenuUseCase.RestaurantNotFoundException.class,
            CreateOrderUseCase.RestaurantNotFoundException.class
    })
    public ResponseEntity<ApiErrorResponse> handleRestaurantNotFound(RuntimeException ex) {
        return respond(HttpStatus.NOT_FOUND, "Restaurant introuvable");
    }

    @ExceptionHandler({
            com.qrrestaurant.menu.application.ManageCategoryUseCase.CategoryNotFoundException.class,
            com.qrrestaurant.menu.application.ManageMenuItemUseCase.MenuItemNotFoundException.class,
            com.qrrestaurant.menu.application.ManageCompositionUseCase.CompositionNotFoundException.class,
            com.qrrestaurant.order.application.GetOrderUseCase.OrderNotFoundException.class,
            com.qrrestaurant.order.application.UpdateOrderStatusUseCase.OrderNotFoundException.class,
            CreateCheckoutSessionUseCase.OrderNotFoundException.class
    })
    public ResponseEntity<ApiErrorResponse> handleNotFound(RuntimeException ex) {
        return respond(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({
            com.qrrestaurant.menu.application.ManageCategoryUseCase.NoRestaurantException.class,
            com.qrrestaurant.menu.application.GetAdminMenuUseCase.NoRestaurantException.class,
            com.qrrestaurant.order.application.GetRestaurantOrdersUseCase.NoRestaurantException.class,
            com.qrrestaurant.order.application.UpdateOrderStatusUseCase.NoRestaurantException.class,
            com.qrrestaurant.restaurant.application.GetRestaurantUseCase.NoRestaurantException.class
    })
    public ResponseEntity<ApiErrorResponse> handleNoRestaurant(RuntimeException ex) {
        return respond(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({
            com.qrrestaurant.order.domain.Order.InvalidStatusTransitionException.class,
            com.qrrestaurant.order.domain.Order.CheckoutUnavailableException.class,
            com.qrrestaurant.order.domain.Order.UnpaidOrderStatusUpdateException.class
    })
    public ResponseEntity<ApiErrorResponse> handleConflict(RuntimeException ex) {
        return respond(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({
            PasswordPolicy.PasswordTooShortException.class,
            RestaurantTheme.InvalidThemeException.class,
            OrderPricingPolicy.InvalidQuantityException.class,
            OrderPricingPolicy.MenuItemsNotFoundException.class,
            OrderPricingPolicy.ItemUnavailableException.class,
            OrderPricingPolicy.InvalidOrderItemException.class,
            Restaurant.PaymentNotConfiguredException.class,
            StripeWebhookController.InvalidWebhookSignatureException.class,
            StripeWebhookController.MissingOrderMetadataException.class,
            StripeWebhookController.InvalidWebhookPayloadException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(RuntimeException ex) {
        return respond(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne du serveur");
    }

    @ExceptionHandler({
            CreateCheckoutSessionUseCase.InvalidOrderException.class,
            OrderPricingPolicy.ItemRestaurantMismatchException.class
    })
    public ResponseEntity<ApiErrorResponse> handleForbidden(RuntimeException ex) {
        return respond(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    @ExceptionHandler({
            AuthService.InvalidCredentialsException.class
    })
    public ResponseEntity<ApiErrorResponse> handleUnauthorized(RuntimeException ex) {
        return respond(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler({
            AuthService.EmailAlreadyRegisteredException.class,
            com.qrrestaurant.restaurant.application.OnboardingUseCase.OwnerAlreadyHasRestaurantException.class
    })
    public ResponseEntity<ApiErrorResponse> handleAlreadyExists(RuntimeException ex) {
        return respond(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler({
            RestaurantSlugGenerator.SlugGenerationException.class,
            CreateCheckoutSessionUseCase.PriceUpdateException.class
    })
    public ResponseEntity<ApiErrorResponse> handleServerError(RuntimeException ex) {
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    @ExceptionHandler(StorageService.StorageUploadException.class)
    public ResponseEntity<ApiErrorResponse> handleStorageUploadError(StorageService.StorageUploadException ex) {
        return respond(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(PaymentGateway.CheckoutSessionCreationException.class)
    public ResponseEntity<ApiErrorResponse> handlePaymentUnavailable(PaymentGateway.CheckoutSessionCreationException ex) {
        return respond(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Requête invalide");
        return respond(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingHeader(MissingRequestHeaderException ex) {
        if ("Stripe-Signature".equalsIgnoreCase(ex.getHeaderName())) {
            return respond(HttpStatus.BAD_REQUEST, "Signature Stripe manquante");
        }
        return respond(HttpStatus.BAD_REQUEST, "En-tête requis manquant");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadableMessage(HttpMessageNotReadableException ex) {
        return respond(HttpStatus.BAD_REQUEST, "Requête invalide");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return respond(HttpStatus.BAD_REQUEST, "Paramètre invalide");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return respond(HttpStatus.METHOD_NOT_ALLOWED, "Méthode HTTP non supportée");
    }

    private ResponseEntity<ApiErrorResponse> respond(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(status.value(), message));
    }
}
