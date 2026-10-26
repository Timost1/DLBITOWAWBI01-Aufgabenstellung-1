package de.iu.fallstudie.projektportal.service;

import de.iu.fallstudie.projektportal.Integrationstest;
import de.iu.fallstudie.projektportal.domain.Benutzer;
import de.iu.fallstudie.projektportal.domain.Mandant;
import de.iu.fallstudie.projektportal.domain.Rolle;
import de.iu.fallstudie.projektportal.web.dto.BenutzerAendernDto;
import de.iu.fallstudie.projektportal.web.dto.BenutzerAnlegenDto;
import de.iu.fallstudie.projektportal.web.dto.BenutzerDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests der Benutzer- und Rollenverwaltung, insbesondere der Schutzregeln, die
 * verhindern, dass ein Mandant sich selbst handlungsunfaehig macht.
 */
class BenutzerServiceTest extends Integrationstest {

    @Autowired
    private BenutzerService benutzerService;

    @Autowired
    private ProjektService projektService;

    @Test
    @DisplayName("Ein neues Konto wird mit gehashtem Kennwort und Rollen gespeichert")
    void kontoAnlegen() {
        Mandant mandant = mandant("alpha");
        Benutzer admin = benutzer(mandant, "admin@alpha.test", Rolle.ADMIN);

        BenutzerDto angelegt = benutzerService.anlegen(angemeldet(admin),
                new BenutzerAnlegenDto("Neu.Person@Alpha.test", "Neu", "Person",
                        "Startkennwort1!", Set.of(Rolle.MITARBEITER)));

        assertThat(angelegt.email()).isEqualTo("neu.person@alpha.test");
        assertThat(angelegt.rollen()).containsExactly(Rolle.MITARBEITER);
        assertThat(angelegt.aktiv()).isTrue();

        Benutzer gespeichert = benutzerRepository.findByEmail("neu.person@alpha.test")
                .orElseThrow();
        assertThat(gespeichert.getPasswortHash())
                .isNotEqualTo("Startkennwort1!")
                .startsWith("$2");
        assertThat(passwordEncoder.matches("Startkennwort1!", gespeichert.getPasswortHash()))
                .isTrue();
    }

    @Test
    @DisplayName("E-Mail-Adressen bleiben eindeutig")
    void doppelteEmailWirdAbgelehnt() {
        Mandant mandant = mandant("alpha");
        Benutzer admin = benutzer(mandant, "admin@alpha.test", Rolle.ADMIN);

        assertThatThrownBy(() -> benutzerService.anlegen(angemeldet(admin),
                new BenutzerAnlegenDto("admin@alpha.test", "Zweit", "Konto",
                        "Startkennwort1!", Set.of(Rolle.ADMIN))))
                .isInstanceOf(FachlicheRegelException.class);
    }

    @Test
    @DisplayName("Administratoren können sich nicht selbst aussperren")
    void selbstaussperrungWirdVerhindert() {
        Mandant mandant = mandant("alpha");
        Benutzer admin = benutzer(mandant, "admin@alpha.test", Rolle.ADMIN);

        assertThatThrownBy(() -> benutzerService.aendern(angemeldet(admin), admin.getId(),
                new BenutzerAendernDto("Vor", "Nach", Set.of(Rolle.MITARBEITER), true, null)))
                .isInstanceOf(FachlicheRegelException.class)
                .hasMessageContaining("Administratorrolle");

        assertThatThrownBy(() -> benutzerService.aendern(angemeldet(admin), admin.getId(),
                new BenutzerAendernDto("Vor", "Nach", Set.of(Rolle.ADMIN), false, null)))
                .isInstanceOf(FachlicheRegelException.class)
                .hasMessageContaining("deaktiviert");
    }

