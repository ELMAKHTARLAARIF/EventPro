package com.eventpro.service;

import com.eventpro.dto.LigneReservationRequestDTO;
import com.eventpro.dto.ReservationRequestDTO;
import com.eventpro.dto.ReservationResponseDTO;
import com.eventpro.dto.ReservationUpdateDTO;
import com.eventpro.entity.*;
import com.eventpro.enums.StatutEvenement;
import com.eventpro.enums.StatutReservation;
import com.eventpro.mapper.ReservationMapper;
import com.eventpro.repository.ClientRepository;
import com.eventpro.repository.EvenementRepository;
import com.eventpro.repository.PromotionRepository;
import com.eventpro.repository.ReservationRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;

@Service
public class ReservationService {

    private static final long DUREE_BLOCAGE_MINUTES = 10;   // cahier: 10 minutes

    private static final List<StatutReservation> STATUTS_ACTIFS = List.of(StatutReservation.EN_ATTENTE, StatutReservation.CONFIRMEE);

    private final ReservationRepository reservationRepository;
    private final EvenementRepository evenementRepository;
    private final ClientRepository clientRepository;
    private final PromotionRepository promotionRepository;
    private final CategorieBilletService categorieService;
    private final PaymentService paymentService;
    private final TicketService ticketService;
    private final ReservationMapper reservationMapper;

    public ReservationService(ReservationRepository reservationRepository, EvenementRepository evenementRepository, ClientRepository clientRepository, PromotionRepository promotionRepository, CategorieBilletService categorieService, PaymentService paymentService, TicketService ticketService, ReservationMapper reservationMapper) {
        this.reservationRepository = reservationRepository;
        this.evenementRepository = evenementRepository;
        this.clientRepository = clientRepository;
        this.promotionRepository = promotionRepository;
        this.categorieService = categorieService;
        this.paymentService = paymentService;
        this.ticketService = ticketService;
        this.reservationMapper = reservationMapper;
    }

