package com.eventpro.service;

import com.eventpro.dto.user.UtilisateurResponseDTO;
import com.eventpro.entity.User;
import com.eventpro.exception.ResourceNotFoundException;
import com.eventpro.exception.ValidationException;
import com.eventpro.mapper.UtilisateurMapper;
import com.eventpro.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Common logic for ALL accounts (client, organisateur, administrateur, agent):
 * email check, password hashing, cleaning, global list, activate / deactivate.
 */
@Service
public class UtilisateurService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UtilisateurMapper utilisateurMapper;

    public UtilisateurService(UserRepository userRepository, PasswordEncoder passwordEncoder, UtilisateurMapper utilisateurMapper) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.utilisateurMapper = utilisateurMapper;
    }

    public void verifierEmailLibre(String email) {
        if (userRepository.existsByEmailIgnoreCase(email.trim())) {
            throw new ValidationException("Cet email est deja utilise");
        }
    }

    public void verifierEmailLibre(String email, Long idACompte) {
        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email.trim(), idACompte)) {
            throw new ValidationException("Cet email est deja utilise");
        }
    }

    public String hasher(String motDePasse) {
        return passwordEncoder.encode(motDePasse);
    }

    public void nettoyer(User user) {
        user.setNom(user.getNom().trim());
        user.setEmail(user.getEmail().trim().toLowerCase());
    }

    @Transactional(readOnly = true)
    public List<UtilisateurResponseDTO> findAll() {
        return userRepository.findAll(Sort.by("id")).stream().map(utilisateurMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UtilisateurResponseDTO findById(Long id) {
        return utilisateurMapper.toResponse(findUser(id));
    }

    @Transactional
    public UtilisateurResponseDTO changerActivation(Long id, boolean actif) {
        User user = findUser(id);
        user.setActif(actif);
        return utilisateurMapper.toResponse(user);
    }

    private User findUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + id));
    }
}
