package com.gms_server.auth_app.services;

import com.gms_server.auth_app.configs.PersonaConfig;
import com.gms_server.auth_app.configs.ZitadelCredentialsConfig;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PersonaService {

    private final ZitadelCredentialsConfig zitadelCredentialsConfig;

    public PersonaService(ZitadelCredentialsConfig zitadelCredentialsConfig) {
        this.zitadelCredentialsConfig = zitadelCredentialsConfig;
    }

    public PersonaConfig getPersonaConfig(String persona) {
        if (persona == null) {
            throw new IllegalArgumentException("Persona cannot be null");
        }

        return switch (persona.toLowerCase()) {
            case "vendor" -> new PersonaConfig(
                    zitadelCredentialsConfig.getVendorIssuer(),
                    zitadelCredentialsConfig.getVendorOrganizationId(),
                    zitadelCredentialsConfig.getVendorClientId(),
                    zitadelCredentialsConfig.getVendorClientSecret(),
                    zitadelCredentialsConfig.getVendorRedirectUri(),
                    zitadelCredentialsConfig.getVendorLogoutRedirectUri(),
                    zitadelCredentialsConfig.getVendorProjectId(),
                    zitadelCredentialsConfig.getVendorManagementToken(),
                    zitadelCredentialsConfig.getVendorSessionIdName()
            );
            case "consumer" -> new PersonaConfig(
                    zitadelCredentialsConfig.getConsumerIssuer(),
                    zitadelCredentialsConfig.getConsumerOrganizationId(),
                    zitadelCredentialsConfig.getConsumerClientId(),
                    zitadelCredentialsConfig.getConsumerClientSecret(),
                    zitadelCredentialsConfig.getConsumerRedirectUri(),
                    zitadelCredentialsConfig.getConsumerLogoutRedirectUri(),
                    zitadelCredentialsConfig.getConsumerProjectId(),
                    zitadelCredentialsConfig.getConsumerManagementToken(),
                    zitadelCredentialsConfig.getConsumerSessionIdName()
            );
            case "affiliate" -> new PersonaConfig(
                    zitadelCredentialsConfig.getAffiliateIssuer(),
                    zitadelCredentialsConfig.getAffiliateOrganizationId(),
                    zitadelCredentialsConfig.getAffiliateClientId(),
                    zitadelCredentialsConfig.getAffiliateClientSecret(),
                    zitadelCredentialsConfig.getAffiliateRedirectUri(),
                    zitadelCredentialsConfig.getAffiliateLogoutRedirectUri(),
                    zitadelCredentialsConfig.getAffiliateProjectId(),
                    zitadelCredentialsConfig.getAffiliateManagementToken(),
                    zitadelCredentialsConfig.getAffiliateSessionIdName()
            );
            case "gms" -> new PersonaConfig(
                    zitadelCredentialsConfig.getGmsIssuer(),
                    zitadelCredentialsConfig.getGmsOrganizationId(),
                    zitadelCredentialsConfig.getGmsClientId(),
                    zitadelCredentialsConfig.getGmsClientSecret(),
                    zitadelCredentialsConfig.getGmsRedirectUri(),
                    zitadelCredentialsConfig.getGmsLogoutRedirectUri(),
                    zitadelCredentialsConfig.getGmsProjectId(),
                    zitadelCredentialsConfig.getGmsManagementToken(),
                    zitadelCredentialsConfig.getGmsSessionIdName()
            );
            default -> throw new IllegalArgumentException("Unsupported persona: " + persona);
        };
    }

    /**
     * Checks if given persona is valid.
     */
    public boolean isValidPersona(String persona) {
        return ("vendor".equalsIgnoreCase(persona) ||
                "consumer".equalsIgnoreCase(persona) ||
                "affiliate".equalsIgnoreCase(persona) ||
                "gms".equalsIgnoreCase(persona));
    }

    /**
     * Returns management token for given persona.
     */
    public String getManagementToken(String persona) {
        if (persona == null) return null;

        return switch (persona.toLowerCase()) {
            case "vendor" -> zitadelCredentialsConfig.getVendorManagementToken();
            case "consumer" -> zitadelCredentialsConfig.getConsumerManagementToken();
            case "affiliate" -> zitadelCredentialsConfig.getAffiliateManagementToken();
            case "gms" -> zitadelCredentialsConfig.getGmsManagementToken();
            default -> null;
        };
    }

    /**
     * Returns callback URL for the given persona (uses redirect URI from config)
     */
    public String getCallbackUrl(String persona) {
        if (persona == null) {
            throw new IllegalArgumentException("Persona cannot be null");
        }

        return switch (persona.toLowerCase()) {
            case "vendor" -> zitadelCredentialsConfig.getVendorAfterLoginRedirectUri();
            case "consumer" -> zitadelCredentialsConfig.getConsumerAfterLoginRedirectUri();
            case "affiliate" -> zitadelCredentialsConfig.getAffiliateAfterLoginRedirectUri();
            case "gms" -> zitadelCredentialsConfig.getGmsRedirectUri();
            default -> throw new IllegalArgumentException("Unsupported persona: " + persona);
        };
    }

    /**
     * Returns logout callback URL for the given persona
     */
    public String getLogoutCallbackUrl(String persona) {
        if (persona == null) {
            throw new IllegalArgumentException("Persona cannot be null");
        }

        return switch (persona.toLowerCase()) {
            case "vendor" -> zitadelCredentialsConfig.getVendorLogoutRedirectUri();
            case "consumer" -> zitadelCredentialsConfig.getConsumerLogoutRedirectUri();
            case "affiliate" -> zitadelCredentialsConfig.getAffiliateLogoutRedirectUri();
            case "gms" -> zitadelCredentialsConfig.getGmsLogoutRedirectUri();
            default -> throw new IllegalArgumentException("Unsupported persona: " + persona);
        };
    }

    /**
     * Checks if JWT payload contains the given persona role.
     */
    public boolean hasPersonaRole(Map<String, Object> payload, String projectId, String persona) {
        if (payload == null || persona == null || persona.isEmpty()) {
            return false;
        }

        System.out.printf("Checking persona role '%s' for project '%s' in payload: %s%n",
                persona, projectId, payload);

        // 1. Global roles
        Map<String, Object> globalRoles =
                (Map<String, Object>) payload.get("urn:zitadel:iam:org:project:roles");
        if (globalRoles != null && globalRoles.containsKey(persona)) {
            return true;
        }

        // 2. Project-specific roles
        String projectRolesKey = String.format("urn:zitadel:iam:org:project:%s:roles", projectId);
        Map<String, Object> projectRoles =
                (Map<String, Object>) payload.get(projectRolesKey);

        return projectRoles != null && projectRoles.containsKey(persona);
    }
}
