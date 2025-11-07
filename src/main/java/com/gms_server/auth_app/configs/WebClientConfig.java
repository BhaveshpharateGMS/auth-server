package com.gms_server.auth_app.configs;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * ============================================
 * WEB CLIENT CONFIGURATION
 * ============================================
 * 
 * Configures WebClient for optimal performance and reliability:
 * 1. Connection pooling (reuse connections)
 * 2. Timeouts (prevent hanging requests)
 * 3. Buffer size limits (prevent memory issues)
 * 4. Connection lifecycle management
 */
@Configuration
public class WebClientConfig {

    private static final Logger logger = LoggerFactory.getLogger(WebClientConfig.class);

    @Bean
    public WebClient.Builder webClientBuilder() {
        logger.info("⚙️ [CONFIG] Configuring WebClient with connection pooling and timeouts");

        // Configure connection pool
        ConnectionProvider connectionProvider = ConnectionProvider.builder("custom")
                .maxConnections(100)            // Max connections in pool
                .maxIdleTime(Duration.ofSeconds(20))     // Close idle connections after 20s
                .maxLifeTime(Duration.ofSeconds(60))     // Close all connections after 60s
                .pendingAcquireTimeout(Duration.ofSeconds(60))  // Wait max 60s for connection
                .evictInBackground(Duration.ofSeconds(120))     // Background cleanup every 120s
                .build();

        // Configure HTTP client with timeouts
        HttpClient httpClient = HttpClient.create(connectionProvider)
                // Connection timeout: max time to establish connection
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)  // 10 seconds
                // Response timeout: max time to receive response
                .responseTimeout(Duration.ofSeconds(10))
                // Add read/write timeout handlers
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS))
                );

        // Configure buffer size (for large responses)
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(16 * 1024 * 1024)) // 16MB max buffer
                .build();

        logger.info("✅ [CONFIG] WebClient configured successfully");
        logger.info("   - Connection pool: 100 max connections");
        logger.info("   - Connect timeout: 10s");
        logger.info("   - Response timeout: 10s");
        logger.info("   - Max buffer size: 16MB");

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .exchangeStrategies(strategies);
    }
}

