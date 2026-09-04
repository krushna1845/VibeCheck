package com.krushna.moviebooking.common.security;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;

/**
 * Client HTTP interceptor that injects perimeter authentication headers
 * (X-Internal-Service and X-Internal-Secret) and propagates Authorization header
 * on outgoing RestTemplate/RestClient requests.
 */
public class InternalAuthInterceptor implements ClientHttpRequestInterceptor {

    private final String serviceName;
    private final String secret;

    public InternalAuthInterceptor(String serviceName, String secret) {
        this.serviceName = serviceName;
        this.secret = secret;
    }

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        request.getHeaders().set(InternalAuthConstants.INTERNAL_SERVICE_HEADER, serviceName);
        request.getHeaders().set(InternalAuthConstants.INTERNAL_SECRET_HEADER, secret);

        // Propagate user Authorization header from active request context if not already set
        if (request.getHeaders().getFirst("Authorization") == null) {
            var attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                String authHeader = attributes.getRequest().getHeader("Authorization");
                if (authHeader != null && !authHeader.isBlank()) {
                    request.getHeaders().set("Authorization", authHeader);
                }
            }
        }

        return execution.execute(request, body);
    }
}
