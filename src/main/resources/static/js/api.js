/*
 * Zugriffsschicht des Frontends auf die REST-API.
 *
 * Alle HTTP-Aufrufe laufen ueber diese Datei. Das haelt Aufrufdetails
 * (Header, CSRF-Token, Fehlerauswertung) an einer Stelle und macht die
 * Ansichten unabhaengig von der Transportebene: Eine Ansicht ruft
 * PP.api.projekte() auf und weiss nicht, dass dahinter ein fetch steht.
 */
(function () {
    "use strict";

    const PP = window.PP || (window.PP = {});

    /**
     * Fehlerobjekt der API-Schicht. Es transportiert den HTTP-Status, die
     * fachliche Meldung des Servers und - bei Validierungsfehlern - die
     * feldbezogenen Meldungen, damit die Oberflaeche sie am jeweiligen
     * Eingabefeld anzeigen kann.
     */
    class ApiFehler extends Error {
        constructor(status, meldung, feldfehler) {
            super(meldung);
            this.name = "ApiFehler";
            this.status = status;
            this.feldfehler = feldfehler || {};
        }
    }

    /** Liest ein Cookie aus; wird fuer das CSRF-Token benoetigt. */
    function leseCookie(name) {
        const treffer = document.cookie
            .split("; ")
            .find(eintrag => eintrag.startsWith(name + "="));
        return treffer ? decodeURIComponent(treffer.substring(name.length + 1)) : null;
    }

    /** Base64-Kodierung, die auch Umlaute in Anmeldedaten korrekt behandelt. */
    function nachBase64(text) {
        const bytes = new TextEncoder().encode(text);
        let binaer = "";
        bytes.forEach(byte => {
            binaer += String.fromCharCode(byte);
        });
        return btoa(binaer);
    }

    /**
     * Stellt sicher, dass ein CSRF-Token vorliegt.
     *
     * <p>Beim Abmelden loescht Spring Security das Token-Cookie. Vor einer
     * erneuten Anmeldung in derselben geladenen Seite muss daher ein neues
     * Token angefordert werden.</p>
     */
    async function sichereTokenZu() {
        if (leseCookie("XSRF-TOKEN")) {
            return;
        }
        await fetch("/api/csrf", {credentials: "same-origin"});
    }

    /**
     * Zentraler Aufruf. Setzt bei schreibenden Anfragen das CSRF-Token, wandelt
     * Fehlerantworten in ApiFehler um und gibt sonst den geparsten JSON-Koerper
     * zurueck.
     */
    async function anfrage(pfad, optionen) {
        const einstellungen = optionen || {};
        const kopfzeilen = {"Accept": "application/json"};

        // Das Token wird bei jedem Aufruf frisch gelesen: Spring Security
        // erneuert es nach einer Anmeldung.
        const csrfToken = leseCookie("XSRF-TOKEN");
        if (csrfToken) {
            kopfzeilen["X-XSRF-TOKEN"] = csrfToken;
        }
        if (einstellungen.anmeldedaten) {
            kopfzeilen["Authorization"] = "Basic " + nachBase64(
                einstellungen.anmeldedaten.email + ":" + einstellungen.anmeldedaten.passwort);
        }

        const konfiguration = {
            method: einstellungen.methode || "GET",
            headers: kopfzeilen,
            // Sitzungscookie mitsenden, aber nur an den eigenen Ursprung.
            credentials: "same-origin"
        };
        if (einstellungen.daten !== undefined) {
            kopfzeilen["Content-Type"] = "application/json";
            konfiguration.body = JSON.stringify(einstellungen.daten);
        }

        let antwort;
        try {
            antwort = await fetch(pfad, konfiguration);
        } catch (netzwerkfehler) {
            throw new ApiFehler(0, "Der Server ist nicht erreichbar. "
                + "Bitte prüfen Sie Ihre Verbindung.", null);
        }

        if (antwort.status === 204) {
            return null;
        }

        const inhaltstyp = antwort.headers.get("Content-Type") || "";
        const koerper = inhaltstyp.includes("application/json")
            ? await antwort.json().catch(() => null)
            : null;

        if (!antwort.ok) {
            const meldung = (koerper && koerper.meldung)
                || "Die Anfrage konnte nicht ausgeführt werden (HTTP " + antwort.status + ").";
            throw new ApiFehler(antwort.status, meldung, koerper && koerper.feldfehler);
        }
        return koerper;
    }

    PP.ApiFehler = ApiFehler;

    PP.api = {

        // --- Sitzung ---------------------------------------------------------

        /**
         * Meldet den Benutzer an. Die Anmeldedaten werden genau einmal per
         * HTTP Basic uebertragen; danach traegt die serverseitige Session die
         * Authentifizierung, sodass das Kennwort nicht im Browser verbleibt.
         */
        async anmelden(email, passwort) {
            await sichereTokenZu();
            return anfrage("/api/auth/login", {
                methode: "POST",
                anmeldedaten: {email: email, passwort: passwort}
            });
        },

        /** Fragt eine bestehende Sitzung ab (beim Start der Anwendung). */
        sitzung() {
            return anfrage("/api/auth/sitzung");
        },

        /** Meldet ab; die Session wird serverseitig invalidiert. */
        abmelden() {
            return anfrage("/api/auth/logout", {methode: "POST"});
        },

        // --- Projekte --------------------------------------------------------

        projekte() {
            return anfrage("/api/projekte");
        },

        projekt(id) {
            return anfrage("/api/projekte/" + id);
        },

        projektAnlegen(daten) {
            return anfrage("/api/projekte", {methode: "POST", daten: daten});
        },

        projektAendern(id, daten) {
            return anfrage("/api/projekte/" + id, {methode: "PUT", daten: daten});
        },

        projektArchivieren(id) {
            return anfrage("/api/projekte/" + id + "/archivierung", {methode: "POST"});
        },

        projektReaktivieren(id) {
            return anfrage("/api/projekte/" + id + "/reaktivierung", {methode: "POST"});
        },

        // --- Aufgaben --------------------------------------------------------

        aufgaben(projektId) {
            return anfrage("/api/projekte/" + projektId + "/aufgaben");
        },

        aufgabeAnlegen(projektId, daten) {
            return anfrage("/api/projekte/" + projektId + "/aufgaben",
                {methode: "POST", daten: daten});
        },

        aufgabeAendern(id, daten) {
            return anfrage("/api/aufgaben/" + id, {methode: "PUT", daten: daten});
        },

        aufgabeStatusAendern(id, status) {
            return anfrage("/api/aufgaben/" + id + "/status",
                {methode: "PATCH", daten: {status: status}});
        },

        aufgabeLoeschen(id) {
            return anfrage("/api/aufgaben/" + id, {methode: "DELETE"});
        },

        // --- Benutzer --------------------------------------------------------

        /** Auswahlliste aktiver Mitarbeitender (auch fuer die Projektleitung). */
        mitarbeitende() {
            return anfrage("/api/mitarbeitende");
        },

        benutzer() {
            return anfrage("/api/benutzer");
        },

        benutzerAnlegen(daten) {
            return anfrage("/api/benutzer", {methode: "POST", daten: daten});
        },

        benutzerAendern(id, daten) {
            return anfrage("/api/benutzer/" + id, {methode: "PUT", daten: daten});
        }
    };
})();
