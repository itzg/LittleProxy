package org.littleshoot.proxy;

import java.util.Collection;
import java.util.HashMap;


/**
 * Launches a new HTTP proxy using SSL and a self-signed certificate.
 */
public class SslLauncher {

    /**
     * Starts the proxy from the command line.
     * 
     * @param args Any command line arguments.
     */
    public static void main(final String... args) {
        final int defaultPort = 8080;
        int port;
        if (args.length > 0) {
            final String arg = args[0];
            try {
                port = Integer.parseInt(arg);
            } catch (final NumberFormatException e) {
                port = defaultPort;
            }
        } else {
            port = defaultPort;
        }
        
        System.out.println("About to start SSL server on port: "+port);
        final HttpProxyServer server = new DefaultHttpProxyServer(port, 
            new HashMap<String, Collection<HttpFilter>>(), null, 
            new SelfSignedKeyStoreManager(), null);
        System.out.println("About to start...");
        server.start();
    }
}
