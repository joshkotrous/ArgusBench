package com.financehub.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;
import java.util.Map;

@Component
public class JwtUtil {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SECRET_KEY = "your-256-bit-secret"; // This should be securely stored and retrieved
    
    public boolean validateToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return false;
            }
            
            String header = new String(Base64.getUrlDecoder().decode(parts[0]));
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            
            Map<String, Object> headerMap = objectMapper.readValue(header, Map.class);
            Map<String, Object> payloadMap = objectMapper.readValue(payload, Map.class);
            
            String alg = (String) headerMap.get("alg");
            if ("none".equals(alg)) {
                // Reject tokens with 'none' algorithm to prevent bypass
                return false;
            }
            
            if ("HS256".equals(alg)) {
                return validateHS256(parts[0] + "." + parts[1], parts[2]);
            }
            
            // Reject unsupported algorithms
            return false;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    private boolean validateHS256(String data, String signature) {
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET_KEY.getBytes(), "HmacSHA256");
            hmac.init(secretKeySpec);
            byte[] computedHash = hmac.doFinal(data.getBytes());
            String computedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(computedHash);
            return computedSignature.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }
    
    public String extractSubject(String token) {
        try {
            String[] parts = token.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            Map<String, Object> claims = objectMapper.readValue(payload, Map.class);
            return (String) claims.get("sub");
        } catch (Exception e) {
            return null;
        }
    }
    
    public String extractTenantId(String token) {
        try {
            String[] parts = token.split("\\.");
            String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
            Map<String, Object> claims = objectMapper.readValue(payload, Map.class);
            return (String) claims.get("tenant_id");
        } catch (Exception e) {
            return null;
        }
    }
}