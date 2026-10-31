package ar.unsam.vv.atalaya.config;

import ar.unsam.vv.atalaya.dominio.Rol;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * API con HTTP Basic y panel con form login. Permisos por operación según la matriz de la sección
 * 7.6 del informe (CP-35). Lo que no está en la lista se niega (sin credenciales → 401).
 *
 * <p>Los endpoints de /api/v1/qa/** no piden credenciales: solo existen con el perfil qa y así,
 * con el perfil prod, responden 404 y no 401 (CP-36).
 */
@Configuration
public class SeguridadConfig {

    /** Roles que pueden consultar magnitudes, estimaciones y alertas. */
    private static final String[] LECTORES = {
            Rol.OPERADOR_REACTOR.name(), Rol.JEFE_TURNO.name(), Rol.SUPERVISOR_SO.name(), Rol.AUDITOR.name()};

    /** Roles que pueden reconocer alertas. */
    private static final String[] RECONOCEDORES = {
            Rol.OPERADOR_REACTOR.name(), Rol.JEFE_TURNO.name(), Rol.SUPERVISOR_SO.name()};

    @Bean
    @Order(1)
    public SecurityFilterChain api(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/api/**")
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/qa/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/lecturas").hasRole(Rol.ADQUISICION.name())
                        .requestMatchers(HttpMethod.GET, "/api/v1/magnitudes/**", "/api/v1/estimaciones/**",
                                "/api/v1/alertas", "/api/v1/alertas/**", "/api/v1/config/umbrales").hasAnyRole(LECTORES)
                        .requestMatchers(HttpMethod.POST, "/api/v1/alertas/*/reconocimiento").hasAnyRole(RECONOCEDORES)
                        .requestMatchers(HttpMethod.POST, "/api/v1/maniobras").hasRole(Rol.JEFE_TURNO.name())
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/maniobras/*").hasRole(Rol.JEFE_TURNO.name())
                        .requestMatchers(HttpMethod.PUT, "/api/v1/config/umbrales").hasRole(Rol.SUPERVISOR_SO.name())
                        .requestMatchers(HttpMethod.POST, "/api/v1/config/umbrales/*/aprobacion")
                                .hasRole(Rol.JEFE_TURNO.name())
                        .requestMatchers(HttpMethod.GET, "/api/v1/version").authenticated()
                        .anyRequest().denyAll())
                .httpBasic(basic -> basic.authenticationEntryPoint(SeguridadConfig::noAutenticado))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(SeguridadConfig::noAutenticado)
                        .accessDeniedHandler((pedido, respuesta, ex) -> escribirError(respuesta, 403,
                                "SIN_PERMISO", "El rol no tiene permiso para esta operación")))
                .build();
    }

    /**
     * 401 escrito directamente, sin sendError. Con sendError el servidor reenvía a /error, que no está
     * bajo /api/** y lo atiende la cadena del panel: la API respondía 302 al login en vez de 401
     * (defecto encontrado al ejecutar CP-34 contra el servidor real; MockMvc no hace ese reenvío).
     */
    private static void noAutenticado(HttpServletRequest pedido, HttpServletResponse respuesta,
                                      AuthenticationException ex) throws IOException {
        respuesta.setHeader("WWW-Authenticate", "Basic realm=\"ATALAYA\"");
        escribirError(respuesta, 401, "NO_AUTENTICADO", "Faltan credenciales o son incorrectas");
    }

    private static void escribirError(HttpServletResponse respuesta, int estado, String codigo, String mensaje)
            throws IOException {
        respuesta.setStatus(estado);
        respuesta.setContentType("application/json");
        respuesta.setCharacterEncoding("UTF-8");
        respuesta.getWriter().write("{\"codigo\":\"" + codigo + "\",\"mensaje\":\"" + mensaje + "\",\"campo\":null}");
    }

    @Bean
    @Order(2)
    public SecurityFilterChain panel(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers("/actuator/health", "/login", "/error").permitAll()
                        .requestMatchers(HttpMethod.POST, "/panel/alertas/*/reconocer").hasAnyRole(RECONOCEDORES)
                        .requestMatchers("/", "/panel").hasAnyRole(LECTORES)
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/panel", true).permitAll())
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public UserDetailsService usuarios(UsuariosProperties props) {
        var manager = new InMemoryUserDetailsManager();
        props.usuarios().forEach(u -> manager.createUser(User.withUsername(u.nombre())
                .password(u.clave())
                .roles(u.rol().name())
                .build()));
        return manager;
    }
}
