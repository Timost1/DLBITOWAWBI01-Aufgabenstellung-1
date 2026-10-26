package de.iu.fallstudie.projektportal.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Erzwingt das Rendern des CSRF-Tokens in das Antwort-Cookie.
 *
 * <p>Spring Security laedt das CSRF-Token erst bei Bedarf ("deferred"). Da das
 * React-Frontend das Token aus dem Cookie {@code XSRF-TOKEN} liest, wird der
 * Token-Wert hier bei jeder Anfrage einmal abgerufen. Erst dieser Zugriff
 * veranlasst die {@code CookieCsrfTokenRepository}, das Cookie zu setzen.</p>
 */
class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (token != null) {
            token.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
