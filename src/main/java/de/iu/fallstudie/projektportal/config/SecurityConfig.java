package de.iu.fallstudie.projektportal.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Zentrale Sicherheitskonfiguration.
 *
 * <p>Die Konfiguration folgt dem im Lernskript beschriebenen Muster
 * (pfadbasierte Autorisierungsregeln plus HTTP Basic als Anmeldeverfahren),
 * ergaenzt es aber um drei Punkte, die eine Single-Page-Anwendung benoetigt:</p>
 * <ol>
 *   <li>Der nach der Basic-Anmeldung entstandene Sicherheitskontext wird in der
 *       HTTP-Session abgelegt. Das Kennwort wird damit nur einmal beim Login
 *       uebertragen und muss nicht im Browser vorgehalten werden.</li>
 *   <li>Ueber {@code /api/auth/logout} kann die Session serverseitig
 *       invalidiert werden - eine echte Abmeldung, die HTTP Basic allein nicht
 *       bietet.</li>
 *   <li>Weil damit ein Sitzungscookie im Spiel ist, bleibt der CSRF-Schutz
 *       aktiv. Das Token wird im Cookie {@code XSRF-TOKEN} ausgeliefert und vom
 *       Frontend im Header {@code X-XSRF-TOKEN} zurueckgesendet.</li>
 * </ol>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Kennwoerter werden mit BCrypt gehasht. BCrypt ist ein absichtlich
     * langsamer Algorithmus mit Zufallssalz und damit gegen Rainbow-Table- und
     * Brute-Force-Angriffe deutlich robuster als einfache Hashfunktionen.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        SecurityContextRepository kontextSpeicher = new HttpSessionSecurityContextRepository();

        CsrfTokenRequestAttributeHandler csrfHandler = new CsrfTokenRequestAttributeHandler();
        csrfHandler.setCsrfRequestAttributeName(null);

        http
                .securityContext(kontext -> kontext.securityContextRepository(kontextSpeicher))
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .csrfTokenRequestHandler(csrfHandler))
                .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // Statische Auslieferung der Single-Page-Anwendung
                        .requestMatchers("/", "/index.html", "/style.css",
                                "/js/**", "/vendor/**", "/favicon.ico").permitAll()
                        // Ausgabe eines frischen CSRF-Tokens vor der Anmeldung
                        .requestMatchers("/api/csrf").permitAll()
                        // Benutzerverwaltung ist ausschliesslich Administratoren zugaenglich
                        .requestMatchers("/api/benutzer/**").hasRole("ADMIN")
                        // Alles Uebrige erfordert eine Anmeldung
                        .anyRequest().authenticated())
                .httpBasic(basic -> basic
                        .securityContextRepository(kontextSpeicher)
                        .authenticationEntryPoint(jsonEinstiegspunkt()))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(jsonEinstiegspunkt()))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((req, res, auth) ->
                                res.setStatus(HttpServletResponse.SC_NO_CONTENT)));

        return http.build();
    }

    /**
     * Liefert bei fehlender Anmeldung eine schlichte 401-Antwort ohne
     * {@code WWW-Authenticate}-Header. Andernfalls wuerde der Browser seinen
     * eigenen Anmeldedialog oeffnen und das Login-Formular der Anwendung
     * umgehen.
     */
    private AuthenticationEntryPoint jsonEinstiegspunkt() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"meldung\":\"Anmeldung erforderlich.\"}");
        };
    }
}
