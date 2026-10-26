/*
 * Wurzelkomponente, Theme und clientseitige Navigation.
 *
 * Die Anwendung ist eine Single-Page-Anwendung: Es wird genau ein HTML-Dokument
 * geladen, alle weiteren Seitenwechsel finden im Browser statt. Als Router
 * dient der Fragmentteil der URL (#/projekte, #/projekte/7, #/benutzer).
 * Dadurch bleiben Adressen teilbar, und Vor-/Zurueck-Navigation des Browsers
 * funktioniert, ohne dass serverseitiges Routing noetig ist.
 */
(function () {
    "use strict";

    const PP = window.PP;
    const B = PP.bausteine;
    const h = React.createElement;
    const MUI = window.MaterialUI;

    // ------------------------------------------------------------------
    // Theme
    // ------------------------------------------------------------------

    /**
     * Zentrales Theme: Farben, Typografie und Formen werden einmal definiert und
     * gelten fuer alle Komponenten. Einzelne Elemente muessen daher nicht
     * gestaltet werden - genau der Zweck des Themings einer
     * Komponentenbibliothek.
     */
    const theme = MUI.createTheme({
        palette: {
            primary: {main: "#0b3d91"},
            secondary: {main: "#8e24aa"},
            background: {default: "#f4f6f8"}
        },
        typography: {
            fontFamily: "'Segoe UI', 'Helvetica Neue', Arial, sans-serif",
            h5: {fontWeight: 600},
            h6: {fontWeight: 600}
        },
        shape: {borderRadius: 8},
        components: {
            MuiButton: {defaultProps: {disableElevation: true}}
        }
    });

    // ------------------------------------------------------------------
    // Navigation
    // ------------------------------------------------------------------

    /**
     * Wertet den Fragmentteil der URL aus und liefert die aktive Route.
     * Rueckgabe z. B. {ansicht: "projektdetail", projektId: "7"}.
     */
    function leseRoute() {
        const fragment = window.location.hash.replace(/^#\/?/, "");
        const teile = fragment.split("/").filter(teil => teil !== "");

        if (teile[0] === "benutzer") {
            return {ansicht: "benutzer"};
        }
        if (teile[0] === "projekte" && teile[1]) {
            return {ansicht: "projektdetail", projektId: teile[1]};
        }
        return {ansicht: "projekte"};
    }

    /** Hook, der die Komponente bei jeder Änderung des URL-Fragments neu rendert. */
    function verwendeRoute() {
        const [route, setRoute] = React.useState(leseRoute);

        React.useEffect(() => {
            function beiWechsel() {
                setRoute(leseRoute());
            }
            window.addEventListener("hashchange", beiWechsel);
            return () => window.removeEventListener("hashchange", beiWechsel);
        }, []);

        return route;
    }

    /** Kopfzeile mit Navigation, Mandantenangabe und Abmeldung. */
    function Kopfzeile(eigenschaften) {
        const profil = eigenschaften.profil;
        const route = eigenschaften.route;
        const istAdmin = profil.rollen.indexOf("ADMIN") >= 0;

        return h(MUI.AppBar, {position: "static", elevation: 0},
            h(MUI.Toolbar, {sx: {flexWrap: "wrap", gap: 1}},
                h(MUI.Typography, {
                    variant: "h6", component: "span",
                    sx: {fontWeight: 700, mr: 3, letterSpacing: 0.3}
                }, "Projektportal"),

                h(MUI.Box, {component: "nav", "aria-label": "Hauptnavigation",
                    sx: {display: "flex", gap: 0.5, flexGrow: 1}},
                    h(MUI.Button, {
                        color: "inherit", href: "#/projekte",
                        "aria-current": route.ansicht !== "benutzer" ? "page" : undefined,
                        sx: {fontWeight: route.ansicht !== "benutzer" ? 700 : 400}
                    }, "Projekte"),
                    istAdmin
                        ? h(MUI.Button, {
                            color: "inherit", href: "#/benutzer",
                            "aria-current": route.ansicht === "benutzer" ? "page" : undefined,
                            sx: {fontWeight: route.ansicht === "benutzer" ? 700 : 400}
                        }, "Benutzerverwaltung")
                        : null),

                h(MUI.Box, {sx: {display: "flex", alignItems: "center", gap: 1.5}},
                    h(MUI.Box, {sx: {textAlign: "right", display: {xs: "none", sm: "block"}}},
                        h(MUI.Typography, {variant: "body2", sx: {lineHeight: 1.2}},
                            profil.vollerName),
                        h(MUI.Typography, {variant: "caption", sx: {opacity: 0.8}},
                            profil.mandantName)),
                    h(MUI.Button, {
                        color: "inherit", variant: "outlined", size: "small",
                        onClick: eigenschaften.beiAbmeldung
                    }, "Abmelden"))));
    }

    // ------------------------------------------------------------------
    // Sitzungszustand
    // ------------------------------------------------------------------

    const SITZUNG_ANFANG = {status: "pruefe", profil: null};

    function sitzungReducer(zustand, aktion) {
        switch (aktion.type) {
            case "angemeldet":
                return {status: "angemeldet", profil: aktion.profil};
            case "abgemeldet":
                return {status: "abgemeldet", profil: null};
            default:
                return zustand;
        }
    }

    function Anwendung() {
        const [sitzung, dispatch] = React.useReducer(sitzungReducer, SITZUNG_ANFANG);
        const route = verwendeRoute();

        // Beim Start pruefen, ob noch eine gueltige Session besteht. So bleibt
        // ein Neuladen der Seite ohne erneute Anmeldung moeglich.
        React.useEffect(() => {
            PP.api.sitzung()
                .then(profil => dispatch({type: "angemeldet", profil: profil}))
                .catch(() => dispatch({type: "abgemeldet"}));
        }, []);

        async function abmelden() {
            try {
                await PP.api.abmelden();
            } finally {
                dispatch({type: "abgemeldet"});
                window.location.hash = "#/projekte";
            }
        }

        if (sitzung.status === "pruefe") {
            return h(B.Ladeanzeige, {text: "Anwendung wird geladen …"});
        }
        if (sitzung.status === "abgemeldet") {
            return h(PP.Anmeldung, {
                beiAnmeldung: profil => dispatch({type: "angemeldet", profil: profil})
            });
        }

        const profil = sitzung.profil;

        let inhalt;
        if (route.ansicht === "benutzer") {
            // Zusaetzliche Absicherung der Navigation im Client; der Server
            // lehnt die Aufrufe ohnehin mit 403 ab.
            inhalt = profil.rollen.indexOf("ADMIN") >= 0
                ? h(PP.Benutzerverwaltung, {profil: profil})
                : h(B.Fehlerhinweis, {
                    titel: "Kein Zugriff",
                    text: "Die Benutzerverwaltung ist Administratoren vorbehalten."
                });
        } else if (route.ansicht === "projektdetail") {
            inhalt = h(PP.Projektdetail, {profil: profil, projektId: route.projektId});
        } else {
            inhalt = h(PP.Projektuebersicht, {profil: profil});
        }

        return h(React.Fragment, null,
            h("a", {href: "#hauptinhalt", className: "sprungmarke"}, "Zum Hauptinhalt springen"),
            h(Kopfzeile, {profil: profil, route: route, beiAbmeldung: abmelden}),
            h(MUI.Container, {
                id: "hauptinhalt", component: "main", maxWidth: "lg",
                sx: {py: {xs: 3, sm: 4}}
            }, inhalt),
            h(MUI.Box, {component: "footer", sx: {py: 3, textAlign: "center"}},
                h(MUI.Typography, {variant: "caption", color: "text.secondary"},
                    "Projektportal · Fallstudie DLBITOWAWBI01 · "
                    + "Spring Boot und React")));
    }

    // ------------------------------------------------------------------
    // Start
    // ------------------------------------------------------------------

    const wurzel = ReactDOM.createRoot(document.getElementById("root"));
    wurzel.render(
        h(MUI.ThemeProvider, {theme: theme},
            h(MUI.CssBaseline, null),
            h(Anwendung, null)));
})();
