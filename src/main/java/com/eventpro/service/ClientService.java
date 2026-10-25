package com.eventpro.service;

import com.eventpro.dto.ClientRegistrationDTO;
import com.eventpro.entity.Client;
import com.eventpro.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ClientService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public Client registerClient(ClientRegistrationDTO dto) {

        if (userRepository.existsByEmail(dto.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        Client client = new Client();
        client.setNom(dto.nom());
        client.setEmail(dto.email());
        client.setMotDePasse(passwordEncoder.encode(dto.motDePasse()));
        client.setTelephone(dto.telephone());

        return userRepository.save(client);
    }
}