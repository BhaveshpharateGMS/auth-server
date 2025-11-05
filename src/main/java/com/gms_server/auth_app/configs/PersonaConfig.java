package com.gms_server.auth_app.configs;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;


@Getter
@Setter
@ConfigurationProperties(prefix = "persona")
public class PersonaConfig {
    private String issuer;
    private String organizationId;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String logoutRedirectUri;
    private String projectId;
    private String managementToken;
    private String sessionIdName;

    // Default constructor for Spring
    public PersonaConfig() {
    }

    // Constructor for manual instantiation
    public PersonaConfig(String issuer, String organizationId, String clientId, String clientSecret,
                         String redirectUri, String logoutRedirectUri, String projectId,
                         String managementToken, String sessionIdName) {
        this.issuer = issuer;
        this.organizationId = organizationId;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.logoutRedirectUri = logoutRedirectUri;
        this.projectId = projectId;
        this.managementToken = managementToken;
        this.sessionIdName = sessionIdName;
    }

    @Override
    public String toString() {
        return "PersonaConfig{" +
                "issuer='" + issuer + '\'' +
                ", organizationId='" + organizationId + '\'' +
                ", clientId='" + clientId + '\'' +
                ", redirectUri='" + redirectUri + '\'' +
                ", projectId='" + projectId + '\'' +
                ", sessionIdName='" + sessionIdName + '\'' +
                '}';
    }
}
