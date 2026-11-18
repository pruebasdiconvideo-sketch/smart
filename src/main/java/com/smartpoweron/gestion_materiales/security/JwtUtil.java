package com.smartpoweron.gestion_materiales.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

/**
 * Utilitario de generación y validación de tokens JWT firmados con HS256
 * (RF01, RNF03). La clave se toma de application.properties y debe tener al
 * menos 256 bits para HS256.
 */
@Component
public class JwtUtil {

    @Value("${app.jwt.secreto}")
    private String secreto;

    @Value("${app.jwt.expiracion-minutos:480}")
    private long expiracionMinutos;

    private Key key;

    @PostConstruct
    void init() {
        // Si la clave configurada es corta, generamos una clave segura derivada
        // para garantizar HS256 en cualquier entorno (RNF03).
        byte[] bytes = secreto.getBytes();
        if (bytes.length < 32) {
            this.key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        } else {
            this.key = Keys.hmacShaKeyFor(bytes);
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractRol(String token) {
        return extractClaim(token, c -> c.get("rol", String.class));
    }

    public Long extractIdUsuario(String token) {
        return extractClaim(token, c -> c.get("idUsuario", Long.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        return resolver.apply(claims);
    }

    public String generateToken(Long idUsuario, String correo, String rol) {
        long now = System.currentTimeMillis();
        long exp = now + expiracionMinutos * 60_000L;
        return Jwts.builder()
                .setSubject(correo)
                .claim("rol", rol)
                .claim("idUsuario", idUsuario)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(exp))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token, String correo) {
        try {
            String user = extractUsername(token);
            return user != null && user.equals(correo) && !isExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isExpired(String token) {
        Date exp = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody().getExpiration();
        return exp.before(new Date());
    }
}
