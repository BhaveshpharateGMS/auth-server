package com.gms_server.auth_app.utils;

import com.gms_server.auth_app.configs.PersonaConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class ZitadelApiService {
    private static final Logger logger = LoggerFactory.getLogger(ZitadelApiService.class);
    private final WebClient webClient;

    public ZitadelApiService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Mono<Map> exchangeCodeForTokens(String code, String codeVerifier, PersonaConfig config) {
        logger.info("Exchanging code for tokens with client_id: {}", config.getClientId());
        return webClient.post()
                .uri(config.getIssuer() + "/oauth/v2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "authorization_code")
                        .with("client_id", config.getClientId())
                        .with("code", code)
                        .with("redirect_uri", config.getRedirectUri())
                        .with("code_verifier", codeVerifier)
                )
                .retrieve()
                .bodyToMono(Map.class)
                .doOnError(error -> {
                    if (error instanceof org.springframework.web.reactive.function.client.WebClientResponseException ex) {
                        logger.error("Token exchange failed with status: {}, body: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
                    }
                });
    }

    public Mono<Map> getUserInfo(String accessToken, String issuer) {
        return webClient.get()
                .uri(issuer + "/oidc/v1/userinfo")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(Map.class)
                .retry(2)
                .doOnError(error -> logger.error("Failed to get user info after retries: {}", error.getMessage()));
    }

//    public Mono<Boolean> assignRoleIfNeeded(String userId, String issuer, String projectId,String managementToken,String persona) {
//        if (managementToken == null || managementToken.isBlank()) {
//            return Mono.just(false);
//        }
//        return webClient.post()
//                .uri(issuer + "/management/v1/users/{userId}/grants", userId)
//                .contentType(MediaType.APPLICATION_JSON)
//                .header("Authorization", "Bearer " + managementToken)
//                .bodyValue(Map.of(
//                        "projectId", projectId,
//                        "roleKeys", new String[]{persona}
//                ))
//                .retrieve()
//                .bodyToMono(Void.class)
//                .map(v -> true)
//                .onErrorResume(e -> {
//                    // Handle "already exists" error (409)
//                    if (e instanceof org.springframework.web.reactive.function.client.WebClientResponseException ex
//                            && ex.getRawStatusCode() == 409) {
//                        return Mono.just(true);
//                    }
//                    // log warning and return false
//                    logger.warn("assignVendorRole failed: {}", e.getMessage());
//                    return Mono.just(false);
//                });
//    }


    public Mono<Boolean> assignRoleIfNeeded(String userId, String issuer, String projectId, String managementToken, String persona) {
        if (managementToken == null || managementToken.isBlank()) {
            return Mono.just(false);
        }
        return webClient.post()
                .uri(issuer + "/management/v1/users/{userId}/grants", userId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + managementToken)
                .bodyValue(Map.of(
                        "projectId", projectId,
                        "roleKeys", new String[]{persona}
                ))
                .retrieve()
                .bodyToMono(Void.class)
                .thenReturn(true) // ✅ ensures true is always emitted on success
                .onErrorResume(e -> {
                    if (e instanceof org.springframework.web.reactive.function.client.WebClientResponseException ex
                            && ex.getRawStatusCode() == 409) {
                        return Mono.just(true); // role already exists
                    }
                    logger.warn("assignRoleIfNeeded failed: {}", e.getMessage());
                    return Mono.just(false);
                });
    }


    public Mono<Map> refreshTokens(String refreshToken, PersonaConfig config) {
        return webClient.post()
                .uri(config.getIssuer() + "/oauth/v2/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData("grant_type", "refresh_token")
                        .with("client_id", config.getClientId())
                        .with("refresh_token", refreshToken)
                )
                .retrieve()
                .bodyToMono(Map.class);
    }
}
