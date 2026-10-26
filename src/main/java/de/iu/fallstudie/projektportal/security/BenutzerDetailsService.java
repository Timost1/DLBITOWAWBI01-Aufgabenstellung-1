package de.iu.fallstudie.projektportal.security;

import de.iu.fallstudie.projektportal.repository.BenutzerRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Laedt Anmeldedaten aus der Datenbank. Spring Security nutzt diesen Dienst
 * waehrend der Authentifizierung und vergleicht das uebergebene Kennwort
 * anschliessend mit dem gespeicherten BCrypt-Hash.
 */
@Service
public class BenutzerDetailsService implements UserDetailsService {

    private final BenutzerRepository benutzerRepository;

    public BenutzerDetailsService(BenutzerRepository benutzerRepository) {
        this.benutzerRepository = benutzerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return benutzerRepository.findByEmail(email)
                .map(AngemeldeterBenutzer::new)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Kein Benutzerkonto zur angegebenen E-Mail-Adresse gefunden."));
    }
}
