package com.qrrestaurant.payment.infrastructure;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeSdkCheckoutSessionClient implements StripeCheckoutSessionClient {

    private final StripeClient stripeClient;

    public StripeSdkCheckoutSessionClient(@Value("${stripe.secret-key}") String secretKey) {
        this.stripeClient = new StripeClient(secretKey);
    }

    @Override
    public String createCheckoutSessionUrl(SessionCreateParams params) throws StripeException {
        return stripeClient.v1().checkout().sessions().create(params).getUrl();
    }
}
