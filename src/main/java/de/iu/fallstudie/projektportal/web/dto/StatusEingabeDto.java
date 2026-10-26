package de.iu.fallstudie.projektportal.web.dto;

import de.iu.fallstudie.projektportal.domain.Aufgabenstatus;
import jakarta.validation.constraints.NotNull;

/**
 * Eingabe-DTO fuer den haeufigsten Vorgang im Arbeitsalltag: das Umsetzen des
 * Aufgabenstatus. Ein eigener, schmaler Endpunkt vermeidet, dass beim
 * Statuswechsel versehentlich andere Felder ueberschrieben werden.
 */
public record StatusEingabeDto(

        @NotNull(message = "Der Status ist anzugeben.")
        Aufgabenstatus status
) {
}
