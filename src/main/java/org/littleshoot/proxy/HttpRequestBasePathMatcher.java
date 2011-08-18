package org.littleshoot.proxy;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.netty.handler.codec.http.HttpRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Request rule that operates on the request path.
 */
public class HttpRequestBasePathMatcher implements HttpRequestMatcher {
    private final Logger log = LoggerFactory.getLogger(getClass());
	private final Pattern pathPattern;
    
    /**
     * Creates a new URI rule.
     * 
     * @param path The path to match.
     */
    public HttpRequestBasePathMatcher(final Pattern pathPattern) {
        this.pathPattern = pathPattern;
    }

    public HttpRequestBasePathMatcher(String patternRegex) {
    	this(Pattern.compile(patternRegex));
	}

	public boolean shouldFilterResponses(final HttpRequest httpRequest) {
		String uri = httpRequest.getUri();
		int startOfPath = uri.indexOf('/', 8);
		if (startOfPath >= 0) {
			final String pathPart = uri.substring(startOfPath);
			Matcher matcher = pathPattern.matcher(pathPart);
			final boolean result = matcher.lookingAt();
			log.debug("Looking at {} with {} result: {}", new Object[]{pathPart, pathPattern, result});
			return result;
		}
		return false;
    }
    
    @Override
    public String toString() {
        return "Request Matcher for: "+this.pathPattern;
    }
}