    @Test
    @DisplayName("Wer Projekte leitet, behält die Rolle Projektleiter:in")
    void rollenentzugBeiLaufenderProjektleitung() {
        Mandant mandant = mandant("alpha");
        Benutzer admin = benutzer(mandant, "admin@alpha.test", Rolle.ADMIN);
        Benutzer anna = benutzer(mandant, "anna@alpha.test", Rolle.PROJEKTLEITER);
        projekt(mandant, "Laufendes Projekt", anna);

        assertThatThrownBy(() -> benutzerService.aendern(angemeldet(admin), anna.getId(),
                new BenutzerAendernDto("Anna", "Behrens", Set.of(Rolle.MITARBEITER), true, null)))
                .isInstanceOf(FachlicheRegelException.class)
                .hasMessageContaining("Projektleitung");
    }

    @Test
    @DisplayName("Konten fremder Mandanten sind nicht sichtbar und nicht änderbar")
    void mandantentrennungBeiKonten() {
        Mandant alpha = mandant("alpha");
        Mandant beta = mandant("beta");
        Benutzer adminAlpha = benutzer(alpha, "admin@alpha.test", Rolle.ADMIN);
        Benutzer fremder = benutzer(beta, "fremd@beta.test", Rolle.MITARBEITER);

        assertThat(benutzerService.findeAlle(angemeldet(adminAlpha)))
                .extracting(BenutzerDto::email)
                .containsExactly("admin@alpha.test");

        assertThatThrownBy(() -> benutzerService.aendern(angemeldet(adminAlpha), fremder.getId(),
                new BenutzerAendernDto("Hack", "Versuch", Set.of(Rolle.ADMIN), true, null)))
                .isInstanceOf(RessourceNichtGefundenException.class);
    }

    @Test
    @DisplayName("Deaktivierte Konten erscheinen nicht in der Auswahlliste")
    void auswahllisteEnthaeltNurAktiveKonten() {
        Mandant mandant = mandant("alpha");
        Benutzer admin = benutzer(mandant, "admin@alpha.test", Rolle.ADMIN);
        Benutzer lea = benutzer(mandant, "lea@alpha.test", Rolle.MITARBEITER);

        benutzerService.aendern(angemeldet(admin), lea.getId(),
                new BenutzerAendernDto("Lea", "Novak", Set.of(Rolle.MITARBEITER), false, null));

        assertThat(benutzerService.findeAuswahlliste(angemeldet(admin)))
                .noneMatch(eintrag -> eintrag.email().equals("lea@alpha.test"));
    }

    @Test
    @DisplayName("Ein deaktiviertes Konto kann keinem Projekt zugeordnet werden")
    void deaktiviertesKontoNichtZuordenbar() {
        Mandant mandant = mandant("alpha");
        Benutzer admin = benutzer(mandant, "admin@alpha.test", Rolle.ADMIN);
        Benutzer anna = benutzer(mandant, "anna@alpha.test", Rolle.PROJEKTLEITER);
        Benutzer lea = benutzer(mandant, "lea@alpha.test", Rolle.MITARBEITER);

        benutzerService.aendern(angemeldet(admin), lea.getId(),
                new BenutzerAendernDto("Lea", "Novak", Set.of(Rolle.MITARBEITER), false, null));

        assertThatThrownBy(() -> projektService.anlegen(angemeldet(anna),
                new de.iu.fallstudie.projektportal.web.dto.ProjektEingabeDto(
                        "Projekt", null, null, anna.getId(), null, null, Set.of(lea.getId()))))
                .isInstanceOf(FachlicheRegelException.class)
                .hasMessageContaining("deaktiviert");
    }

    @Test
    @DisplayName("Ein leeres Kennwortfeld lässt das bestehende Kennwort unverändert")
    void kennwortBleibtOhneAngabeUnveraendert() {
        Mandant mandant = mandant("alpha");
        Benutzer admin = benutzer(mandant, "admin@alpha.test", Rolle.ADMIN);
        Benutzer lea = benutzer(mandant, "lea@alpha.test", Rolle.MITARBEITER);
        String hashVorher = lea.getPasswortHash();

        benutzerService.aendern(angemeldet(admin), lea.getId(),
                new BenutzerAendernDto("Lea", "Novak", Set.of(Rolle.MITARBEITER), true, null));

        assertThat(benutzerRepository.findById(lea.getId()).orElseThrow().getPasswortHash())
                .isEqualTo(hashVorher);
    }
}
