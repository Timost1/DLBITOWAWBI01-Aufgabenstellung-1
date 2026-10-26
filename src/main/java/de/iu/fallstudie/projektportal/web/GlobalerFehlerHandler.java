package de.iu.fallstudie.projektportal.web;

import de.iu.fallstudie.projektportal.service.FachlicheRegelException;
import de.iu.fallstudie.projektportal.service.RessourceNichtGefundenException;
import de.iu.fallstudie.projektportal.service.ZugriffVerweigertException;
import de.iu.fallstudie.projektportal.web.dto.FehlerAntwortDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Globale Fehlerbehandlung fuer alle REST-Controller.
 *
 * <p>Die Klasse buendelt die Uebersetzung von Exceptions in HTTP-Antworten an
 * einer Stelle, sodass kein Controller eigene try/catch-Blöcke benoetigt. Alle
 * Antworten nutzen dasselbe JSON-Schema ({@link FehlerAntwortDto}).</p>
 *
 * <p>Bewusst wird nach aussen nur eine fachliche Meldung gegeben. Technische
 * Details wie Stacktraces oder SQL-Fehlermeldungen bleiben im Serverlog; ihre
 * Auslieferung waere ein Informationsleck, das Angreifenden Rueckschluesse auf
 * die interne Struktur erlaubt.</p>
 */
@RestControllerAdvice
public class GlobalerFehlerHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalerFehlerHandler.class);

    /**
     * Verstoesse gegen Bean-Validation-Constraints. Die Feldfehler werden als
     * Map ausgeliefert, damit das Frontend sie direkt am jeweiligen
     * Eingabefeld anzeigen kann.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<FehlerAntwortDto> behandleValidierungsfehler(
            MethodArgumentNotValidException ausnahme) {
        Map<String, String> feldfehler = new LinkedHashMap<>();
        for (FieldError fehler : ausnahme.getBindingResult().getFieldErrors()) {
            feldfehler.putIfAbsent(fehler.getField(), fehler.getDefaultMessage());
        }
        ausnahme.getBindingResult().getGlobalErrors().forEach(fehler ->
                feldfehler.putIfAbsent(fehler.getObjectName(), fehler.getDefaultMessage()));

        return ResponseEntity.badRequest().body(FehlerAntwortDto.mitFeldfehlern(
                HttpStatus.BAD_REQUEST.value(),
                "VALIDIERUNG",
                "Die Eingaben sind unvollständig oder ungültig.",
                feldfehler));
    }

    /** Syntaktisch fehlerhafter oder nicht deserialisierbarer Request-Body. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<FehlerAntwortDto> behandleUnlesbarenRequest(
            HttpMessageNotReadableException ausnahme) {
        LOG.debug("Request-Body konnte nicht gelesen werden: {}", ausnahme.getMessage());
        return ResponseEntity.badRequest().body(FehlerAntwortDto.von(
                HttpStatus.BAD_REQUEST.value(),
                "UNLESBARE_ANFRAGE",
                "Die Anfrage konnte nicht verarbeitet werden. "
                        + "Bitte prüfen Sie die übergebenen Werte."));
    }

    /** Nicht konvertierbarer Pfad- oder Query-Parameter, z. B. Text statt Zahl. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<FehlerAntwortDto> behandleTypfehler(
            MethodArgumentTypeMismatchException ausnahme) {
        return ResponseEntity.badRequest().body(FehlerAntwortDto.von(
                HttpStatus.BAD_REQUEST.value(),
                "UNGUELTIGER_PARAMETER",
                "Der Parameter \"" + ausnahme.getName() + "\" hat kein gültiges Format."));
    }

    @ExceptionHandler(RessourceNichtGefundenException.class)
    public ResponseEntity<FehlerAntwortDto> behandleNichtGefunden(
            RessourceNichtGefundenException ausnahme) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(FehlerAntwortDto.von(
                HttpStatus.NOT_FOUND.value(), "NICHT_GEFUNDEN", ausnahme.getMessage()));
    }

    @ExceptionHandler(FachlicheRegelException.class)
    public ResponseEntity<FehlerAntwortDto> behandleFachlicheRegel(
            FachlicheRegelException ausnahme) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(FehlerAntwortDto.von(
                HttpStatus.CONFLICT.value(), "REGELVERSTOSS", ausnahme.getMessage()));
    }

    /**
     * Fehlende Berechtigung - sowohl aus der eigenen Zugriffskontrolle als auch
     * aus den {@code @PreAuthorize}-Pruefungen von Spring Security.
     */
    @ExceptionHandler({ZugriffVerweigertException.class, AccessDeniedException.class})
    public ResponseEntity<FehlerAntwortDto> behandleZugriffVerweigert(RuntimeException ausnahme) {
        String meldung = ausnahme instanceof ZugriffVerweigertException
                ? ausnahme.getMessage()
                : "Für diese Aktion fehlt die erforderliche Berechtigung.";
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(FehlerAntwortDto.von(
                HttpStatus.FORBIDDEN.value(), "KEINE_BERECHTIGUNG", meldung));
    }

    /** Die Ressource existiert, das verwendete HTTP-Verb ist dort aber nicht vorgesehen. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<FehlerAntwortDto> behandleFalscheMethode(
            HttpRequestMethodNotSupportedException ausnahme) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(FehlerAntwortDto.von(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                "METHODE_NICHT_ERLAUBT",
                "Die Methode " + ausnahme.getMethod()
                        + " ist für diese Ressource nicht vorgesehen."));
    }

    /** Verletzte Datenbankbedingung, z. B. eine Unique-Constraint. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<FehlerAntwortDto> behandleIntegritaetsfehler(
            DataIntegrityViolationException ausnahme) {
        LOG.warn("Datenbankbedingung verletzt: {}", ausnahme.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(FehlerAntwortDto.von(
                HttpStatus.CONFLICT.value(),
                "DATENKONFLIKT",
                "Der Datensatz widerspricht einer bestehenden Regel der Datenbank."));
    }

    /**
     * Auffangnetz fuer alle uebrigen Fehler.
     *
     * <p>Spring signalisiert eigene HTTP-Fehler - etwa einen unbekannten Pfad -
     * ueber das Interface {@code ErrorResponse} und bringt den passenden
     * Statuscode selbst mit. Solche Faelle werden hier erkannt und behalten
     * ihren Status; andernfalls wuerde aus einem einfachen Tippfehler in der URL
     * ein gemeldeter Serverfehler.</p>
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<FehlerAntwortDto> behandleUnerwartet(Exception ausnahme) {
        if (ausnahme instanceof ErrorResponse fehlerantwort) {
            int status = fehlerantwort.getStatusCode().value();
            LOG.debug("Von Spring gemeldeter HTTP-Fehler {}: {}", status, ausnahme.getMessage());
            boolean nichtGefunden = status == HttpStatus.NOT_FOUND.value();
            return ResponseEntity.status(status).body(FehlerAntwortDto.von(
                    status,
                    nichtGefunden ? "NICHT_GEFUNDEN" : "ANFRAGEFEHLER",
                    nichtGefunden
                            ? "Die angeforderte Adresse existiert nicht."
                            : "Die Anfrage konnte nicht verarbeitet werden."));
        }

        LOG.error("Unerwarteter Serverfehler", ausnahme);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(FehlerAntwortDto.von(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "SERVERFEHLER",
                "Es ist ein unerwarteter Fehler aufgetreten. "
                        + "Bitte versuchen Sie es später erneut."));
    }
}
