package de.iu.fallstudie.projektportal.config;

import de.iu.fallstudie.projektportal.domain.Aufgabe;
import de.iu.fallstudie.projektportal.domain.Aufgabenstatus;
import de.iu.fallstudie.projektportal.domain.Benutzer;
import de.iu.fallstudie.projektportal.domain.Mandant;
import de.iu.fallstudie.projektportal.domain.Projekt;
import de.iu.fallstudie.projektportal.domain.Rolle;
import de.iu.fallstudie.projektportal.repository.AufgabeRepository;
import de.iu.fallstudie.projektportal.repository.BenutzerRepository;
import de.iu.fallstudie.projektportal.repository.MandantRepository;
import de.iu.fallstudie.projektportal.repository.ProjektRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Legt beim ersten Start einen nachvollziehbaren Demodatenbestand an.
 *
 * <p>Zwei Mandanten sind bewusst enthalten: Erst dadurch laesst sich die
 * Mandantentrennung praktisch ueberpruefen. Der Initialisierer laeuft nur, wenn
 * die Datenbank leer ist, und ueberschreibt daher keine echten Daten.</p>
 */
@Configuration
@ConditionalOnProperty(name = "projektportal.demodaten", havingValue = "true")
public class DemodatenInitialisierer {

    private static final Logger LOG = LoggerFactory.getLogger(DemodatenInitialisierer.class);

    @Value("${projektportal.demo-kennwort}")
    private String demoKennwort;

    @Bean
    ApplicationRunner demodatenAnlegen(MandantRepository mandantRepository,
                                       BenutzerRepository benutzerRepository,
                                       ProjektRepository projektRepository,
                                       AufgabeRepository aufgabeRepository,
                                       PasswordEncoder passwordEncoder) {
        return args -> befuellen(mandantRepository, benutzerRepository,
                projektRepository, aufgabeRepository, passwordEncoder);
    }

