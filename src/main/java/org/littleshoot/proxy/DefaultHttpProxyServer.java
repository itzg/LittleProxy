package org.littleshoot.proxy;

import java.lang.Thread.UncaughtExceptionHandler;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP proxy server.
 */
public class DefaultHttpProxyServer implements HttpProxyServer {
    
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final ChannelGroup allChannels = 
        new DefaultChannelGroup("HTTP-Proxy-Server");
            
    private final int port;
    
    private final ProxyAuthorizationManager authenticationManager =
        new DefaultProxyAuthorizationManager();

    private final Map<String, Collection<HttpFilter>> filters;
    
    private final String chainProxyHostAndPort;

    private final KeyStoreManager ksm;

    private final HttpRequestFilter requestFilter;

    private final ServerBootstrap serverBootstrap;

	private boolean started;
    
    /**
     * Creates a new proxy server.
     * 
     * @param port The port the server should run on.
     */
    public DefaultHttpProxyServer(final int port) {
        this(port, new HashMap<String, Collection<HttpFilter>>());
    }
    
    public DefaultHttpProxyServer(final int port, 
        final Map<String, Collection<HttpFilter>> filters) {
        this(port, filters, null, null, null);
    }
    
    /**
     * Creates a new proxy server.
     * 
     * @param port The port the server should run on.
     * @param requestFilter The filter for HTTP requests.
     * @param responseFilters HTTP filters to apply.
     */
    public DefaultHttpProxyServer(final int port,
        final HttpRequestFilter requestFilter,
        final Map<String, Collection<HttpFilter>> responseFilters) {
        this(port, responseFilters, null, null, requestFilter);
    }
    
    /**
     * Creates a new proxy server.
     * 
     * @param port The port the server should run on.
     * @param filters HTTP filters to apply.
     */
    public DefaultHttpProxyServer(final int port, 
        final Map<String, Collection<HttpFilter>> filters,
        final String chainProxyHostAndPort, final KeyStoreManager ksm,
        final HttpRequestFilter requestFilter) {
        this.port = port;
        this.ksm = ksm;
        this.requestFilter = requestFilter;
        this.filters = new HashMap<String, Collection<HttpFilter>>(filters);
        this.chainProxyHostAndPort = chainProxyHostAndPort;
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler() {
            public void uncaughtException(final Thread t, final Throwable e) {
                log.error("Uncaught throwable", e);
            }
        });
        
        this.serverBootstrap = new ServerBootstrap(
            new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool()));
    }
    
    public void addFilter(String hostAndPort, HttpFilter filter) {
    	if (started) {
    		throw new IllegalStateException("Cannot add filters after server is started");
    	}
    	Collection<HttpFilter> collection = filters.get(hostAndPort);
    	if (collection == null) {
    		collection = new ArrayList<HttpFilter>();
    		filters.put(hostAndPort, collection);
    	}
    	if (!collection.contains(filter)) {
    		collection.add(filter);
    	}
    }
    
    public void start() {
        start(false, true);
    }
    
    public void start(final boolean localOnly, final boolean anyAddress) {
        log.info("Starting proxy on port: "+this.port);
        final HttpServerPipelineFactory factory = 
            new HttpServerPipelineFactory(authenticationManager, 
                this.allChannels, this.chainProxyHostAndPort, this.ksm, 
                new DefaultRelayPipelineFactoryFactory(chainProxyHostAndPort, 
                    Collections.unmodifiableMap(filters), requestFilter, this.allChannels));
        serverBootstrap.setPipelineFactory(factory);
        
        // Binding only to localhost can significantly improve the security of
        // the proxy.
        InetSocketAddress isa;
        if (localOnly) {
            isa = new InetSocketAddress("127.0.0.1", port);
        }
        else if (anyAddress) {
            isa = new InetSocketAddress(port);
        } else {
            try {
                isa = new InetSocketAddress(NetworkUtils.getLocalHost(), port);
            } catch (final UnknownHostException e) {
                log.error("Could not get local host?", e);
                isa = new InetSocketAddress(port);
            }
        }
        final Channel channel = serverBootstrap.bind(isa);
        allChannels.add(channel);
        
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                stop();
            }
        }));

        /*
        final ServerBootstrap sslBootstrap = new ServerBootstrap(
            new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool()));
        sslBootstrap.setPipelineFactory(new HttpsServerPipelineFactory());
        sslBootstrap.bind(new InetSocketAddress("127.0.0.1", 8443));
        */
        
        started = true;
    }
    
    public void stop() {
        log.info("Shutting down proxy");
        final ChannelGroupFuture future = allChannels.close();
        future.awaitUninterruptibly(6*1000);
        serverBootstrap.releaseExternalResources();
        log.info("Done shutting down proxy");
    }

    public void addProxyAuthenticationHandler(
        final ProxyAuthorizationHandler pah) {
        this.authenticationManager.addHandler(pah);
    }

    public KeyStoreManager getKeyStoreManager() {
        return this.ksm;
    }

}
