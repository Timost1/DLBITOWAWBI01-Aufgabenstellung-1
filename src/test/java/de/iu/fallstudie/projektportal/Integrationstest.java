package de.iu.fallstudie.projektportal;

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
import de.iu.fallstudie.projektportal.security.AngemeldeterBenutzer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Set;

/**
 * Gemeinsame Basis der Integrationstests.
 *
 * <p>Jeder Test laeuft in einer eigenen Transaktion, die nach dem Test
 * zurueckgerollt wird. Dadurch startet jeder Test mit leerer Datenbank und die
 * Tests bleiben voneinander unabhaengig - eine Voraussetzung dafuer, dass die
 * Suite reproduzierbar ist.</p>
 *
 * <p>Die Hilfsmethoden legen genau die Datenkonstellationen an, die mehrere
 * Tests benoetigen. Das haelt die einzelnen Testmethoden lesbar und auf ihre
 * eigentliche Aussage konzentriert.</p>
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
public abstract class Integrationstest {

    /** Kennwort aller Testkonten. */
    protected static final String KENNWORT = "Testkennwort1!";

    @Autowired
    protected MandantRepository mandantRepository;

    @Autowired
    protected BenutzerRepository benutzerRepository;

    @Autowired
    protected ProjektRepository projektRepository;

    @Autowired
    protected AufgabeRepository aufgabeRepository;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected Mandant mandant(String kuerzel) {
        return mandantRepository.save(new Mandant(kuerzel, "Mandant " + kuerzel));
    }

    protected Benutzer benutzer(Mandant mandant, String email, Rolle... rollen) {
        return benutzerRepository.save(new Benutzer(
                mandant, email, passwordEncoder.encode(KENNWORT),
                "Vor", email.substring(0, email.indexOf('@')), Set.of(rollen)));
    }

    protected Projekt projekt(Mandant mandant, String name, Benutzer leiter, Benutzer... mitglieder) {
        Projekt projekt = new Projekt(mandant, name, leiter);
        projekt.setStartDatum(LocalDate.of(2026, 1, 1));
        projekt.setMitglieder(new java.util.LinkedHashSet<>(Set.of(mitglieder)));
        return projektRepository.save(projekt);
    }

    protected Aufgabe aufgabe(Projekt projekt, String titel, Aufgabenstatus status) {
        Aufgabe aufgabe = new Aufgabe(projekt, titel);
        aufgabe.setStatus(status);
        return aufgabeRepository.save(aufgabe);
    }

    /** Baut den Sicherheitskontext, den die Serviceschicht erwartet. */
    protected AngemeldeterBenutzer angemeldet(Benutzer benutzer) {
        return new AngemeldeterBenutzer(benutzer);
    }
}
