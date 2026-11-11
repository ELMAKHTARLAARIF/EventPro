package com.eventpro.service;

import com.eventpro.dto.user.ClientRequestDTO;
import com.eventpro.dto.user.ClientUpdateDTO;
import com.eventpro.dto.user.UtilisateurResponseDTO;
import com.eventpro.entity.Client;
import com.eventpro.exception.ResourceNotFoundException;
import com.eventpro.mapper.UtilisateurMapper;
import com.eventpro.repository.ClientRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClientService {

    private final ClientRepository clientRepository;
    private final UtilisateurService utilisateurService;
    private final UtilisateurMapper utilisateurMapper;

    public ClientService(ClientRepository clientRepository, UtilisateurService utilisateurService, UtilisateurMapper utilisateurMapper) {
        this.clientRepository = clientRepository;
        this.utilisateurService = utilisateurService;
        this.utilisateurMapper = utilisateurMapper;
    }

    // ---------- CREATE ----------
    @Transactional
    public UtilisateurResponseDTO create(ClientRequestDTO dto) {

        utilisateurService.verifierEmailLibre(dto.email());

        Client client = utilisateurMapper.toClient(dto);       // nom, email, telephone
        client.setMotDePasse(utilisateurService.hasher(dto.motDePasse()));
        utilisateurService.nettoyer(client);

        return utilisateurMapper.toResponse(clientRepository.save(client));
    }

    // ---------- READ ----------
    @Transactional(readOnly = true)
    public List<UtilisateurResponseDTO> findAll() {
        return clientRepository.findAll(Sort.by("id")).stream().map(utilisateurMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public UtilisateurResponseDTO findById(Long id) {
        return utilisateurMapper.toResponse(findClient(id));
    }

    // ---------- UPDATE ----------
    @Transactional
    public UtilisateurResponseDTO update(Long id, ClientUpdateDTO dto) {

        Client client = findClient(id);
        utilisateurService.verifierEmailLibre(dto.email(), id);

        utilisateurMapper.updateClient(dto, client);
        utilisateurService.nettoyer(client);

        // no save() needed: the entity is managed
        return utilisateurMapper.toResponse(client);
    }

    // no delete: a client has reservations and tickets -> use UtilisateurService.changerActivation(id, false)

    private Client findClient(Long id) {
        return clientRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Client introuvable : " + id));
    }
}
