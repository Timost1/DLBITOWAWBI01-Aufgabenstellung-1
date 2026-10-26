/*
 * Ansicht "Anmeldung".
 *
 * Erste Seite der Anwendung. Ohne gueltige Sitzung ist keine andere Ansicht
 * erreichbar; die Navigation wird erst nach erfolgreicher Anmeldung aufgebaut.
 */
(function () {
    "use strict";

    const PP = window.PP;
    const h = React.createElement;
    const MUI = window.MaterialUI;

    /** Zugaenge des Demodatenbestands - eine Hilfe fuer die Erstinbetriebnahme. */
    const DEMOZUGAENGE = [
        {rolle: "Administrator:in", email: "admin@nordlicht.example"},
        {rolle: "Projektleiter:in", email: "anna.behrens@nordlicht.example"},
        {rolle: "Mitarbeiter:in", email: "lea.novak@nordlicht.example"}
    ];

    const ANFANG = {status: "leer", fehler: null};

    /**
     * Zustandsmaschine des Anmeldevorgangs. Die vollstaendige Zustandsangabe je
     * Aktion verhindert, dass etwa der Ladezustand aktiv bleibt, waehrend schon
     * eine Fehlermeldung angezeigt wird.
     */
    function reducer(zustand, aktion) {
        switch (aktion.type) {
            case "senden":
                return {status: "laedt", fehler: null};
            case "erfolg":
                return {status: "erfolg", fehler: null};
            case "fehler":
                return {status: "fehler", fehler: aktion.fehler};
            default:
                return zustand;
        }
    }

    function Anmeldung(eigenschaften) {
        // Kontrollierte Eingabefelder: Der React-Zustand ist die einzige
        // Wahrheit, das DOM spiegelt ihn lediglich.
        const [email, setEmail] = React.useState("");
        const [passwort, setPasswort] = React.useState("");
        const [anfrage, dispatch] = React.useReducer(reducer, ANFANG);

        const laedt = anfrage.status === "laedt";

        async function absenden(ereignis) {
            ereignis.preventDefault();
            dispatch({type: "senden"});
            try {
                const profil = await PP.api.anmelden(email.trim(), passwort);
                dispatch({type: "erfolg"});
                // Das Kennwort wird nach dem Aufruf nicht weiter vorgehalten.
                setPasswort("");
                eigenschaften.beiAnmeldung(profil);
            } catch (fehler) {
                dispatch({type: "fehler", fehler: fehler});
            }
        }

        return h(MUI.Box, {
            sx: {
                minHeight: "100vh", display: "flex", alignItems: "center",
                justifyContent: "center", p: 2
            }
        },
            h(MUI.Paper, {elevation: 3, sx: {p: {xs: 3, sm: 4}, maxWidth: 460, width: "100%"}},

                h(MUI.Typography, {variant: "overline", color: "primary", fontWeight: 700},
                    "Projektportal"),
                h(MUI.Typography, {variant: "h5", component: "h1", sx: {mt: 0.5, mb: 0.5}},
                    "Anmeldung"),
                h(MUI.Typography, {variant: "body2", color: "text.secondary", sx: {mb: 3}},
                    "Projekt- und Aufgabenmanagement für Kundenprojekte."),

                anfrage.fehler
                    ? h(MUI.Alert, {severity: "error", sx: {mb: 2}},
                        anfrage.fehler.status === 401
                            ? "E-Mail-Adresse oder Kennwort ist nicht korrekt."
                            : anfrage.fehler.message)
                    : null,

                h("form", {onSubmit: absenden, noValidate: true},
                    h(MUI.TextField, {
                        label: "E-Mail-Adresse",
                        type: "email",
                        value: email,
                        onChange: ereignis => setEmail(ereignis.target.value),
                        fullWidth: true,
                        required: true,
                        autoComplete: "username",
                        autoFocus: true,
                        margin: "normal",
                        disabled: laedt
                    }),
                    h(MUI.TextField, {
                        label: "Kennwort",
                        type: "password",
                        value: passwort,
                        onChange: ereignis => setPasswort(ereignis.target.value),
                        fullWidth: true,
                        required: true,
                        autoComplete: "current-password",
                        margin: "normal",
                        disabled: laedt
                    }),
                    h(MUI.Button, {
                        type: "submit",
                        variant: "contained",
                        size: "large",
                        fullWidth: true,
                        sx: {mt: 3},
                        disabled: laedt || email.trim() === "" || passwort === "",
                        startIcon: laedt
                            ? h(MUI.CircularProgress, {size: 18, color: "inherit"})
                            : null
                    }, laedt ? "Anmeldung läuft …" : "Anmelden")),

                h(MUI.Divider, {sx: {my: 3}}),

                h(MUI.Typography, {variant: "subtitle2", gutterBottom: true},
                    "Zugänge des Demodatenbestands"),
                h(MUI.Table, {size: "small"},
                    h(MUI.TableBody, null,
                        DEMOZUGAENGE.map(zugang =>
                            h(MUI.TableRow, {key: zugang.email},
                                h(MUI.TableCell, {sx: {pl: 0, border: 0, whiteSpace: "nowrap"}},
                                    zugang.rolle),
                                h(MUI.TableCell, {sx: {border: 0, fontFamily: "monospace",
                                    fontSize: 13}}, zugang.email))))),
                h(MUI.Typography, {variant: "caption", color: "text.secondary",
                    sx: {display: "block", mt: 1}},
                    "Kennwort für alle Demokonten: Fallstudie2026!")));
    }

    PP.Anmeldung = Anmeldung;
})();
