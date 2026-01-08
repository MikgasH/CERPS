# Criterion 2: Cryptography

## Overview

| Aspect | Details |
|--------|---------|
| **Criterion** | Cryptographic Security Implementation |
| **Password Hashing** | BCrypt (cost factor 12) |
| **Token Signing** | HMAC-SHA256 (JWT) |
| **Data Encryption** | AES-256-GCM |

## Cryptographic Components

```
┌─────────────────────────────────────────────────────────────────┐
│                    CERPS Security Layer                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐  │
│  │  Password Hash  │  │   JWT Tokens    │  │  API Key Store  │  │
│  │     BCrypt      │  │   HMAC-SHA256   │  │   AES-256-GCM   │  │
│  │  (User Service) │  │  (All Services) │  │(Currency Service)│  │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## 1. Password Hashing (BCrypt)

### Why BCrypt

| Factor | BCrypt | SHA-256 | Argon2 |
|--------|--------|---------|--------|
| Adaptive Cost | Configurable | Fixed | Configurable |
| Salt Handling | Automatic | Manual | Automatic |
| Spring Support | Native | Manual | Extra dependency |
| Resistance to GPU | Strong | Weak | Strong |
| Industry Standard | Proven | Not for passwords | Newer |

**Decision:** BCrypt selected for proven security, Spring Security native support, and automatic salt management.

### Implementation

```java
@Configuration
public class SecurityConfig {
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12); // 2^12 iterations
    }
}

@Service
public class AuthService {
    private final PasswordEncoder passwordEncoder;
    
    public void registerUser(String email, String password) {
        String hashedPassword = passwordEncoder.encode(password);
        // Store hashedPassword in database
        // Example: $2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.3EEzZy2Qv
    }
    
    public boolean verifyPassword(String rawPassword, String storedHash) {
        return passwordEncoder.matches(rawPassword, storedHash);
    }
}
```

### BCrypt Hash Structure

```
$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.3EEzZy2QvKjLmu
 │  │  │                      │
 │  │  │                      └── Hash (31 chars)
 │  │  └── Salt (22 chars, Base64)
 │  └── Cost factor (12 = 2^12 = 4096 iterations)
 └── Algorithm version (2a)
