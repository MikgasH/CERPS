package com.example.cerpshashkin.service.security;

import com.example.cerps.common.security.JwtTokenParser;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService extends JwtTokenParser {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private Long jwtExpiration;

    @Override
    protected String getJwtSecret() {
        return jwtSecret;
    }

    public String generateToken(final String username, final Map<String, Object> extraClaims) {
        return buildToken(username, extraClaims, jwtExpiration);
    }

    public String generateToken(final String username) {
        return generateToken(username, new HashMap<>());
    }

    private String buildToken(final String username, final Map<String, Object> extraClaims, final long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(username)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }
}
