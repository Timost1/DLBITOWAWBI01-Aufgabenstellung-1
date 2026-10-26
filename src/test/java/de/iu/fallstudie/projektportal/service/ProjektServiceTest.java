package de.iu.fallstudie.projektportal.service;

import de.iu.fallstudie.projektportal.Integrationstest;
import de.iu.fallstudie.projektportal.domain.Aufgabenstatus;
import de.iu.fallstudie.projektportal.domain.Benutzer;
import de.iu.fallstudie.projektportal.domain.Mandant;
import de.iu.fallstudie.projektportal.domain.Projekt;
import de.iu.fallstudie.projektportal.domain.Projektstatus;
import de.iu.fallstudie.projektportal.domain.Rolle;
import de.iu.fallstudie.projektportal.web.dto.FortschrittDto;
import de.iu.fallstudie.projektportal.web.dto.ProjektDto;
import de.iu.fallstudie.projektportal.web.dto.ProjektEingabeDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests der Projektlogik.
 *
 * <p>Der Schwerpunkt liegt auf den Regeln, die sich nicht deklarativ ausdruecken
 * lassen: Sichtbarkeit von Projekten, Mandantentrennung, Fortschrittsberechnung
 * und Archivierung.</p>
 */
class ProjektServiceTest extends Integrationstest {

    @Autowired
    private ProjektService projektService;

    @Test
    @DisplayName("Mitarbeitende sehen nur Projekte, denen sie zugeordnet sind")
    void sichtbarkeitFuerMitarbeitende() {
        Mandant mandant = mandant("alpha");
        Benutzer leitung = benutzer(mandant, "leitung@alpha.test", Rolle.PROJEKTLEITER);
        Benutzer lea = benutzer(mandant, "lea@alpha.test", Rolle.MITARBEITER);
        Benutzer tim = benutzer(mandant, "tim@alpha.test", Rolle.MITARBEITER);

        projekt(mandant, "Projekt mit Lea", leitung, lea);
        projekt(mandant, "Projekt ohne Lea", leitung, tim);

        List<ProjektDto> sichtbar = projektService.findeSichtbareProjekte(angemeldet(lea));

        assertThat(sichtbar).extracting(ProjektDto::name)
                .containsExactly("Projekt mit Lea");
    }

    @Test
    @DisplayName("Die Projektleitung sieht die von ihr geleiteten Projekte")
    void sichtbarkeitFuerProjektleitung() {
        Mandant mandant = mandant("alpha");
        Benutzer anna = benutzer(mandant, "anna@alpha.test", Rolle.PROJEKTLEITER);
        Benutzer tobias = benutzer(mandant, "tobias@alpha.test", Rolle.PROJEKTLEITER);

        projekt(mandant, "Projekt von Anna", anna);
        projekt(mandant, "Projekt von Tobias", tobias);

        assertThat(projektService.findeSichtbareProjekte(angemeldet(anna)))
                .extracting(ProjektDto::name)
                .containsExactly("Projekt von Anna");
    }

    @Test
    @DisplayName("Administratoren sehen alle Projekte ihres Mandanten")
    void sichtbarkeitFuerAdministratoren() {
        Mandant mandant = mandant("alpha");
        Benutzer admin = benutzer(mandant, "admin@alpha.test", Rolle.ADMIN);
        Benutzer anna = benutzer(mandant, "anna@alpha.test", Rolle.PROJEKTLEITER);

        projekt(mandant, "Projekt A", anna);
        projekt(mandant, "Projekt B", anna);

        assertThat(projektService.findeSichtbareProjekte(angemeldet(admin))).hasSize(2);
    }

    @Test
    @DisplayName("Projekte fremder Mandanten bleiben unsichtbar und nicht abrufbar")
    void mandantentrennung() {
        Mandant alpha = mandant("alpha");
        Mandant beta = mandant("beta");
        Benutzer adminAlpha = benutzer(alpha, "admin@alpha.test", Rolle.ADMIN);
        Benutzer leitungBeta = benutzer(beta, "leitung@beta.test", Rolle.PROJEKTLEITER);

        Projekt fremdesProjekt = projekt(beta, "Fremdes Projekt", leitungBeta);

        assertThat(projektService.findeSichtbareProjekte(angemeldet(adminAlpha))).isEmpty();

        // Selbst mit bekannter ID bleibt der Zugriff verwehrt - und zwar mit 404,
        // damit die Existenz des fremden Datensatzes nicht erkennbar wird.
        assertThatThrownBy(() ->
                projektService.findeProjekt(angemeldet(adminAlpha), fremdesProjekt.getId()))
                .isInstanceOf(RessourceNichtGefundenException.class);
    }

