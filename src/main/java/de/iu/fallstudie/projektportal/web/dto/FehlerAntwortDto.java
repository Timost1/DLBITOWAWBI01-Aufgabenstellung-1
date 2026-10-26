package de.iu.fallstudie.projektportal.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

/**
 * Einheitliches Fehlerformat der REST-API.
 *
 * <p>Alle Fehlerantworten - Validierungsfehler, fachliche Regelverstoesse,
 * fehlende Rechte, unbekannte Ressourcen - verwenden dieselbe Struktur. Das
 * Frontend muss dadurch nur ein Format auswerten. Die Feldfehler sind nur bei
 * Validierungsfehlern gesetzt.</p>
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FehlerAntwortDto(
        Instant zeitpunkt,
        int status,
        String fehlercode,
        String meldung,
        Map<String, String> feldfehler
) {

    public static FehlerAntwortDto von(int status, String fehlercode, String meldung) {
        return new FehlerAntwortDto(Instant.now(), status, fehlercode, meldung, null);
    }

    public static FehlerAntwortDto mitFeldfehlern(int status, String fehlercode, String meldung,
                                                  Map<String, String> feldfehler) {
        return new FehlerAntwortDto(Instant.now(), status, fehlercode, meldung, feldfehler);
    }
}
