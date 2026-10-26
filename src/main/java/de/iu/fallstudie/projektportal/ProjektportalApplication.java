package de.iu.fallstudie.projektportal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Einstiegspunkt der Anwendung.
 *
 * <p>{@code SpringApplication.run(...)} startet den in Spring Boot eingebetteten
 * Webserver (Tomcat) und initialisiert den Anwendungskontext. Alle weiteren
 * Bausteine (Controller, Services, Repositories) werden ueber Component Scanning
 * im Paket {@code de.iu.fallstudie.projektportal} gefunden.</p>
 */
@SpringBootApplication
public class ProjektportalApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProjektportalApplication.class, args);
    }
}
