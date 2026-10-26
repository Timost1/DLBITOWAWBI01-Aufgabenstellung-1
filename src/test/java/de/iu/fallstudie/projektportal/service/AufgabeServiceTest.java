package de.iu.fallstudie.projektportal.service;

import de.iu.fallstudie.projektportal.Integrationstest;
import de.iu.fallstudie.projektportal.domain.Aufgabe;
import de.iu.fallstudie.projektportal.domain.Aufgabenstatus;
import de.iu.fallstudie.projektportal.domain.Benutzer;
import de.iu.fallstudie.projektportal.domain.Mandant;
import de.iu.fallstudie.projektportal.domain.Projekt;
import de.iu.fallstudie.projektportal.domain.Rolle;
import de.iu.fallstudie.projektportal.web.dto.AufgabeDto;
import de.iu.fallstudie.projektportal.web.dto.AufgabeEingabeDto;
import de.iu.fallstudie.projektportal.web.dto.StatusEingabeDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests der Aufgabenlogik: Statuswechsel, Zuordnung von Bearbeitenden und die
 * Frage, wer an einem Projekt ueberhaupt arbeiten darf.
 */
class AufgabeServiceTest extends Integrationstest {

    @Autowired
    private AufgabeService aufgabeService;

    @Autowired
    private ProjektService projektService;

    @Test
    @DisplayName("Zugeordnete Mitarbeitende dürfen Aufgaben anlegen und den Status setzen")
    void mitarbeitendeArbeitenAmProjekt() {
        Mandant mandant = mandant("alpha");
        Benutzer anna = benutzer(mandant, "anna@alpha.test", Rolle.PROJEKTLEITER);
        Benutzer lea = benutzer(mandant, "lea@alpha.test", Rolle.MITARBEITER);
        Projekt projekt = projekt(mandant, "CRM", anna, lea);

        AufgabeDto angelegt = aufgabeService.anlegen(angemeldet(lea), projekt.getId(),
                new AufgabeEingabeDto("Feldmapping abstimmen", "Abgleich der Felder",
                        Aufgabenstatus.OFFEN, lea.getId(), null));

        assertThat(angelegt.status()).isEqualTo(Aufgabenstatus.OFFEN);
        assertThat(angelegt.bearbeiter().id()).isEqualTo(lea.getId());

        AufgabeDto erledigt = aufgabeService.statusAendern(angemeldet(lea), angelegt.id(),
                new StatusEingabeDto(Aufgabenstatus.ERLEDIGT));

        assertThat(erledigt.status()).isEqualTo(Aufgabenstatus.ERLEDIGT);
        assertThat(projektService.berechneFortschritt(projekt.getId()).prozentErledigt())
                .isEqualTo(100);
    }

    @Test
    @DisplayName("Nicht zugeordnete Mitarbeitende erreichen die Aufgaben nicht")
    void fremdeMitarbeitendeHabenKeinenZugriff() {
        Mandant mandant = mandant("alpha");
        Benutzer anna = benutzer(mandant, "anna@alpha.test", Rolle.PROJEKTLEITER);
        Benutzer aussenstehend = benutzer(mandant, "tim@alpha.test", Rolle.MITARBEITER);
        Projekt projekt = projekt(mandant, "CRM", anna);

        assertThatThrownBy(() -> aufgabeService.anlegen(angemeldet(aussenstehend),
                projekt.getId(),
                new AufgabeEingabeDto("Fremde Aufgabe", null, Aufgabenstatus.OFFEN, null, null)))
                .isInstanceOf(RessourceNichtGefundenException.class);
    }

    @Test
    @DisplayName("Aufgaben können nur an Projektbeteiligte vergeben werden")
    void bearbeiterMussProjektbeteiligtSein() {
        Mandant mandant = mandant("alpha");
        Benutzer anna = benutzer(mandant, "anna@alpha.test", Rolle.PROJEKTLEITER);
        Benutzer aussenstehend = benutzer(mandant, "tim@alpha.test", Rolle.MITARBEITER);
        Projekt projekt = projekt(mandant, "CRM", anna);

        assertThatThrownBy(() -> aufgabeService.anlegen(angemeldet(anna), projekt.getId(),
                new AufgabeEingabeDto("Aufgabe", null, Aufgabenstatus.OFFEN,
                        aussenstehend.getId(), null)))
                .isInstanceOf(FachlicheRegelException.class)
                .hasMessageContaining("zugeordnet");
    }

    @Test
    @DisplayName("In einem archivierten Projekt sind keine Änderungen möglich")
    void archiviertesProjektErlaubtKeineAufgabenaenderung() {
        Mandant mandant = mandant("alpha");
        Benutzer anna = benutzer(mandant, "anna@alpha.test", Rolle.PROJEKTLEITER);
        Projekt projekt = projekt(mandant, "Altprojekt", anna);
        Aufgabe aufgabe = aufgabe(projekt, "Altaufgabe", Aufgabenstatus.OFFEN);
        projektService.archivieren(angemeldet(anna), projekt.getId());

        assertThatThrownBy(() -> aufgabeService.statusAendern(angemeldet(anna), aufgabe.getId(),
                new StatusEingabeDto(Aufgabenstatus.ERLEDIGT)))
                .isInstanceOf(FachlicheRegelException.class);

        // Lesen bleibt erlaubt: Die Projekthistorie soll nachvollziehbar bleiben.
        assertThat(aufgabeService.findeAufgabenZuProjekt(angemeldet(anna), projekt.getId()))
                .hasSize(1);
    }

    @Test
    @DisplayName("Aufgaben eines fremden Mandanten sind nicht adressierbar")
    void mandantentrennungBeiAufgaben() {
        Mandant alpha = mandant("alpha");
        Mandant beta = mandant("beta");
        Benutzer annaAlpha = benutzer(alpha, "anna@alpha.test", Rolle.ADMIN);
        Benutzer leitungBeta = benutzer(beta, "leitung@beta.test", Rolle.PROJEKTLEITER);
        Projekt fremdesProjekt = projekt(beta, "Fremd", leitungBeta);
        Aufgabe fremdeAufgabe = aufgabe(fremdesProjekt, "Fremd", Aufgabenstatus.OFFEN);

        assertThatThrownBy(() -> aufgabeService.statusAendern(angemeldet(annaAlpha),
                fremdeAufgabe.getId(), new StatusEingabeDto(Aufgabenstatus.ERLEDIGT)))
                .isInstanceOf(RessourceNichtGefundenException.class);
    }

    @Test
    @DisplayName("Eine gelöschte Aufgabe verschwindet aus dem Projekt und aus dem Fortschritt")
    void aufgabeLoeschen() {
        Mandant mandant = mandant("alpha");
        Benutzer anna = benutzer(mandant, "anna@alpha.test", Rolle.PROJEKTLEITER);
        Projekt projekt = projekt(mandant, "CRM", anna);
        Aufgabe bleibt = aufgabe(projekt, "Bleibt", Aufgabenstatus.ERLEDIGT);
        Aufgabe geht = aufgabe(projekt, "Geht", Aufgabenstatus.OFFEN);

        aufgabeService.loeschen(angemeldet(anna), geht.getId());

        assertThat(aufgabeService.findeAufgabenZuProjekt(angemeldet(anna), projekt.getId()))
                .extracting(AufgabeDto::id)
                .containsExactly(bleibt.getId());
        assertThat(projektService.berechneFortschritt(projekt.getId()).prozentErledigt())
                .isEqualTo(100);
    }
}
