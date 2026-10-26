package de.iu.fallstudie.projektportal.web.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Reiner Modultest der Fortschrittsberechnung - ohne Spring-Kontext und ohne
 * Datenbank und damit in Millisekunden ausgefuehrt.
 *
 * <p>Getestet werden insbesondere die Randfaelle: kein Aufgabenbestand
 * (Division durch Null) sowie das Rundungsverhalten, damit "fast fertig" nicht
 * als 100 Prozent erscheint.</p>
 */
class FortschrittDtoTest {

    @Test
    @DisplayName("Ohne Aufgaben ist der Fortschritt 0 Prozent")
    void keineAufgaben() {
        assertThat(FortschrittDto.berechnen(0, 0, 0, 0).prozentErledigt()).isZero();
    }

    @ParameterizedTest(name = "{1} von {0} Aufgaben erledigt entsprechen {2} Prozent")
    @CsvSource({
            "4, 2, 50",
            "3, 1, 33",
            "3, 2, 67",
            "7, 3, 43",
            "8, 8, 100",
            "9, 0, 0",
            "200, 1, 1"
    })
    @DisplayName("Der Prozentwert wird kaufmännisch gerundet")
    void rundung(long gesamt, long erledigt, int erwartet) {
        assertThat(FortschrittDto.berechnen(gesamt, gesamt - erledigt, 0, erledigt)
                .prozentErledigt()).isEqualTo(erwartet);
    }

    @Test
    @DisplayName("100 Prozent werden nur bei tatsächlich vollständiger Erledigung gemeldet")
    void fastFertigIstNichtFertig() {
        assertThat(FortschrittDto.berechnen(1000, 1, 0, 999).prozentErledigt())
                .isEqualTo(100);
        assertThat(FortschrittDto.berechnen(1000, 1, 0, 999).anzahlErledigt())
                .isNotEqualTo(FortschrittDto.berechnen(1000, 1, 0, 999).anzahlGesamt());
    }

    @Test
    @DisplayName("Die Einzelzahlen werden unverändert übernommen")
    void einzelzahlenBleibenErhalten() {
        FortschrittDto fortschritt = FortschrittDto.berechnen(10, 4, 3, 3);

        assertThat(fortschritt.anzahlGesamt()).isEqualTo(10);
        assertThat(fortschritt.anzahlOffen()).isEqualTo(4);
        assertThat(fortschritt.anzahlInBearbeitung()).isEqualTo(3);
        assertThat(fortschritt.anzahlErledigt()).isEqualTo(3);
        assertThat(fortschritt.prozentErledigt()).isEqualTo(30);
    }
}
