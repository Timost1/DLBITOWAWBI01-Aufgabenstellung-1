/*
 * Wiederverwendbare Bausteine der Oberflaeche.
 *
 * Hier liegen die Komponenten, die in mehreren Ansichten vorkommen, sowie die
 * Hilfsmittel fuer Zustandsverwaltung und Formatierung. Die Ansichten bleiben
 * dadurch schlank und beschreiben nur noch ihren eigenen Aufbau.
 */
(function () {
    "use strict";

    const PP = window.PP || (window.PP = {});
    const h = React.createElement;
    const MUI = window.MaterialUI;

    // ------------------------------------------------------------------
    // Beschriftungen und Formatierung
    // ------------------------------------------------------------------

    /** Anzeigetexte der Rollen; die Enum-Namen selbst bleiben technisch. */
    const ROLLENNAMEN = {
        ADMIN: "Administrator:in",
        PROJEKTLEITER: "Projektleiter:in",
        MITARBEITER: "Mitarbeiter:in"
    };

    /** Anzeigetexte der Aufgabenstatus gemäß Product Backlog. */
    const AUFGABENSTATUS = [
        {wert: "OFFEN", text: "Offen", farbe: "default"},
        {wert: "IN_BEARBEITUNG", text: "In Bearbeitung", farbe: "warning"},
        {wert: "ERLEDIGT", text: "Erledigt", farbe: "success"}
    ];

    function statusInfo(wert) {
        return AUFGABENSTATUS.find(eintrag => eintrag.wert === wert)
            || {wert: wert, text: wert, farbe: "default"};
    }

    const datumsformat = new Intl.DateTimeFormat("de-DE", {
        day: "2-digit", month: "2-digit", year: "numeric"
    });

    /**
     * Formatiert ein ISO-Datum (YYYY-MM-DD) fuer die deutsche Darstellung.
     * Die API liefert bewusst ISO-8601; die landesspezifische Aufbereitung
     * erfolgt erst in der Oberflaeche.
     */
    function formatiereDatum(isoDatum) {
        if (!isoDatum) {
            return "–";
        }
        const teile = isoDatum.split("-");
        return datumsformat.format(new Date(
            Number(teile[0]), Number(teile[1]) - 1, Number(teile[2])));
    }

    // ------------------------------------------------------------------
    // Zustandsverwaltung fuer Serverabfragen
    // ------------------------------------------------------------------

    const ANFANGSZUSTAND = {status: "leer", daten: null, fehler: null};

    /**
     * Zustandsmaschine fuer den Lebenszyklus einer Serverabfrage. Jede Aktion
     * beschreibt den vollstaendigen Folgezustand. Dadurch kann nicht gleichzeitig
     * ein Ladeindikator und eine alte Fehlermeldung sichtbar sein - ein
     * typisches Problem, wenn mehrere unabhaengige useState-Werte koordiniert
     * werden muessen.
     */
    function anfrageReducer(zustand, aktion) {
        switch (aktion.type) {
            case "laden":
                return {status: "laedt", daten: zustand.daten, fehler: null};
            case "erfolg":
                return {status: "erfolg", daten: aktion.daten, fehler: null};
            case "fehler":
                return {status: "fehler", daten: null, fehler: aktion.fehler};
            default:
                return zustand;
        }
    }

    /**
     * Laedt Daten vom Server und stellt Zustand sowie eine Funktion zum
     * erneuten Laden bereit.
     *
     * @param ladeFunktion   Funktion, die ein Promise mit den Daten liefert
     * @param abhaengigkeiten Werte, bei deren Aenderung neu geladen wird
     */
    function verwendeAnfrage(ladeFunktion, abhaengigkeiten) {
        const [zustand, dispatch] = React.useReducer(anfrageReducer, ANFANGSZUSTAND);
        const [zaehler, setZaehler] = React.useState(0);

        React.useEffect(() => {
            let verworfen = false;
            dispatch({type: "laden"});
            ladeFunktion()
                .then(daten => {
                    // Antworten zu bereits verlassenen Ansichten werden
                    // verworfen; das verhindert Wettlaufsituationen.
                    if (!verworfen) {
                        dispatch({type: "erfolg", daten: daten});
                    }
                })
                .catch(fehler => {
                    if (!verworfen) {
                        dispatch({type: "fehler", fehler: fehler});
                    }
                });
            return () => {
                verworfen = true;
            };
            // eslint-disable-next-line
        }, (abhaengigkeiten || []).concat(zaehler));

        return {
            status: zustand.status,
            daten: zustand.daten,
            fehler: zustand.fehler,
            neuLaden: () => setZaehler(wert => wert + 1)
        };
    }

    // ------------------------------------------------------------------
    // Komponenten
    // ------------------------------------------------------------------

    /** Ladeanzeige waehrend laufender Serverabfragen. */
    function Ladeanzeige(eigenschaften) {
        return h(MUI.Box, {
            sx: {display: "flex", justifyContent: "center", alignItems: "center", py: 6, gap: 2},
            role: "status",
            "aria-live": "polite"
        },
            h(MUI.CircularProgress, {size: 28}),
            h(MUI.Typography, {color: "text.secondary"},
                eigenschaften.text || "Daten werden geladen …"));
    }

    /** Einheitliche Darstellung eines Fehlers samt Wiederholungsmöglichkeit. */
    function Fehlerhinweis(eigenschaften) {
        const fehler = eigenschaften.fehler;
        return h(MUI.Alert, {
            severity: "error",
            sx: {my: 2},
            action: eigenschaften.beiWiederholung
                ? h(MUI.Button, {
                    color: "inherit",
                    size: "small",
                    onClick: eigenschaften.beiWiederholung
                }, "Erneut versuchen")
                : null
        },
            h(MUI.AlertTitle, null, eigenschaften.titel || "Fehler"),
            fehler ? fehler.message : eigenschaften.text);
    }

    /** Statusanzeige einer Aufgabe. */
    function Statuskennzeichen(eigenschaften) {
        const info = statusInfo(eigenschaften.status);
        return h(MUI.Chip, {
            label: info.text,
            color: info.farbe,
            size: eigenschaften.size || "small",
            variant: info.farbe === "default" ? "outlined" : "filled"
        });
    }

    /**
     * Fortschrittsbalken eines Projekts. Zeigt den vom Server berechneten
     * Anteil erledigter Aufgaben und die Aufschluesselung je Status.
     */
    function Fortschrittsanzeige(eigenschaften) {
        const fortschritt = eigenschaften.fortschritt
            || {anzahlGesamt: 0, anzahlOffen: 0, anzahlInBearbeitung: 0, anzahlErledigt: 0,
                prozentErledigt: 0};

        return h(MUI.Box, {sx: {width: "100%"}},
            h(MUI.Box, {sx: {display: "flex", justifyContent: "space-between", mb: 0.5}},
                h(MUI.Typography, {variant: "body2", color: "text.secondary"},
                    "Fortschritt"),
                h(MUI.Typography, {
                    variant: "body2",
                    fontWeight: 600,
                    "aria-label": "Fortschritt " + fortschritt.prozentErledigt + " Prozent"
                }, fortschritt.prozentErledigt + " %")),
            h(MUI.LinearProgress, {
                variant: "determinate",
                value: fortschritt.prozentErledigt,
                sx: {height: 8, borderRadius: 4},
                "aria-hidden": true
            }),
            h(MUI.Typography, {variant: "caption", color: "text.secondary",
                sx: {display: "block", mt: 0.75}},
                fortschritt.anzahlErledigt + " von " + fortschritt.anzahlGesamt
                + " Aufgaben erledigt · " + fortschritt.anzahlInBearbeitung
                + " in Bearbeitung · " + fortschritt.anzahlOffen + " offen"));
    }

    /** Abschnittsrahmen mit Titel und optionaler Aktion in der Kopfzeile. */
    function Abschnitt(eigenschaften) {
        return h(MUI.Paper, {variant: "outlined", sx: {p: {xs: 2, sm: 3}, mb: 3}},
            h(MUI.Box, {sx: {display: "flex", flexWrap: "wrap", gap: 1,
                justifyContent: "space-between", alignItems: "center", mb: 2}},
                h(MUI.Typography, {variant: "h6", component: "h2"}, eigenschaften.titel),
                eigenschaften.aktion || null),
            eigenschaften.children);
    }

    /** Hinweis, wenn eine Liste keine Einträge enthält. */
    function Leerhinweis(eigenschaften) {
        return h(MUI.Box, {sx: {py: 4, textAlign: "center"}},
            h(MUI.Typography, {color: "text.secondary"}, eigenschaften.text));
    }

    PP.bausteine = {
        h: h,
        MUI: MUI,
        ROLLENNAMEN: ROLLENNAMEN,
        AUFGABENSTATUS: AUFGABENSTATUS,
        statusInfo: statusInfo,
        formatiereDatum: formatiereDatum,
        verwendeAnfrage: verwendeAnfrage,
        Ladeanzeige: Ladeanzeige,
        Fehlerhinweis: Fehlerhinweis,
        Statuskennzeichen: Statuskennzeichen,
        Fortschrittsanzeige: Fortschrittsanzeige,
        Abschnitt: Abschnitt,
        Leerhinweis: Leerhinweis
    };
})();
