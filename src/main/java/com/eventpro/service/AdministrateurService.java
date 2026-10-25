package com.eventpro.service;

import com.eventpro.dto.AdministrateurRegistrationDTO;
import com.eventpro.entity.Administrateur;
import com.eventpro.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdministrateurService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdministrateurService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Administrateur createAdministrateur(AdministrateurRegistrationDTO dto) {

        if (userRepository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        Administrateur administrateur = new Administrateur();

        administrateur.setNom(dto.nom());
        administrateur.setEmail(dto.email());
        administrateur.setMotDePasse(passwordEncoder.encode(dto.motDePasse()));

        return userRepository.save(administrateur);
    }
}