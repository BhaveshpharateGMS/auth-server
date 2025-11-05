package com.gms_server.auth_app.services;

import com.gms_server.auth_app.utils.RedisService;
import com.gms_server.auth_app.utils.ZitadelApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class CachingService {

    private static final Logger logger = LoggerFactory.getLogger(CachingService.class);

    private final ZitadelApiService zitadelApiService;
    private final RedisService redisService;

    public CachingService(ZitadelApiService zitadelApiService, RedisService redisService) {
        this.zitadelApiService = zitadelApiService;
        this.redisService = redisService;
    }

    /** ---------------- TOKEN → USER INFO ---------------- */
    public Map<String, Object> getUserInfoByToken(String accessToken, String issuer) {
        String key = "token:userinfo:" + accessToken;
        Map<String, Object> cached = redisService.getValue(key, Map.class);
        if (cached != null) {
            logger.info("UserInfo cache hit (redis) {}", cached);
            return cached;
        }
        logger.info("UserInfo cache miss — fetching from Zitadel {}", accessToken);
        try {
            Map<String, Object> userInfo = zitadelApiService.getUserInfo(accessToken, issuer).block();
            if (userInfo != null) {
                redisService.setValueWithExpiry(key, userInfo, 15, java.util.concurrent.TimeUnit.MINUTES);
            }
            return userInfo;
        } catch (Exception e) {
            logger.error("Failed to fetch UserInfo from Zitadel: {}", e.getMessage(), e);
            return null;
        }
    }

    /** ---------------- USERID → USER INFO + ROLES ---------------- */
    public void cacheUser(String userId, Map<String, Object> userData) {
        if (userId != null && userData != null) {
            String key = "user:userinfo:" + userId;
            redisService.setValueWithExpiry(key, userData, 30, java.util.concurrent.TimeUnit.MINUTES); // Use 30min expiry
            logger.debug("User cached successfully (redis): {}", userId);
        }
    }

    public Map<String, Object> getUserById(String userId) {
        String key = "user:userinfo:" + userId;
        Map<String, Object> cached = redisService.getValue(key, Map.class);
        if (cached != null) {
            logger.debug("User cache hit (redis)");
        } else {
            logger.debug("User cache miss for ID: {}", userId);
        }
        return cached;
    }

    /** ---------------- INVALIDATION & MONITORING ---------------- */
    public void invalidateUser(String userId) {
        String key = "user:userinfo:" + userId;
        redisService.delete(key);
        logger.info("Invalidated user cache: {}", userId);
    }

    public void invalidateToken(String accessToken) {
        String key = "token:userinfo:" + accessToken;
        redisService.delete(key);
        logger.info("Invalidated token cache");
    }

//    public void clearAllCaches() {
//        // Not implemented; would require Redis key scan & delete by prefix, which is discouraged if cache can be large.
//        logger.warn("ClearAllCaches called on redis, but NOT implemented. Use Redis FLUSH or appropriate command if needed!");
//    }
//
//    public String getCacheStats() {
//        return "Redis cache does not support local stats; monitor externally.";
//    }
}
