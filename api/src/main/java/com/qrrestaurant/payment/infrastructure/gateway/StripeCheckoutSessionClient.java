package com.qrrestaurant.payment.infrastructure.gateway;

import com.stripe.exception.StripeException;
import com.stripe.param.checkout.SessionCreateParams;

public interface StripeCheckoutSessionClient {

    String createCheckoutSessionUrl(SessionCreateParams params) throws StripeException;
}
