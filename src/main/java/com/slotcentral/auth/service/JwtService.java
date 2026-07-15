package com.slotcentral.auth.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.*;
import com.slotcentral.auth.domain.EmployeeRole;
import com.slotcentral.auth.repository.RevokedTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private final RSAKey rsaKey;
    private final RSASSASigner signer;
    private final RSASSAVerifier verifier;
    private final RevokedTokenRepository revokedTokenRepository;

    @Value("${auth.jwt.player-session-ttl-seconds:3600}")
    private long playerSessionTtlSeconds;

    @Value("${auth.jwt.employee-token-ttl-seconds:28800}")
    private long employeeTokenTtlSeconds;

    public JwtService(
            @Value("${auth.jwt.private-key-pem:}") String privateKeyPem,
            @Value("${auth.jwt.public-key-pem:}") String publicKeyPem,
            RevokedTokenRepository revokedTokenRepository) throws Exception {
        this.revokedTokenRepository = revokedTokenRepository;

        if (!privateKeyPem.isBlank() && !publicKeyPem.isBlank()) {
            log.info("Loading RSA key pair from configured PEM files");
            this.rsaKey = parseKeyPair(privateKeyPem, publicKeyPem);
        } else {
            log.info("Generating RSA key pair (dev mode - no PEM keys configured)");
            this.rsaKey = new RSAKeyGenerator(2048)
                    .keyID(UUID.randomUUID().toString())
                    .generate();
        }
        this.signer = new RSASSASigner(this.rsaKey);
        this.verifier = new RSASSAVerifier(this.rsaKey.toRSAPublicKey());
    }

    private RSAKey parseKeyPair(String privateKeyPem, String publicKeyPem) throws Exception {
        java.security.KeyFactory kf = java.security.KeyFactory.getInstance("RSA");

        String privPem = privateKeyPem
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] privBytes = java.util.Base64.getDecoder().decode(privPem);
        java.security.spec.PKCS8EncodedKeySpec privSpec = new java.security.spec.PKCS8EncodedKeySpec(privBytes);
        java.security.interfaces.RSAPrivateKey privateKey = (java.security.interfaces.RSAPrivateKey) kf.generatePrivate(privSpec);

        String pubPem = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
        byte[] pubBytes = java.util.Base64.getDecoder().decode(pubPem);
        java.security.spec.X509EncodedKeySpec pubSpec = new java.security.spec.X509EncodedKeySpec(pubBytes);
        java.security.interfaces.RSAPublicKey publicKey = (java.security.interfaces.RSAPublicKey) kf.generatePublic(pubSpec);

        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    public String issuePlayerToken(String uid) {
        return issueToken(uid, "player", null, playerSessionTtlSeconds);
    }

    public String issueEmployeeToken(String uid, EmployeeRole role) {
        return issueToken(uid, "employee", role, employeeTokenTtlSeconds);
    }

    private String issueToken(String subject, String type, EmployeeRole role, long ttlSeconds) {
        try {
            Instant now = Instant.now();
            Instant exp = now.plusSeconds(ttlSeconds);
            String jti = UUID.randomUUID().toString();

            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .subject(subject)
                    .issuer("slot-auth-service")
                    .issueTime(Date.from(now))
                    .expirationTime(Date.from(exp))
                    .jwtID(jti)
                    .claim("type", type);

            if (role != null) {
                claimsBuilder.claim("role", role.name());
            }

            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256)
                            .keyID(rsaKey.getKeyID())
                            .build(),
                    claimsBuilder.build()
            );
            jwt.sign(signer);
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new IllegalStateException("Failed to issue JWT", e);
        }
    }

    public JWTClaimsSet validateToken(String token) throws ParseException, JOSEException {
        SignedJWT jwt = SignedJWT.parse(token);
        if (!jwt.verify(verifier)) {
            throw new JOSEException("JWT signature verification failed");
        }
        JWTClaimsSet claims = jwt.getJWTClaimsSet();
        if (claims.getExpirationTime().before(new Date())) {
            throw new JOSEException("JWT token is expired");
        }
        String jti = claims.getJWTID();
        if (jti != null && revokedTokenRepository.existsByJti(jti)) {
            throw new JOSEException("JWT token has been revoked");
        }
        return claims;
    }

    public void revokeToken(String token) throws ParseException {
        SignedJWT jwt = SignedJWT.parse(token);
        JWTClaimsSet claims = jwt.getJWTClaimsSet();
        String jti = claims.getJWTID();
        if (jti == null) {
            log.warn("Revocation attempted on token without a JTI claim for subject '{}'; skipping", claims.getSubject());
            return;
        }

        com.slotcentral.auth.domain.RevokedToken revoked = new com.slotcentral.auth.domain.RevokedToken();
        revoked.setJti(jti);
        revoked.setSubject(claims.getSubject());
        revoked.setExpiresAt(claims.getExpirationTime().toInstant());
        revokedTokenRepository.save(revoked);
    }

    public JWKSet getJwks() {
        return new JWKSet(rsaKey.toPublicJWK());
    }

    public long getPlayerSessionTtlSeconds() {
        return playerSessionTtlSeconds;
    }

    public long getEmployeeTokenTtlSeconds() {
        return employeeTokenTtlSeconds;
    }
}