```

### Cost Factor Selection

| Cost | Iterations | Time (approx) | Use Case |
|------|------------|---------------|----------|
| 10 | 1,024 | ~100ms | Development |
| 12 | 4,096 | ~300ms | **Production (selected)** |
| 14 | 16,384 | ~1s | High security |

**Decision:** Cost factor 12 provides balance between security (resistant to brute force) and user experience (acceptable login time).

## 2. JWT Token Security (HMAC-SHA256)

### Why HMAC-SHA256

| Factor | HS256 | RS256 | ES256 |
|--------|-------|-------|-------|
| Key Management | Single secret | Public/Private pair | Public/Private pair |
| Performance | Fast | Slower | Fast |
| Complexity | Simple | Complex | Complex |
| Use Case | Single issuer | Multiple verifiers | Multiple verifiers |

**Decision:** HS256 selected for simplicity (single API Gateway validates all tokens) and performance.

### Implementation

```java
@Component
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration}")
    private long jwtExpiration; // 86400000 (24 hours)
    
    public String generateToken(String email, List<String> roles) {
        return Jwts.builder()
            .subject(email)
            .claim("roles", roles)
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(getSigningKey(), Jwts.SIG.HS256)
            .compact();
    }
    
    public Claims validateToken(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
    
    private SecretKey getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

### JWT Token Structure

```json
// Header
{
  "alg": "HS256",
  "typ": "JWT"
}

// Payload
{
  "sub": "user@example.com",
  "roles": ["ROLE_USER"],
  "iat": 1703070000,
  "exp": 1703156400
}

// Signature
HMACSHA256(
  base64UrlEncode(header) + "." + base64UrlEncode(payload),
  secret
)
```

### Token Security Measures

| Measure | Implementation |
|---------|----------------|
| Signature Verification | Every request at API Gateway |
| Expiration Check | 24-hour lifetime enforced |
| Secret Key Size | 256 bits (32 bytes) minimum |
| Transport Security | HTTPS recommended in production |

## 3. API Key Encryption (AES-256-GCM)

### Why AES-256-GCM

| Factor | AES-GCM | AES-CBC | ChaCha20 |
|--------|---------|---------|----------|
| Authentication | Built-in | Separate MAC needed | Built-in |
| Performance | Hardware acceleration | Hardware acceleration | Software only |
| Industry Standard | NIST approved | Legacy | Newer |
| IV Handling | 96-bit unique | 128-bit unique | 96-bit unique |

**Decision:** AES-256-GCM selected for authenticated encryption (confidentiality + integrity) and hardware acceleration support.

### Implementation

```java
@Service
public class EncryptionService {
    
    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12;  // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // bits
    
    private final SecretKey masterKey;
    
    public EncryptionService(@Value("${encryption.master-key}") String masterKeyBase64) {
        byte[] keyBytes = Base64.getDecoder().decode(masterKeyBase64);
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException("Master key must be 256 bits (32 bytes)");
        }
        this.masterKey = new SecretKeySpec(keyBytes, "AES");
    }
    
    public String encrypt(String plaintext) {
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, masterKey, spec);
        
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        
        // Prepend IV to ciphertext
        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
        
        return Base64.getEncoder().encodeToString(combined);
    }
    
    public String decrypt(String encryptedBase64) {
        byte[] combined = Base64.getDecoder().decode(encryptedBase64);
        
        byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH);
        byte[] ciphertext = Arrays.copyOfRange(combined, GCM_IV_LENGTH, combined.length);
        
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, masterKey, spec);
        
        byte[] plaintext = cipher.doFinal(ciphertext);
        return new String(plaintext, StandardCharsets.UTF_8);
    }
}
```

### Encryption Data Flow

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Plain API Key │────>│   Encrypt       │────>│  Database       │
│  "abc123xyz"    │     │   (AES-256-GCM) │     │  (encrypted)    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
                                                        │
                                                        v
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│   Plain API Key │<────│   Decrypt       │<────│  Database       │
│  "abc123xyz"    │     │   (AES-256-GCM) │     │  (encrypted)    │
└─────────────────┘     └─────────────────┘     └─────────────────┘
```

### Encrypted Data Format

```
Base64(IV || Ciphertext || AuthTag)
       │        │            │
       │        │            └── 128-bit authentication tag
       │        └── Encrypted data
       └── 96-bit random IV (unique per encryption)
```

## Key Management

### Environment Variables

```env
# JWT Secret (256 bits, Base64 encoded)
JWT_SECRET=your-base64-encoded-32-byte-key

# AES Master Key (256 bits, Base64 encoded)
ENCRYPTION_MASTER_KEY=your-base64-encoded-32-byte-key
```

### Key Generation

```bash
# Generate secure 256-bit key
openssl rand -base64 32

# Example output (use your own!)
# K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv72ol/pe/Unols=
```

### Key Rotation Strategy

| Component | Rotation Frequency | Method |
|-----------|-------------------|--------|
| JWT Secret | On compromise | Environment variable update, restart services |
| Master Key | On compromise | Re-encrypt all API keys with new key |
| API Provider Keys | Monthly/On compromise | Admin API endpoint |

## Security Summary

| Component | Algorithm | Key Size | Purpose |
|-----------|-----------|----------|---------|
| Password Storage | BCrypt | N/A (adaptive) | User authentication |
| Token Signing | HMAC-SHA256 | 256 bits | API authorization |
| API Key Storage | AES-256-GCM | 256 bits | Credential protection |

## Compliance

| Standard | Relevance |
|----------|-----------|
| OWASP | Password storage, token security guidelines followed |
| NIST SP 800-132 | BCrypt follows PBKDF recommendations |
| NIST SP 800-38D | AES-GCM authenticated encryption |
