package com.qrrestaurant.payment.presentation;

import com.qrrestaurant.auth.infrastructure.JwtService;
import com.qrrestaurant.payment.application.CreateCheckoutSessionUseCase;
import com.qrrestaurant.payment.domain.PaymentGateway;
import com.qrrestaurant.shared.presentation.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PaymentControllerHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateCheckoutSessionUseCase createCheckoutSessionUseCase;

    @MockBean
    private JwtService jwtService;

    @Test
    void shouldExposeActionablePaymentErrorWhenCheckoutSessionCannotBeCreated() throws Exception {
        UUID orderId = UUID.randomUUID();
        when(createCheckoutSessionUseCase.execute(orderId))
                .thenThrow(new PaymentGateway.CheckoutSessionCreationException(
                        "Le paiement en ligne est temporairement indisponible. Réessayez dans quelques instants.",
                        new RuntimeException("stripe unavailable")));

        mockMvc.perform(post("/api/public/payments/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "%s"
                                }
                                """.formatted(orderId)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(
                        "Le paiement en ligne est temporairement indisponible. Réessayez dans quelques instants."));
    }
}
