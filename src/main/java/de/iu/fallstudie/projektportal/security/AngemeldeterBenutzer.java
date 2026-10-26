package de.iu.fallstudie.projektportal.security;

import de.iu.fallstudie.projektportal.domain.Benutzer;
import de.iu.fallstudie.projektportal.domain.Rolle;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Adapter zwischen dem Domaenenobjekt {@link Benutzer} und Spring Security.
 *
 * <p>Neben den Anmeldedaten transportiert das Objekt die technische Benutzer-ID
 * und die Mandanten-ID. Die Serviceschicht liest den Mandanten ausschliesslich
 * aus diesem Sicherheitskontext und niemals aus dem Request. Damit kann ein
 * Client seinen Mandanten nicht selbst bestimmen.</p>
 */
public class AngemeldeterBenutzer implements UserDetails {

    private final Long benutzerId;
    private final Long mandantId;
    private final String email;
    private final String passwortHash;
    private final String vollerName;
    private final boolean aktiv;
    private final List<GrantedAuthority> autoritaeten;

    public AngemeldeterBenutzer(Benutzer benutzer) {
        this.benutzerId = benutzer.getId();
        this.mandantId = benutzer.getMandant().getId();
        this.email = benutzer.getEmail();
        this.passwortHash = benutzer.getPasswortHash();
        this.vollerName = benutzer.getVollerName();
        this.aktiv = benutzer.isAktiv();
        this.autoritaeten = benutzer.getRollen().stream()
                .map(Rolle::name)
                // Spring Security erwartet das Praefix ROLE_, damit hasRole(...) greift
                .map(name -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + name))
                .toList();
    }

    public Long getBenutzerId() {
        return benutzerId;
    }

    public Long getMandantId() {
        return mandantId;
    }

    public String getVollerName() {
        return vollerName;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return autoritaeten;
    }

    @Override
    public String getPassword() {
        return passwortHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return aktiv;
    }
}
