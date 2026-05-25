package com.qrrestaurant.payment.infrastructure;

import com.stripe.exception.StripeException;
import com.stripe.param.checkout.SessionCreateParams;

public interface StripeCheckoutSessionClient {

    String createCheckoutSessionUrl(SessionCreateParams params) throws StripeException;
}