    // ---------- CREATE ----------
    @Transactional
    public ReservationResponseDTO create(ReservationRequestDTO dto) {

        Evenement evenement = evenementRepository.findById(dto.evenementId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Evenement introuvable : " + dto.evenementId()));
        verifierVentesOuvertes(evenement);

        Client client = clientRepository.findById(dto.clientId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client introuvable : " + dto.clientId()));
        if (!client.isActif()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce compte est desactive");
        }

        // stock: locked by CategorieBilletService
        Map<Long, CategorieBillet> categories = categorieService.verrouiller(idsDe(dto.lignes()), evenement.getId());

        Reservation reservation = new Reservation();
        reservation.setReference(genererReference());
        reservation.setClient(client);
        reservation.setEvenement(evenement);
        reservation.setStatut(StatutReservation.EN_ATTENTE);
        reservation.setDateCreation(LocalDateTime.now());
        reservation.setDateExpiration(reservation.getDateCreation().plusMinutes(DUREE_BLOCAGE_MINUTES));

        ajouterLignes(reservation, dto.lignes(), categories);
        appliquerPromotion(reservation, dto.codePromo());

        Reservation saved = reservationRepository.save(reservation);
        confirmer(saved);

        return reservationMapper.toResponse(saved);
    }

    // ---------- UPDATE ----------
    @Transactional
    public ReservationResponseDTO update(Long reservationId, ReservationUpdateDTO dto) {

        // lock the reservation first, then the categories (always this order)
        Reservation reservation = reservationRepository.findByIdForUpdate(reservationId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reservation introuvable : " + reservationId));

        if (reservation.getStatut() != StatutReservation.EN_ATTENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seule une reservation en attente de paiement peut etre modifiee");
        }
        if (LocalDateTime.now().isAfter(reservation.getDateExpiration())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette reservation a expire, creez-en une nouvelle");
        }

        Evenement evenement = reservation.getEvenement();
        verifierVentesOuvertes(evenement);

        // lock old AND new categories together
        Set<Long> ids = new LinkedHashSet<>(idsDe(dto.lignes()));
        reservation.getLignes().forEach(l -> ids.add(l.getCategorie().getId()));
        Map<Long, CategorieBillet> categories = categorieService.verrouiller(ids, evenement.getId());

        // 1. release the old places and the old promo use
        for (LigneReservation ancienne : reservation.getLignes()) {
            categorieService.libererPlaces(categories.get(ancienne.getCategorie().getId()), ancienne.getQuantite());
        }
        libererPromotion(reservation);
        reservation.getLignes().clear();   // orphanRemoval deletes the old lines

        // 2. apply the new lines and promo (same rules as create)
        ajouterLignes(reservation, dto.lignes(), categories);
        appliquerPromotion(reservation, dto.codePromo());

        // no save() needed: the reservation is managed
        return reservationMapper.toResponse(reservation);
    }

    // ---------- READ ----------
    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> findAllByEvenementId(Long evenementId) {

        if (!evenementRepository.existsById(evenementId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Evenement introuvable : " + evenementId);
        }

        return reservationMapper.toResponseList(reservationRepository.findByEvenementIdOrderByDateCreationDesc(evenementId));
    }

    // ---------- coordination: payment, stock sold, tickets ----------
    private void confirmer(Reservation reservation) {

        paymentService.creerPaiement(reservation);

        for (LigneReservation ligne : reservation.getLignes()) {
            categorieService.confirmerVente(ligne.getCategorie(), ligne.getQuantite());
        }

        ticketService.genererBillets(reservation);

        reservation.setStatut(StatutReservation.CONFIRMEE);
    }

    // ---------- reservation rules ----------
    private List<Long> idsDe(List<LigneReservationRequestDTO> lignes) {
        return lignes.stream().map(LigneReservationRequestDTO::categorieId).toList();
    }

    private void ajouterLignes(Reservation reservation, List<LigneReservationRequestDTO> lignes, Map<Long, CategorieBillet> categories) {

        Long clientId = reservation.getClient().getId();
        // a new reservation has no id yet: 0 excludes nothing
        Long exclure = reservation.getId() == null ? 0L : reservation.getId();

        for (LigneReservationRequestDTO l : lignes) {

            CategorieBillet categorie = categories.get(l.categorieId());
            if (categorie == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La categorie " + l.categorieId() + " n'appartient pas a cet evenement");
            }
            int quantite = l.quantite();

            // quota per client: a reservation rule
            long dejaReservee = reservationRepository.quantiteDejaReservee(clientId, categorie.getId(), STATUTS_ACTIFS, exclure);
            if (dejaReservee + quantite > categorie.getQuotaMaxParClient()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quota depasse pour la categorie " + categorie.getNom() + " : maximum " + categorie.getQuotaMaxParClient() + " par client (vous en avez deja " + dejaReservee + " dans d'autres reservations)");
            }

            // stock: handled by CategorieBilletService
            categorieService.reserverPlaces(categorie, quantite);

            LigneReservation ligne = new LigneReservation();
            ligne.setCategorie(categorie);
            ligne.setQuantite(quantite);
            ligne.setPrixUnitaire(categorie.getPrixUnitaire());
            ligne.setSousTotal(categorie.getPrixUnitaire().multiply(BigDecimal.valueOf(quantite)));
            reservation.addLigne(ligne);
        }
    }

    private void appliquerPromotion(Reservation reservation, String codePromo) {

        BigDecimal sousTotal = reservation.calculerTotal();
        BigDecimal remise = BigDecimal.ZERO;

        if (codePromo != null && !codePromo.isBlank()) {
            Promotion promotion = promotionRepository.findWithLockByCodeIgnoreCaseAndEvenementId(codePromo.trim(), reservation.getEvenement().getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Code promo inconnu pour cet evenement"));

            if (!promotion.estUtilisable()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Ce code promo n'est plus valide (expire ou limite atteinte)");
            }

            remise = promotion.calculerReduction(sousTotal);
            promotion.setNombreUtilisations(promotion.getNombreUtilisations() + 1);
            reservation.setPromotion(promotion);
        }

        reservation.setMontantTotal(sousTotal.subtract(remise).max(BigDecimal.ZERO));
    }

    // gives back the promo use taken by this reservation
    private void libererPromotion(Reservation reservation) {
        Promotion ancienne = reservation.getPromotion();
        if (ancienne != null) {
            promotionRepository.findWithLockByCodeIgnoreCaseAndEvenementId(ancienne.getCode(), reservation.getEvenement().getId()).ifPresent(p -> p.setNombreUtilisations(Math.max(0, p.getNombreUtilisations() - 1)));
            reservation.setPromotion(null);
        }
    }

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