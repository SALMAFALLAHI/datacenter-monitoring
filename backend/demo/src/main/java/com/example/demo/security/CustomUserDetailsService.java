package com.example.demo.security;

import com.example.demo.entity.Administrateur;
import com.example.demo.repository.AdministrateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final AdministrateurRepository administrateurRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Administrateur user = administrateurRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur non trouvé : " + email));

        if (!user.isActif()) {
            throw new UsernameNotFoundException("Utilisateur désactivé");
        }

        String roleName = user.getRole() != null ? user.getRole() : "ADMIN";

        return User.builder()
                .username(user.getEmail())
                .password(user.getMotDePasse())
                .authorities(new SimpleGrantedAuthority("ROLE_" + roleName))
                .build();
    }
}