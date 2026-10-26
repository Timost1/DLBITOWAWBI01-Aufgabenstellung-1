package de.iu.fallstudie.projektportal.web;

import de.iu.fallstudie.projektportal.Integrationstest;
import de.iu.fallstudie.projektportal.domain.Aufgabe;
import de.iu.fallstudie.projektportal.domain.Aufgabenstatus;
import de.iu.fallstudie.projektportal.domain.Benutzer;
import de.iu.fallstudie.projektportal.domain.Mandant;
import de.iu.fallstudie.projektportal.domain.Projekt;
import de.iu.fallstudie.projektportal.domain.Rolle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests der HTTP-Schnittstelle.
 *
 * <p>Waehrend die Servicetests die Fachlogik pruefen, sichern diese Tests den
 * Vertrag der REST-API ab: Statuscodes, Fehlerformat, Authentifizierung,
 * rollenbasierte Autorisierung und CSRF-Schutz. Sie laufen gegen die
 * vollstaendige Spring-Security-Filterkette und damit gegen dieselbe
 * Konfiguration wie der Produktivbetrieb.</p>
 */
@AutoConfigureMockMvc
class RestApiTest extends Integrationstest {

    private static final String JSON_PROJEKT_VORLAGE = """
            {"name":"%s","kunde":"Testkunde","beschreibung":null,
             "leiterId":%d,"startDatum":null,"endDatum":null,"mitgliedIds":[]}
            """;

    @Autowired
    private MockMvc mockMvc;

    private Benutzer admin;
    private Benutzer anna;
    private Benutzer lea;
    private Projekt crm;
    private Aufgabe aufgabe;
    private Projekt fremdesProjekt;

    @BeforeEach
    void datenAnlegen() {
        Mandant nordlicht = mandant("nordlicht");
        admin = benutzer(nordlicht, "admin@nordlicht.test", Rolle.ADMIN);
        anna = benutzer(nordlicht, "anna@nordlicht.test", Rolle.PROJEKTLEITER, Rolle.MITARBEITER);
        lea = benutzer(nordlicht, "lea@nordlicht.test", Rolle.MITARBEITER);
        crm = projekt(nordlicht, "CRM-Migration", anna, lea);
        aufgabe = aufgabe(crm, "Feldmapping abstimmen", Aufgabenstatus.OFFEN);

        Mandant orbit = mandant("orbit");
        Benutzer leitungOrbit = benutzer(orbit, "leitung@orbit.test", Rolle.PROJEKTLEITER);
        fremdesProjekt = projekt(orbit, "Data-Warehouse", leitungOrbit);
    }

