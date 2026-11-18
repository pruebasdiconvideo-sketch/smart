package com.smartpoweron.gestion_materiales.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro JWT (RF01, RNF03). Lee el token de:
 *  - encabezado Authorization: Bearer &lt;token&gt;  (servicios REST), o
 *  - cookie HttpOnly `jwt`                       (vistas Thymeleaf, RF04).
 *
 * Si el token es válido y el usuario está activo, carga la authorities y
 * deja pasar la petición; si no, el SecurityConfig responde 401 o redirige.
 */
@Component
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UsuarioDetailsService userDetailsService;

    public JwtFilter(JwtUtil jwtUtil, UsuarioDetailsService userDetailsService) {
        this.jwtUtil = jwtUtil;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String token = extraerToken(req);
        if (token != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                String correo = jwtUtil.extractUsername(token);
                if (correo != null && jwtUtil.validateToken(token, correo)) {
                    UserDetails ud = userDetailsService.loadUserByUsername(correo);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(ud, null, ud.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            } catch (Exception ignored) {
                // token inválido o vencido: el SecurityConfig responderá 401/redirect
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(req, res);
    }

    private String extraerToken(HttpServletRequest req) {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }
        if (req.getCookies() != null) {
            for (Cookie c : req.getCookies()) {
                if ("jwt".equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }
}