    void befuellen(MandantRepository mandantRepository,
                   BenutzerRepository benutzerRepository,
                   ProjektRepository projektRepository,
                   AufgabeRepository aufgabeRepository,
                   PasswordEncoder passwordEncoder) {

        if (mandantRepository.count() > 0) {
            LOG.info("Datenbestand vorhanden - Demodaten werden nicht erneut angelegt.");
            return;
        }

        String hash = passwordEncoder.encode(demoKennwort);

        // ------------------------------------------------------------------
        // Mandant 1: Nordlicht IT-Services GmbH
        // ------------------------------------------------------------------
        Mandant nordlicht = mandantRepository.save(
                new Mandant("nordlicht", "Nordlicht IT-Services GmbH"));

        benutzerRepository.save(new Benutzer(nordlicht,
                "admin@nordlicht.example", hash, "Sabine", "Kleinert",
                EnumSet.of(Rolle.ADMIN)));
        Benutzer anna = benutzerRepository.save(new Benutzer(nordlicht,
                "anna.behrens@nordlicht.example", hash, "Anna", "Behrens",
                EnumSet.of(Rolle.PROJEKTLEITER, Rolle.MITARBEITER)));
        Benutzer tobias = benutzerRepository.save(new Benutzer(nordlicht,
                "tobias.marek@nordlicht.example", hash, "Tobias", "Marek",
                EnumSet.of(Rolle.PROJEKTLEITER, Rolle.MITARBEITER)));
        Benutzer lea = benutzerRepository.save(new Benutzer(nordlicht,
                "lea.novak@nordlicht.example", hash, "Lea", "Novak",
                EnumSet.of(Rolle.MITARBEITER)));
        Benutzer jonas = benutzerRepository.save(new Benutzer(nordlicht,
                "jonas.riedl@nordlicht.example", hash, "Jonas", "Riedl",
                EnumSet.of(Rolle.MITARBEITER)));
        Benutzer mira = benutzerRepository.save(new Benutzer(nordlicht,
                "mira.kaya@nordlicht.example", hash, "Mira", "Kaya",
                EnumSet.of(Rolle.MITARBEITER)));

        Projekt crm = projekt(projektRepository, nordlicht,
                "CRM-Migration Stadtwerke Lübeck", "Stadtwerke Lübeck AG", anna,
                Set.of(lea, jonas, mira),
                "Ablösung des Altsystems und Übernahme von 140.000 Kundendatensätzen "
                        + "in das neue CRM inklusive Schnittstelle zur Abrechnung.",
                LocalDate.of(2026, 1, 12), LocalDate.of(2026, 9, 30));

        aufgaben(aufgabeRepository, List.of(
                aufgabe(crm, "Feldmapping Altsystem abstimmen", Aufgabenstatus.ERLEDIGT, lea,
                        LocalDate.of(2026, 2, 6),
                        "Abgleich der 82 Kundenfelder mit dem Zielschema."),
                aufgabe(crm, "Migrationsskripte implementieren", Aufgabenstatus.ERLEDIGT, jonas,
                        LocalDate.of(2026, 3, 20),
                        "Batchweise Übernahme mit Protokollierung fehlerhafter Datensätze."),
                aufgabe(crm, "Testmigration auf Vorabsystem", Aufgabenstatus.ERLEDIGT, jonas,
                        LocalDate.of(2026, 4, 24), null),
                aufgabe(crm, "Schnittstelle zur Abrechnung anbinden", Aufgabenstatus.IN_BEARBEITUNG,
                        mira, LocalDate.of(2026, 6, 12),
                        "REST-Anbindung an das Abrechnungssystem, inkl. Fehlerbehandlung."),
                aufgabe(crm, "Schulungsunterlagen erstellen", Aufgabenstatus.IN_BEARBEITUNG, lea,
                        LocalDate.of(2026, 7, 3), null),
                aufgabe(crm, "Abnahmetests mit Fachbereich", Aufgabenstatus.OFFEN, null,
                        LocalDate.of(2026, 8, 21),
                        "Gemeinsame Abnahme der Migrationsergebnisse mit dem Kundenteam."),
                aufgabe(crm, "Produktivumstellung planen", Aufgabenstatus.OFFEN, null,
                        LocalDate.of(2026, 9, 11), null)));

        Projekt portal = projekt(projektRepository, nordlicht,
                "Webportal Relaunch Kramer & Söhne", "Kramer & Söhne KG", tobias,
                Set.of(mira, lea),
                "Neuentwicklung des Kundenportals mit barrierefreiem Frontend und "
                        + "Anbindung an das bestehende Warenwirtschaftssystem.",
                LocalDate.of(2026, 3, 2), LocalDate.of(2026, 11, 27));

        aufgaben(aufgabeRepository, List.of(
                aufgabe(portal, "Anforderungsworkshop durchführen", Aufgabenstatus.ERLEDIGT, mira,
                        LocalDate.of(2026, 3, 13), null),
                aufgabe(portal, "Informationsarchitektur entwerfen", Aufgabenstatus.IN_BEARBEITUNG,
                        mira, LocalDate.of(2026, 5, 8), null),
                aufgabe(portal, "Komponentenbibliothek auswählen", Aufgabenstatus.OFFEN, lea,
                        LocalDate.of(2026, 5, 29),
                        "Bewertung von Material UI und Ant Design anhand der Projektkriterien."),
                aufgabe(portal, "Barrierefreiheit prüfen (WCAG 2.1 AA)", Aufgabenstatus.OFFEN, null,
                        LocalDate.of(2026, 9, 18), null)));

        Projekt umzug = projekt(projektRepository, nordlicht,
                "Rechenzentrumsumzug Nordcampus", "Nordlicht IT-Services GmbH", anna,
                Set.of(jonas),
                "Verlagerung von 42 Serversystemen an den neuen Standort; "
                        + "Projekt im Jahr 2025 abgeschlossen und archiviert.",
                LocalDate.of(2025, 2, 3), LocalDate.of(2025, 10, 17));
        aufgaben(aufgabeRepository, List.of(
                aufgabe(umzug, "Umzugsfenster mit Fachbereichen abstimmen", Aufgabenstatus.ERLEDIGT,
                        jonas, LocalDate.of(2025, 4, 11), null),
                aufgabe(umzug, "Hardware verlagern und in Betrieb nehmen", Aufgabenstatus.ERLEDIGT,
                        jonas, LocalDate.of(2025, 9, 26), null)));
        umzug.archivieren();
        projektRepository.save(umzug);

        // ------------------------------------------------------------------
        // Mandant 2: belegt die Mandantentrennung
        // ------------------------------------------------------------------
        Mandant orbit = mandantRepository.save(
                new Mandant("orbit", "Orbit Consulting AG"));

        benutzerRepository.save(new Benutzer(orbit,
                "admin@orbit.example", hash, "Halil", "Demir",
                EnumSet.of(Rolle.ADMIN)));
        Benutzer orbitLeitung = benutzerRepository.save(new Benutzer(orbit,
                "ruth.olsen@orbit.example", hash, "Ruth", "Olsen",
                EnumSet.of(Rolle.PROJEKTLEITER, Rolle.MITARBEITER)));

        Projekt orbitProjekt = projekt(projektRepository, orbit,
                "Data-Warehouse Konsolidierung", "Orbit Consulting AG", orbitLeitung,
                Set.of(),
                "Interner Datenbestand eines zweiten Mandanten - für Benutzer des "
                        + "Mandanten Nordlicht nicht sichtbar.",
                LocalDate.of(2026, 4, 1), null);
        aufgaben(aufgabeRepository, List.of(
                aufgabe(orbitProjekt, "Quellsysteme inventarisieren", Aufgabenstatus.OFFEN,
                        null, null, null)));

        LOG.info("Demodaten angelegt: {} Mandanten, {} Benutzerkonten, {} Projekte.",
                mandantRepository.count(), benutzerRepository.count(), projektRepository.count());
    }

    private Projekt projekt(ProjektRepository projektRepository, Mandant mandant, String name,
                            String kunde, Benutzer leiter, Set<Benutzer> mitglieder,
                            String beschreibung, LocalDate start, LocalDate ende) {
        Projekt projekt = new Projekt(mandant, name, leiter);
        projekt.setKunde(kunde);
        projekt.setBeschreibung(beschreibung);
        projekt.setStartDatum(start);
        projekt.setEndDatum(ende);
        projekt.setMitglieder(new LinkedHashSet<>(mitglieder));
        return projektRepository.save(projekt);
    }

    private Aufgabe aufgabe(Projekt projekt, String titel, Aufgabenstatus status,
                            Benutzer bearbeiter, LocalDate faelligAm, String beschreibung) {
        Aufgabe aufgabe = new Aufgabe(projekt, titel);
        aufgabe.setStatus(status);
        aufgabe.setBearbeiter(bearbeiter);
        aufgabe.setFaelligAm(faelligAm);
        aufgabe.setBeschreibung(beschreibung);
        return aufgabe;
    }

    private void aufgaben(AufgabeRepository aufgabeRepository, List<Aufgabe> liste) {
        aufgabeRepository.saveAll(liste);
    }
}