    // ------------------------------------------------------------------
    // Authentifizierung
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Ohne Anmeldung wird jeder API-Zugriff mit 401 abgewiesen")
    void ohneAnmeldungKeinZugriff() throws Exception {
        mockMvc.perform(get("/api/projekte"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Die Anmeldung liefert Profil, Rollen und Mandant zurück")
    void anmeldungLiefertProfil() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(httpBasic(anna.getEmail(), KENNWORT))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(anna.getEmail()))
                .andExpect(jsonPath("$.mandantKuerzel").value("nordlicht"))
                .andExpect(jsonPath("$.rollen").isArray());
    }

    @Test
    @DisplayName("Ein falsches Kennwort führt zu 401")
    void falschesKennwort() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .with(httpBasic(anna.getEmail(), "falsch"))
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Die Abmeldung beendet die Sitzung mit 204")
    void abmeldung() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .with(httpBasic(anna.getEmail(), KENNWORT))
                        .with(csrf()))
                .andExpect(status().isNoContent());
    }

    // ------------------------------------------------------------------
    // CSRF-Schutz
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Eine schreibende Anfrage ohne CSRF-Token wird abgewiesen")
    void schreibzugriffBrauchtCsrfToken() throws Exception {
        mockMvc.perform(post("/api/projekte")
                        .with(httpBasic(anna.getEmail(), KENNWORT))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_PROJEKT_VORLAGE.formatted("Neues Projekt", anna.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Lesende Anfragen benötigen kein CSRF-Token")
    void lesezugriffOhneCsrfToken() throws Exception {
        mockMvc.perform(get("/api/projekte")
                        .with(httpBasic(anna.getEmail(), KENNWORT)))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------
    // Autorisierung
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Die Projektliste enthält nur Projekte, für die der Benutzer berechtigt ist")
    void projektlisteIstEingeschraenkt() throws Exception {
        mockMvc.perform(get("/api/projekte").with(httpBasic(lea.getEmail(), KENNWORT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("CRM-Migration"));
    }

    @Test
    @DisplayName("Mitarbeitende dürfen keine Projekte anlegen")
    void mitarbeitendeDuerfenKeineProjekteAnlegen() throws Exception {
        mockMvc.perform(post("/api/projekte")
                        .with(httpBasic(lea.getEmail(), KENNWORT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_PROJEKT_VORLAGE.formatted("Verbotenes Projekt", anna.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.fehlercode").value("KEINE_BERECHTIGUNG"));
    }

    @Test
    @DisplayName("Die Benutzerverwaltung ist ausschließlich Administratoren zugänglich")
    void benutzerverwaltungNurFuerAdministratoren() throws Exception {
        mockMvc.perform(get("/api/benutzer").with(httpBasic(anna.getEmail(), KENNWORT)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/benutzer").with(httpBasic(admin.getEmail(), KENNWORT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    @DisplayName("Die Auswahlliste der Mitarbeitenden steht auch der Projektleitung offen")
    void auswahllisteFuerProjektleitung() throws Exception {
        mockMvc.perform(get("/api/mitarbeitende").with(httpBasic(anna.getEmail(), KENNWORT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                // Das schmale DTO gibt bewusst keine Rollen und keinen Aktivstatus aus.
                .andExpect(jsonPath("$[0].rollen").doesNotExist());
    }

    @Test
    @DisplayName("Ein Projekt eines fremden Mandanten liefert 404")
    void fremdeProjekteSindNichtAdressierbar() throws Exception {
        mockMvc.perform(get("/api/projekte/" + fremdesProjekt.getId())
                        .with(httpBasic(admin.getEmail(), KENNWORT)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.fehlercode").value("NICHT_GEFUNDEN"));
    }

    // ------------------------------------------------------------------
    // Validierung und Fehlerformat
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Verletzte Eingabeprüfungen liefern 400 mit feldbezogenen Meldungen")
    void validierungsfehlerMitFeldbezug() throws Exception {
        mockMvc.perform(post("/api/projekte")
                        .with(httpBasic(anna.getEmail(), KENNWORT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \",\"leiterId\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fehlercode").value("VALIDIERUNG"))
                .andExpect(jsonPath("$.feldfehler.name").exists())
                .andExpect(jsonPath("$.feldfehler.leiterId").exists());
    }

    @Test
    @DisplayName("Ein syntaktisch fehlerhafter Request-Body liefert 400")
    void unlesbarerRequestBody() throws Exception {
        mockMvc.perform(post("/api/projekte")
                        .with(httpBasic(anna.getEmail(), KENNWORT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Kaputt\""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fehlercode").value("UNLESBARE_ANFRAGE"));
    }

    @Test
    @DisplayName("Ein fachlicher Regelverstoß liefert 409 mit verständlicher Meldung")
    void fachlicherRegelverstoss() throws Exception {
        mockMvc.perform(post("/api/projekte")
                        .with(httpBasic(anna.getEmail(), KENNWORT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_PROJEKT_VORLAGE.formatted("CRM-Migration", anna.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.fehlercode").value("REGELVERSTOSS"))
                .andExpect(jsonPath("$.meldung").value(
                        org.hamcrest.Matchers.containsString("existiert bereits")));
    }

    @Test
    @DisplayName("Ein unbekannter Pfad liefert 404 und keinen Serverfehler")
    void unbekannterPfad() throws Exception {
        mockMvc.perform(get("/api/gibtesnicht").with(httpBasic(anna.getEmail(), KENNWORT)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Ein nicht vorgesehenes HTTP-Verb liefert 405 und keinen Serverfehler")
    void falschesHttpVerb() throws Exception {
        mockMvc.perform(delete("/api/projekte/" + crm.getId())
                        .with(httpBasic(anna.getEmail(), KENNWORT))
                        .with(csrf()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.fehlercode").value("METHODE_NICHT_ERLAUBT"));
    }

    // ------------------------------------------------------------------
    // Fachliche Abläufe über HTTP
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Ein Statuswechsel wirkt sich unmittelbar auf den Projektfortschritt aus")
    void statuswechselAktualisiertFortschritt() throws Exception {
        mockMvc.perform(get("/api/projekte/" + crm.getId())
                        .with(httpBasic(lea.getEmail(), KENNWORT)))
                .andExpect(jsonPath("$.fortschritt.prozentErledigt").value(0));

        mockMvc.perform(patch("/api/aufgaben/" + aufgabe.getId() + "/status")
                        .with(httpBasic(lea.getEmail(), KENNWORT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ERLEDIGT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ERLEDIGT"));

        mockMvc.perform(get("/api/projekte/" + crm.getId())
                        .with(httpBasic(lea.getEmail(), KENNWORT)))
                .andExpect(jsonPath("$.fortschritt.prozentErledigt").value(100));
    }

    @Test
    @DisplayName("Ein ungültiger Statuswert wird abgewiesen")
    void ungueltigerStatuswert() throws Exception {
        mockMvc.perform(patch("/api/aufgaben/" + aufgabe.getId() + "/status")
                        .with(httpBasic(lea.getEmail(), KENNWORT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"VIELLEICHT\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Ein neues Projekt wird mit 201 angelegt und erscheint in der Liste")
    void projektAnlegenUeberHttp() throws Exception {
        mockMvc.perform(post("/api/projekte")
                        .with(httpBasic(anna.getEmail(), KENNWORT))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_PROJEKT_VORLAGE.formatted("Portalrelaunch", anna.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Portalrelaunch"))
                .andExpect(jsonPath("$.status").value("AKTIV"))
                .andExpect(jsonPath("$.darfBearbeiten").value(true));

        mockMvc.perform(get("/api/projekte").with(httpBasic(anna.getEmail(), KENNWORT)))
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Ein Benutzerkonto wird ohne Kennwort-Hash ausgeliefert")
    void kennwortHashWirdNichtAusgeliefert() throws Exception {
        mockMvc.perform(get("/api/benutzer").with(httpBasic(admin.getEmail(), KENNWORT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].passwortHash").doesNotExist())
                .andExpect(jsonPath("$[0].passwort").doesNotExist());
    }

    @Test
    @DisplayName("Der Endpunkt für das CSRF-Token ist ohne Anmeldung erreichbar")
    void csrfEndpunktIstOeffentlich() throws Exception {
        mockMvc.perform(get("/api/csrf"))
                .andExpect(status().isNoContent());
    }
}