    @Test
    @DisplayName("Der Fortschritt entspricht dem Anteil erledigter Aufgaben")
    void fortschrittsberechnung() {
        Mandant mandant = mandant("alpha");
        Benutzer anna = benutzer(mandant, "anna@alpha.test", Rolle.PROJEKTLEITER);
        Projekt projekt = projekt(mandant, "Messprojekt", anna);

        aufgabe(projekt, "A", Aufgabenstatus.ERLEDIGT);
        aufgabe(projekt, "B", Aufgabenstatus.ERLEDIGT);
        aufgabe(projekt, "C", Aufgabenstatus.IN_BEARBEITUNG);
        aufgabe(projekt, "D", Aufgabenstatus.OFFEN);

        FortschrittDto fortschritt = projektService.berechneFortschritt(projekt.getId());

        assertThat(fortschritt.anzahlGesamt()).isEqualTo(4);
        assertThat(fortschritt.anzahlErledigt()).isEqualTo(2);
        assertThat(fortschritt.anzahlInBearbeitung()).isEqualTo(1);
        assertThat(fortschritt.anzahlOffen()).isEqualTo(1);
        assertThat(fortschritt.prozentErledigt()).isEqualTo(50);
    }

    @Test
    @DisplayName("Ein Projekt ohne Aufgaben hat 0 Prozent Fortschritt und keine Division durch Null")
    void fortschrittOhneAufgaben() {
        Mandant mandant = mandant("alpha");
        Benutzer anna = benutzer(mandant, "anna@alpha.test", Rolle.PROJEKTLEITER);
        Projekt projekt = projekt(mandant, "Leeres Projekt", anna);

        assertThat(projektService.berechneFortschritt(projekt.getId()).prozentErledigt())
                .isZero();
    }

    @Test
    @DisplayName("Nur die verantwortliche Projektleitung darf ein Projekt archivieren")
    void archivierungNurDurchVerantwortlicheLeitung() {
        Mandant mandant = mandant("alpha");
        Benutzer anna = benutzer(mandant, "anna@alpha.test", Rolle.PROJEKTLEITER);
        Benutzer tobias = benutzer(mandant, "tobias@alpha.test",
                Rolle.PROJEKTLEITER, Rolle.MITARBEITER);
        Projekt projekt = projekt(mandant, "Projekt von Anna", anna, tobias);

        assertThatThrownBy(() -> projektService.archivieren(angemeldet(tobias), projekt.getId()))
                .isInstanceOf(ZugriffVerweigertException.class);

        ProjektDto archiviert = projektService.archivieren(angemeldet(anna), projekt.getId());
        assertThat(archiviert.status()).isEqualTo(Projektstatus.ARCHIVIERT);
        assertThat(archiviert.darfBearbeiten()).isFalse();
    }

    @Test
    @DisplayName("Ein archiviertes Projekt ist schreibgeschützt, kann aber reaktiviert werden")
    void archiviertesProjektIstSchreibgeschuetzt() {
        Mandant mandant = mandant("alpha");
        Benutzer anna = benutzer(mandant, "anna@alpha.test", Rolle.PROJEKTLEITER);
        Projekt projekt = projekt(mandant, "Altprojekt", anna);
        projektService.archivieren(angemeldet(anna), projekt.getId());

        ProjektEingabeDto eingabe = new ProjektEingabeDto("Neuer Name", null, null,
                anna.getId(), null, null, Set.of());

        assertThatThrownBy(() ->
                projektService.aendern(angemeldet(anna), projekt.getId(), eingabe))
                .isInstanceOf(FachlicheRegelException.class);

        assertThat(projektService.reaktivieren(angemeldet(anna), projekt.getId()).status())
                .isEqualTo(Projektstatus.AKTIV);
    }

