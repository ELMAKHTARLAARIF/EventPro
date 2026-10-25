package com.eventpro.service;

import com.eventpro.dto.OrganisateurRegistrationDTO;
import com.eventpro.entity.Organisateur;
import com.eventpro.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrganisateurService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public OrganisateurService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Organisateur registerOrganisateur(OrganisateurRegistrationDTO dto) {

        if (userRepository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        Organisateur organisateur = new Organisateur();
        organisateur.setNom(dto.nom());
        organisateur.setEmail(dto.email());
        organisateur.setMotDePasse(passwordEncoder.encode(dto.motDePasse()));
        organisateur.setNomOrganisation(dto.nomOrganisation());

        return userRepository.save(organisateur);
    }
}