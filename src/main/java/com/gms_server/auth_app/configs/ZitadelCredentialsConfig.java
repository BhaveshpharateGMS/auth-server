package com.gms_server.auth_app.configs;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ZitadelCredentialsConfig {

    // --- Redis ---
    @Value("${spring.data.redis.url}")
    private String redisUrl;

    // --- Global Management Token ---
    @Value("${zitadel.global-management-token}")
    private String globalManagementToken;

    // --- Vendor ---
    @Value("${zitadel.vendor.issuer}")
    private String vendorIssuer;

    @Value("${zitadel.vendor.organization-id}")
    private String vendorOrganizationId;

    @Value("${zitadel.vendor.client-id}")
    private String vendorClientId;

    @Value("${zitadel.vendor.client-secret}")
    private String vendorClientSecret;

    @Value("${zitadel.vendor.redirect-uri}")
    private String vendorRedirectUri;

    @Value("${zitadel.vendor.logout-redirect-uri}")
    private String vendorLogoutRedirectUri;

    @Value("${zitadel.vendor.project-id}")
    private String vendorProjectId;

    @Value("${zitadel.vendor.management-token}")
    private String vendorManagementToken;

    @Value("${zitadel.vendor.session-id-name}")
    private String vendorSessionIdName;

    @Value("${zitadel.vendor.after-login-redirect-uri}")
    private String vendorAfterLoginRedirectUri;

    // --- Consumer ---
    @Value("${zitadel.consumer.issuer}")
    private String consumerIssuer;

    @Value("${zitadel.consumer.organization-id}")
    private String consumerOrganizationId;

    @Value("${zitadel.consumer.client-id}")
    private String consumerClientId;

    @Value("${zitadel.consumer.client-secret}")
    private String consumerClientSecret;

    @Value("${zitadel.consumer.redirect-uri}")
    private String consumerRedirectUri;

    @Value("${zitadel.consumer.logout-redirect-uri}")
    private String consumerLogoutRedirectUri;

    @Value("${zitadel.consumer.project-id}")
    private String consumerProjectId;

    @Value("${zitadel.consumer.management-token}")
    private String consumerManagementToken;

    @Value("${zitadel.consumer.session-id-name}")
    private String consumerSessionIdName;

    @Value("${zitadel.consumer.after-login-redirect-uri}")
    private String consumerAfterLoginRedirectUri;

    // --- Affiliate ---
    @Value("${zitadel.affiliate.issuer}")
    private String affiliateIssuer;

    @Value("${zitadel.affiliate.organization-id}")
    private String affiliateOrganizationId;

    @Value("${zitadel.affiliate.client-id}")
    private String affiliateClientId;

    @Value("${zitadel.affiliate.client-secret}")
    private String affiliateClientSecret;

    @Value("${zitadel.affiliate.redirect-uri}")
    private String affiliateRedirectUri;

    @Value("${zitadel.affiliate.logout-redirect-uri}")
    private String affiliateLogoutRedirectUri;

    @Value("${zitadel.affiliate.project-id}")
    private String affiliateProjectId;

    @Value("${zitadel.affiliate.management-token}")
    private String affiliateManagementToken;

    @Value("${zitadel.affiliate.session-id-name}")
    private String affiliateSessionIdName;

    @Value("${zitadel.affiliate.after-login-redirect-uri}")
    private String affiliateAfterLoginRedirectUri;

    // --- GMS ---
    @Value("${zitadel.gms.issuer}")
    private String gmsIssuer;

    @Value("${zitadel.gms.organization-id}")
    private String gmsOrganizationId;

    @Value("${zitadel.gms.client-id}")
    private String gmsClientId;

    @Value("${zitadel.gms.client-secret}")
    private String gmsClientSecret;

    @Value("${zitadel.gms.redirect-uri}")
    private String gmsRedirectUri;

    @Value("${zitadel.gms.logout-redirect-uri}")
    private String gmsLogoutRedirectUri;

    @Value("${zitadel.gms.project-id}")
    private String gmsProjectId;

    @Value("${zitadel.gms.management-token}")
    private String gmsManagementToken;

    @Value("${zitadel.gms.session-id-name}")
    private String gmsSessionIdName;

    @Value("${zitadel.gms.after-login-redirect-uri}")
    private String gmsAfterLoginRedirectUri;

    // --- Getters ---
    public String getRedisUrl() { return redisUrl; }
    public String getGlobalManagementToken() { return globalManagementToken; }

    public String getVendorIssuer() { return vendorIssuer; }
    public String getVendorOrganizationId() { return vendorOrganizationId; }
    public String getVendorClientId() { return vendorClientId; }
    public String getVendorClientSecret() { return vendorClientSecret; }
    public String getVendorRedirectUri() { return vendorRedirectUri; }
    public String getVendorLogoutRedirectUri() { return vendorLogoutRedirectUri; }
    public String getVendorProjectId() { return vendorProjectId; }
    public String getVendorManagementToken() { return vendorManagementToken; }
    public String getVendorSessionIdName() { return vendorSessionIdName; }
    public String getVendorAfterLoginRedirectUri() { return vendorAfterLoginRedirectUri; }

    public String getConsumerIssuer() { return consumerIssuer; }
    public String getConsumerOrganizationId() { return consumerOrganizationId; }
    public String getConsumerClientId() { return consumerClientId; }
    public String getConsumerClientSecret() { return consumerClientSecret; }
    public String getConsumerRedirectUri() { return consumerRedirectUri; }
    public String getConsumerLogoutRedirectUri() { return consumerLogoutRedirectUri; }
    public String getConsumerProjectId() { return consumerProjectId; }
    public String getConsumerManagementToken() { return consumerManagementToken; }
    public String getConsumerSessionIdName() { return consumerSessionIdName; }
    public String getConsumerAfterLoginRedirectUri() { return consumerAfterLoginRedirectUri; }

    public String getAffiliateIssuer() { return affiliateIssuer; }
    public String getAffiliateOrganizationId() { return affiliateOrganizationId; }
    public String getAffiliateClientId() { return affiliateClientId; }
    public String getAffiliateClientSecret() { return affiliateClientSecret; }
    public String getAffiliateRedirectUri() { return affiliateRedirectUri; }
    public String getAffiliateLogoutRedirectUri() { return affiliateLogoutRedirectUri; }
    public String getAffiliateProjectId() { return affiliateProjectId; }
    public String getAffiliateManagementToken() { return affiliateManagementToken; }
    public String getAffiliateSessionIdName() { return affiliateSessionIdName; }
    public String getAffiliateAfterLoginRedirectUri() { return affiliateAfterLoginRedirectUri; }

    public String getGmsIssuer() { return gmsIssuer; }
    public String getGmsOrganizationId() { return gmsOrganizationId; }
    public String getGmsClientId() { return gmsClientId; }
    public String getGmsClientSecret() { return gmsClientSecret; }
    public String getGmsRedirectUri() { return gmsRedirectUri; }
    public String getGmsLogoutRedirectUri() { return gmsLogoutRedirectUri; }
    public String getGmsProjectId() { return gmsProjectId; }
    public String getGmsManagementToken() { return gmsManagementToken; }
    public String getGmsSessionIdName() { return gmsSessionIdName; }
    public String getGmsAfterLoginRedirectUri() { return gmsAfterLoginRedirectUri; }
}