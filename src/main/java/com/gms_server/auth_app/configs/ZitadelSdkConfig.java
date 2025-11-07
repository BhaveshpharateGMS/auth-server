package com.gms_server.auth_app.configs;

import com.zitadel.ApiException;
import com.zitadel.Zitadel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Zitadel SDK
 * Supports both Service Account (Private Key JWT) and Personal Access Token authentication
 */
@Configuration
public class ZitadelSdkConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(ZitadelSdkConfig.class);
    
    @Value("${zitadel.instance-url:https://zitadel-gms-7a6uvy.us1.zitadel.cloud}")
    private String instanceUrl;
    
    @Value("${zitadel.key-file-path:}")
    private String keyFilePath;
    
    @Value("${zitadel.access-token:YbYMne1IUxMs7B8O2eAivrEjd6Yg54tJdAfF65EXqVmw8u9dlJcjepi1RWrrm6ksXvQWPMk}")
    private String accessToken;
    
    /**
     * Create Zitadel SDK client bean
     * Automatically chooses authentication method based on configuration
     * 
     * @return Configured Zitadel client
     * @throws ApiException if initialization fails
     */
    @Bean
    public Zitadel zitadelClient() throws ApiException {
        logger.info("Initializing Zitadel SDK client for instance: {}", instanceUrl);
        
        try {
            Zitadel zitadel;
            
            // Prefer Service Account if key file is configured
            if (keyFilePath != null && !keyFilePath.isEmpty()) {
                logger.info("Using Service Account authentication with key file: {}", keyFilePath);
                zitadel = Zitadel.withPrivateKey(instanceUrl, keyFilePath);
                logger.info("✓ Zitadel SDK client initialized with Service Account");
            } 
            // Fall back to Personal Access Token
            else if (accessToken != null && !accessToken.isEmpty()) {
                logger.info("Using Personal Access Token authentication");
                zitadel = Zitadel.withAccessToken(instanceUrl, accessToken);
                logger.info("✓ Zitadel SDK client initialized with PAT");
            } 
            else {
                throw new IllegalStateException(
                    "No authentication configured! Set either zitadel.key-file-path or zitadel.access-token"
                );
            }
            
            return zitadel;
        } catch (ApiException e) {
            logger.error("✗ Failed to initialize Zitadel SDK client: {}", e.getMessage(), e);
            throw e;
        }
    }
}
