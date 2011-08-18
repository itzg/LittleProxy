package org.littleshoot.proxy;

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;

/**
 * HTTP filter that does nothing.
 */
public class NoOpHttpFilter implements HttpFilter {

    public boolean shouldFilterResponses(HttpRequest httpRequest) {
        return false;
    }

    public HttpResponse filterResponse(HttpResponse response, String requestUri) {
        return response;
    }

    public int getMaxResponseSize() {
        // Should not be called.
        throw new UnsupportedOperationException("Not supported!!");
    }

}
