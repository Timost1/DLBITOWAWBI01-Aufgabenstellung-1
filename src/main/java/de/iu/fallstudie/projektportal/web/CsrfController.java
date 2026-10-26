package de.iu.fallstudie.projektportal.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stellt ein frisches CSRF-Token bereit.
 *
 * <p>Hintergrund: Beim Abmelden verwirft Spring Security das bestehende
 * CSRF-Token und loescht das zugehoerige Cookie. Eine erneute Anmeldung in
 * derselben geladenen Seite haette danach kein Token mehr zur Hand und wuerde
 * abgewiesen. Das Frontend ruft diesen Endpunkt deshalb auf, wenn kein Token
 * vorliegt; der {@code CsrfCookieFilter} setzt daraufhin ein neues Cookie.</p>
 *
 * <p>Der Endpunkt ist ohne Anmeldung erreichbar. Das ist unkritisch, da das
 * Token an die jeweilige Sitzung gebunden ist und ohnehin nur vor
 * seitenuebergreifenden Anfragen schuetzt - nicht vor dem Auslesen durch die
 * eigene Anwendung.</p>
 */
@RestController
public class CsrfController {

    @GetMapping("/api/csrf")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void token() {
        // Kein Rueckgabewert notwendig: Das Token liegt im Antwort-Cookie.
    }
}
