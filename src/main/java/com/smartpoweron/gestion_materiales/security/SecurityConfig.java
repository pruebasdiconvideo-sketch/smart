package com.smartpoweron.gestion_materiales.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;
import java.util.Map;

/**
 * Configuración de seguridad (RF02, RNF03, RNF04, §7.6).
 *
 * Tres capas:
 *   1) Filtro JWT: exige token válido en toda ruta /api (salvo /api/auth/login)
 *      y en toda vista (salvo /login y estáticos).
 *   2) Autorización por rol: cada endpoint y cada vista se limitan a los roles
 *      de la Tabla 70 (servicios API) y de la Tabla 69 (vistas) del informe.
 *   3) Stateless: la sesión se cierra por vencimiento del JWT (RF04, PA-03).
 *
 * El token se envía en cookie HttpOnly para las vistas y en Authorization para
 * los servicios REST. Las contraseñas se cifran con BCrypt (RNF04).
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())                 // API stateless con JWT
            .cors(cors -> cors.configurationSource(corsSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth

                // --- Recursos públicos ---
                .requestMatchers(HttpMethod.GET,
                        "/login", "/", "/css/**", "/js/**", "/images/**",
                        "/webjars/**", "/favicon.ico").permitAll()
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/h2-console/**").permitAll()

                // --- A0 · Acceso (Tabla 70) ---
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                .requestMatchers(HttpMethod.GET,  "/api/admin/usuarios").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/admin/usuarios").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,  "/api/admin/usuarios/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH,"/api/admin/usuarios/**/desactivar").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH,"/api/admin/usuarios/**/reactivar").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET,  "/api/admin/parametros-prueba").hasAnyRole("ADMIN","INSPECTOR")
                .requestMatchers(HttpMethod.POST, "/api/admin/parametros-prueba").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT,  "/api/admin/parametros-prueba/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/admin/parametros-prueba/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/admin/parametros-prueba/**/reactivar").hasRole("ADMIN")

                // --- M1 · Inventario y catálogo ---
                .requestMatchers(HttpMethod.GET,  "/api/almacen/materiales").hasAnyRole("ADMIN","ALMACENERO","JEFE_PRODUCCION","OPERARIO_ENSAMBLE")
                .requestMatchers(HttpMethod.POST, "/api/almacen/materiales").hasAnyRole("ALMACENERO","ADMIN")
                .requestMatchers(HttpMethod.PUT,  "/api/almacen/materiales/**").hasAnyRole("ALMACENERO","ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/almacen/materiales/**").hasAnyRole("ALMACENERO","ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/almacen/materiales/**/reactivar").hasAnyRole("ALMACENERO","ADMIN")
                .requestMatchers(HttpMethod.GET,  "/api/almacen/ordenes-compra").hasAnyRole("ALMACENERO","ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/almacen/ordenes-compra").hasAnyRole("ALMACENERO","ADMIN")
                .requestMatchers(HttpMethod.PUT,  "/api/almacen/ordenes-compra/**").hasAnyRole("ALMACENERO","ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/almacen/ordenes-compra/**").hasAnyRole("ALMACENERO","ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/almacen/ordenes-compra/**/reabrir").hasAnyRole("ALMACENERO","ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/almacen/ingresos").hasRole("ALMACENERO")
                .requestMatchers(HttpMethod.GET,  "/api/almacen/alertas-stock").hasAnyRole("ALMACENERO","ADMIN")
                .requestMatchers(HttpMethod.GET,  "/api/almacen/categorias").hasAnyRole("ADMIN","ALMACENERO","JEFE_PRODUCCION","OPERARIO_ENSAMBLE")
                .requestMatchers(HttpMethod.GET,  "/api/almacen/categorias-lista").hasAnyRole("ADMIN","ALMACENERO","JEFE_PRODUCCION","OPERARIO_ENSAMBLE")
                .requestMatchers(HttpMethod.GET,  "/api/almacen/instancias").hasAnyRole("ADMIN","ALMACENERO","JEFE_PRODUCCION","OPERARIO_ENSAMBLE")
                // --- Empresas (proveedores y clientes) ---
                .requestMatchers(HttpMethod.GET,  "/api/empresa").hasAnyRole("ADMIN","ALMACENERO","JEFE_PRODUCCION","OPERARIO_ENSAMBLE")
                .requestMatchers(HttpMethod.GET,  "/api/empresa/**").hasAnyRole("ADMIN","ALMACENERO","JEFE_PRODUCCION","OPERARIO_ENSAMBLE")
                .requestMatchers(HttpMethod.POST, "/api/empresa").hasAnyRole("ALMACENERO","ADMIN","JEFE_PRODUCCION")
                .requestMatchers(HttpMethod.PUT,  "/api/empresa/**").hasAnyRole("ALMACENERO","ADMIN","JEFE_PRODUCCION")
                .requestMatchers(HttpMethod.PATCH, "/api/empresa/**/desactivar").hasAnyRole("ALMACENERO","ADMIN","JEFE_PRODUCCION")
                .requestMatchers(HttpMethod.PATCH, "/api/empresa/**/reactivar").hasAnyRole("ALMACENERO","ADMIN","JEFE_PRODUCCION")
                .requestMatchers(HttpMethod.GET,  "/api/almacen/materiales/siguiente-codigo").hasAnyRole("ALMACENERO","ADMIN")

                // --- M2 · Producción / Solicitudes ---
                .requestMatchers(HttpMethod.GET,  "/api/produccion/proyectos").hasAnyRole("JEFE_PRODUCCION","ADMIN","INSPECTOR","OPERARIO_ENSAMBLE","ALMACENERO")
                .requestMatchers(HttpMethod.POST, "/api/produccion/proyectos").hasAnyRole("JEFE_PRODUCCION","ADMIN")
                .requestMatchers(HttpMethod.PUT,  "/api/produccion/proyectos/**").hasAnyRole("JEFE_PRODUCCION","ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/produccion/proyectos/**").hasAnyRole("JEFE_PRODUCCION","ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/produccion/solicitudes").hasRole("JEFE_PRODUCCION")
                .requestMatchers(HttpMethod.GET,  "/api/produccion/solicitudes").hasAnyRole("JEFE_PRODUCCION","ADMIN")
                .requestMatchers(HttpMethod.PUT,  "/api/produccion/solicitudes/**").hasAnyRole("JEFE_PRODUCCION","ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/produccion/solicitudes/**").hasAnyRole("JEFE_PRODUCCION","ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/produccion/solicitudes/*/aprobar").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/produccion/solicitudes/*/rechazar").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/produccion/solicitudes/*/verificar-stock").hasRole("JEFE_PRODUCCION")
                .requestMatchers(HttpMethod.GET,  "/api/produccion/solicitudes/*/faltantes").hasRole("JEFE_PRODUCCION")

                // --- M3 · Despacho ---
                .requestMatchers(HttpMethod.GET,  "/api/despacho/bandeja").hasRole("ALMACENERO")
                .requestMatchers(HttpMethod.GET,  "/api/despacho/validar").hasRole("ALMACENERO")
                .requestMatchers(HttpMethod.POST, "/api/despacho").hasRole("ALMACENERO")
                .requestMatchers(HttpMethod.GET, "/api/despacho/*").hasAnyRole("ALMACENERO","ADMIN","OPERARIO_ENSAMBLE")
                .requestMatchers(HttpMethod.PUT, "/api/despacho/**").hasAnyRole("ALMACENERO","ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/despacho/**").hasAnyRole("ALMACENERO","ADMIN","OPERARIO_ENSAMBLE")
                .requestMatchers(HttpMethod.GET,  "/api/despacho/pendientes-confirmacion").hasAnyRole("OPERARIO_ENSAMBLE","ALMACENERO","ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/despacho/*/confirmar").hasRole("OPERARIO_ENSAMBLE")

                // --- M4 · Calidad ---
                .requestMatchers(HttpMethod.POST, "/api/calidad/pruebas").hasRole("INSPECTOR")
                .requestMatchers(HttpMethod.GET,  "/api/calidad/pruebas").hasAnyRole("INSPECTOR","JEFE_PRODUCCION","ADMIN")
                .requestMatchers(HttpMethod.PUT,  "/api/calidad/pruebas/**").hasAnyRole("INSPECTOR","ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/calidad/pruebas/**").hasAnyRole("INSPECTOR","ADMIN")
                .requestMatchers(HttpMethod.GET,  "/api/calidad/proyectos/*/certificado.pdf").hasAnyRole("INSPECTOR","ADMIN")

                // --- M5 · Mermas y devoluciones ---
                .requestMatchers(HttpMethod.POST, "/api/merma/devoluciones").hasRole("OPERARIO_ENSAMBLE")
                .requestMatchers(HttpMethod.GET,  "/api/merma/devoluciones").hasAnyRole("OPERARIO_ENSAMBLE","ALMACENERO","JEFE_PRODUCCION")
                .requestMatchers(HttpMethod.PUT,  "/api/merma/devoluciones/**").hasAnyRole("OPERARIO_ENSAMBLE","ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/merma/devoluciones/**").hasAnyRole("OPERARIO_ENSAMBLE","ALMACENERO","JEFE_PRODUCCION","ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/merma/devoluciones/*/reintegrar").hasRole("ALMACENERO")
                .requestMatchers(HttpMethod.POST, "/api/merma/devoluciones/*/marcar-merma").hasRole("ALMACENERO")
                .requestMatchers(HttpMethod.POST, "/api/merma/devoluciones/*/validar").hasRole("JEFE_PRODUCCION")
                .requestMatchers(HttpMethod.GET,  "/api/merma/reporte").hasRole("JEFE_PRODUCCION")

                // --- M6 · Trazabilidad ---
                .requestMatchers(HttpMethod.GET,  "/api/trazabilidad/materiales/**").hasAnyRole("JEFE_PRODUCCION","ADMIN")

                // --- Vistas Thymeleaf (Tabla 69) ---
                .requestMatchers(HttpMethod.GET, "/inicio").authenticated()
                .requestMatchers(HttpMethod.GET, "/almacen/catalogo").hasAnyRole("ALMACENERO","ADMIN","JEFE_PRODUCCION")
                .requestMatchers(HttpMethod.GET, "/almacen/ingresos").hasAnyRole("ALMACENERO","ADMIN")
                .requestMatchers(HttpMethod.GET, "/produccion/proyectos").hasAnyRole("JEFE_PRODUCCION","ADMIN")
                .requestMatchers(HttpMethod.GET, "/produccion/solicitudes").hasAnyRole("JEFE_PRODUCCION","ADMIN")
                .requestMatchers(HttpMethod.GET, "/produccion/aprobaciones").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/despacho/bandeja").hasRole("ALMACENERO")
                .requestMatchers(HttpMethod.GET, "/despacho/preparar").hasRole("ALMACENERO")
                .requestMatchers(HttpMethod.GET, "/despacho/confirmar").hasRole("OPERARIO_ENSAMBLE")
                .requestMatchers(HttpMethod.GET, "/calidad/pruebas").hasAnyRole("INSPECTOR","JEFE_PRODUCCION","ADMIN")
                .requestMatchers(HttpMethod.GET, "/merma/devoluciones").hasAnyRole("OPERARIO_ENSAMBLE","ALMACENERO","JEFE_PRODUCCION")
                .requestMatchers(HttpMethod.GET, "/merma/reporte").hasRole("JEFE_PRODUCCION")
                .requestMatchers(HttpMethod.GET, "/trazabilidad").hasAnyRole("JEFE_PRODUCCION","ADMIN")
                .requestMatchers(HttpMethod.GET, "/admin/usuarios").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/admin/parametros").hasRole("ADMIN")

                .anyRequest().authenticated()
            )
            .exceptionHandling(eh -> eh
                .authenticationEntryPoint((req, res, ex) -> {
                    String path = req.getRequestURI();
                    if (path.startsWith("/api/")) {
                        res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        res.setCharacterEncoding("UTF-8");
                        res.getWriter().write(new ObjectMapper().writeValueAsString(
                                Map.of("error", "No autenticado", "mensaje", "Debe iniciar sesión para acceder.")));
                    } else {
                        res.sendRedirect("/login?error=sesion");
                    }
                })
                .accessDeniedHandler((req, res, ex) -> {
                    String path = req.getRequestURI();
                    if (path.startsWith("/api/")) {
                        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                        res.setCharacterEncoding("UTF-8");
                        res.getWriter().write(new ObjectMapper().writeValueAsString(
                                Map.of("error", "Acceso denegado", "mensaje", "Su rol no tiene permiso para esta operación.")));
                    } else {
                        res.sendRedirect("/inicio?error=rol");
                    }
                })
            )
            .headers(h -> h.frameOptions(f -> f.disable()))   // H2 console
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }

    private CorsConfigurationSource corsSource() {
        CorsConfiguration c = new CorsConfiguration();
        c.setAllowedOriginPatterns(List.of("*"));
        c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("*"));
        c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", c);
        return src;
    }
}
