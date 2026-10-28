package com.eventpro.service;

import com.eventpro.dto.LigneReservationRequestDTO;
import com.eventpro.dto.ReservationRequestDTO;
import com.eventpro.dto.ReservationResponseDTO;
import com.eventpro.entity.*;
import com.eventpro.enums.StatutEvenement;
import com.eventpro.enums.StatutReservation;
import com.eventpro.mapper.ReservationMapper;
import com.eventpro.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private static final long DUREE_BLOCAGE_MINUTES = 10;   // cahier: 10 minutes

    private static final List<StatutReservation> STATUTS_ACTIFS = List.of(StatutReservation.EN_ATTENTE, StatutReservation.CONFIRMEE);

    private final ReservationRepository reservationRepository;
    private final EvenementRepository evenementRepository;
    private final ClientRepository clientRepository;
    private final CategorieBilletRepository categorieRepository;
    private final PromotionRepository promotionRepository;
    private final ReservationMapper reservationMapper;

    public ReservationService(ReservationRepository reservationRepository, EvenementRepository evenementRepository, ClientRepository clientRepository, CategorieBilletRepository categorieRepository, PromotionRepository promotionRepository, ReservationMapper reservationMapper) {
        this.reservationRepository = reservationRepository;
        this.evenementRepository = evenementRepository;
        this.clientRepository = clientRepository;
        this.categorieRepository = categorieRepository;
        this.promotionRepository = promotionRepository;
        this.reservationMapper = reservationMapper;
    }

    // ---------- CREATE ----------
    // everything below runs in ONE transaction: the locks are released at the commit
    @Transactional
    public ReservationResponseDTO create(ReservationRequestDTO dto) {

        // event and client
        Evenement evenement = evenementRepository.findById(dto.evenementId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evenement introuvable : " + dto.evenementId()));
        verifierVentesOuvertes(evenement);

        Client client = clientRepository.findById(dto.clientId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable : " + dto.clientId()));
        if (!client.isActif()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce compte est desactive");
        }

        // lock all requested categories (SELECT ... FOR UPDATE)
        List<Long> ids = dto.lignes().stream().map(LigneReservationRequestDTO::categorieId).toList();

        Map<Long, CategorieBillet> categories = categorieRepository.findAllForUpdate(ids, evenement.getId()).stream().collect(Collectors.toMap(CategorieBillet::getId, Function.identity()));

        // build the reservation and its lines
        Reservation reservation = new Reservation();
        reservation.setReference(genererReference());
        reservation.setClient(client);
        reservation.setEvenement(evenement);
        reservation.setStatut(StatutReservation.EN_ATTENTE);
        reservation.setDateCreation(LocalDateTime.now());
        reservation.setDateExpiration(reservation.getDateCreation().plusMinutes(DUREE_BLOCAGE_MINUTES));

        for (LigneReservationRequestDTO l : dto.lignes()) {
                  // verefie categoryBillet for event
            CategorieBillet categorie = categories.get(l.categorieId());
            if (categorie == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La categorie " + l.categorieId() + " n'appartient pas a cet evenement");
            }
            int quantite = l.quantite();

            // quota per client (counts pending and confirmed reservations)
            long dejaReservee = reservationRepository.quantiteDejaReservee(client.getId(), categorie.getId(), STATUTS_ACTIFS);
            if (dejaReservee + quantite > categorie.getQuotaMaxParClient()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quota depasse pour la categorie " + categorie.getNom() + " : maximum " + categorie.getQuotaMaxParClient() + " par client (vous en avez deja " + dejaReservee + ")");
            }

            // stock: placesDisponibles = quantiteTotal - (placesReservees + placesVendues)
            if (quantite > categorie.placesDisponibles()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock insuffisant pour la categorie " + categorie.getNom() + " : " + categorie.placesDisponibles() + " place(s) disponible(s)");
            }
            // calcule new place reserve
            categorie.setPlacesReservees(categorie.getPlacesReservees() + quantite);

            // line: the price is copied from the category, never taken from the client
            LigneReservation ligne = new LigneReservation();
            ligne.setCategorie(categorie);
            ligne.setQuantite(quantite);
            ligne.setPrixUnitaire(categorie.getPrixUnitaire());
            ligne.setSousTotal(categorie.getPrixUnitaire().multiply(BigDecimal.valueOf(quantite)));
            reservation.addLigne(ligne);
        }

        // promotion
        BigDecimal sousTotal = reservation.calculerTotal();
        BigDecimal remise = BigDecimal.ZERO;

        if (dto.codePromo() != null && !dto.codePromo().isBlank()) {
            Promotion promotion = promotionRepository.findWithLockByCodeIgnoreCaseAndEvenementId(dto.codePromo().trim(), evenement.getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Code promo inconnu pour cet evenement"));

            if (!promotion.estUtilisable()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce code promo n'est plus valide (expire ou limite atteinte)");
            }

            remise = promotion.calculerReduction(sousTotal);
            promotion.setNombreUtilisations(promotion.getNombreUtilisations() + 1);
            reservation.setPromotion(promotion);
        }

        // total = sousTotal - remise, never below 0
        reservation.setMontantTotal(sousTotal.subtract(remise).max(BigDecimal.ZERO));

        return reservationMapper.toResponse(reservationRepository.save(reservation));
    }

    // ---------- READ ----------
    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> findAllByEvenementId(Long evenementId) {

        if (!evenementRepository.existsById(evenementId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evenement introuvable : " + evenementId);
        }

        return reservationMapper.toResponseList(reservationRepository.findByEvenementIdOrderByDateCreationDesc(evenementId));
    }

    // ---------- helpers ----------
    private void verifierVentesOuvertes(Evenement evenement) {
        LocalDateTime now = LocalDateTime.now();

        if (evenement.getStatut() != StatutEvenement.PUBLIE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet evenement n'est pas ouvert a la reservation");
        }
        if (now.isBefore(evenement.getDateOuvertureVente()) || now.isAfter(evenement.getDateClotureVente())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Les ventes ne sont pas ouvertes pour cet evenement");
        }
    }

    private String genererReference() {
        return "RES-" + Year.now().getValue() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}