    @Test
    @DisplayName("Ein Projektname darf innerhalb eines Mandanten nicht doppelt vorkommen")
    void projektnameIstProMandantEindeutig() {
        Mandant mandant = mandant("alpha");
        Benutzer anna = benutzer(mandant, "anna@alpha.test", Rolle.PROJEKTLEITER);
        projekt(mandant, "CRM-Migration", anna);

        ProjektEingabeDto eingabe = new ProjektEingabeDto("crm-migration", null, null,
                anna.getId(), null, null, Set.of());

        assertThatThrownBy(() -> projektService.anlegen(angemeldet(anna), eingabe))
                .isInstanceOf(FachlicheRegelException.class)
                .hasMessageContaining("existiert bereits");
    }

    @Test
    @DisplayName("Das Enddatum darf nicht vor dem Startdatum liegen")
    void zeitraumWirdGeprueft() {
        Mandant mandant = mandant("alpha");
        Benutzer anna = benutzer(mandant, "anna@alpha.test", Rolle.PROJEKTLEITER);

        ProjektEingabeDto eingabe = new ProjektEingabeDto("Zeitreise", null, null,
                anna.getId(), LocalDate.of(2026, 5, 1), LocalDate.of(2026, 4, 1), Set.of());

        assertThatThrownBy(() -> projektService.anlegen(angemeldet(anna), eingabe))
                .isInstanceOf(FachlicheRegelException.class)
                .hasMessageContaining("Enddatum");
    }

    @Test
    @DisplayName("Als Projektleitung ist nur ein Konto mit passender Rolle zulässig")
    void projektleitungBrauchtPassendeRolle() {
        Mandant mandant = mandant("alpha");
        Benutzer anna = benutzer(mandant, "anna@alpha.test", Rolle.PROJEKTLEITER);
        Benutzer lea = benutzer(mandant, "lea@alpha.test", Rolle.MITARBEITER);

        ProjektEingabeDto eingabe = new ProjektEingabeDto("Neues Projekt", null, null,
                lea.getId(), null, null, Set.of());

        assertThatThrownBy(() -> projektService.anlegen(angemeldet(anna), eingabe))
                .isInstanceOf(FachlicheRegelException.class)
                .hasMessageContaining("Projektleiter:in");
    }

    @Test
    @DisplayName("Mitarbeitende eines fremden Mandanten können nicht zugeordnet werden")
    void mitgliederNurAusEigenemMandanten() {
        Mandant alpha = mandant("alpha");
        Mandant beta = mandant("beta");
        Benutzer anna = benutzer(alpha, "anna@alpha.test", Rolle.PROJEKTLEITER);
        Benutzer fremder = benutzer(beta, "fremd@beta.test", Rolle.MITARBEITER);

        ProjektEingabeDto eingabe = new ProjektEingabeDto("Grenzfall", null, null,
                anna.getId(), null, null, Set.of(fremder.getId()));

        assertThatThrownBy(() -> projektService.anlegen(angemeldet(anna), eingabe))
                .isInstanceOf(RessourceNichtGefundenException.class);
    }

    @Test
    @DisplayName("Beim Anlegen werden Stammdaten und Zuordnungen übernommen")
    void anlegenUebernimmtAlleAngaben() {
        Mandant mandant = mandant("alpha");
        Benutzer anna = benutzer(mandant, "anna@alpha.test", Rolle.PROJEKTLEITER);
        Benutzer lea = benutzer(mandant, "lea@alpha.test", Rolle.MITARBEITER);

        ProjektEingabeDto eingabe = new ProjektEingabeDto("  Portalrelaunch  ", "Kramer KG",
                "Neuentwicklung", anna.getId(),
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 1), Set.of(lea.getId()));

        ProjektDto angelegt = projektService.anlegen(angemeldet(anna), eingabe);

        assertThat(angelegt.name()).isEqualTo("Portalrelaunch");
        assertThat(angelegt.kunde()).isEqualTo("Kramer KG");
        assertThat(angelegt.status()).isEqualTo(Projektstatus.AKTIV);
        assertThat(angelegt.leiter().id()).isEqualTo(anna.getId());
        assertThat(angelegt.mitglieder()).hasSize(1);
        assertThat(angelegt.fortschritt().anzahlGesamt()).isZero();
    }
}
