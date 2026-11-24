package com.eventpro.service;

import com.eventpro.dto.event.EvenementAvecPromotionsDTO;
import com.eventpro.dto.promotion.PromotionRequestDTO;
import com.eventpro.dto.promotion.PromotionResponseDTO;
import com.eventpro.dto.promotion.PromotionUpdateDTO;
import com.eventpro.entity.Evenement;
import com.eventpro.entity.Promotion;
import com.eventpro.enums.StatutEvenement;
import com.eventpro.enums.TypeReduction;
import com.eventpro.exception.ResourceNotFoundException;
import com.eventpro.exception.ValidationException;
import com.eventpro.mapper.PromotionMapper;
import com.eventpro.repository.EvenementRepository;
import com.eventpro.repository.PromotionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PromotionServiceTest {

    @Mock PromotionRepository promotionRepository;
    @Mock EvenementRepository evenementRepository;
    @Mock PromotionMapper promotionMapper;

    @InjectMocks PromotionService service;

    // ---------- create ----------

    @Test
    void create_evenementInconnu_404() {
        when(evenementRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.create(1L, mock(PromotionRequestDTO.class)));
    }

    @Test
    void create_evenementAnnule_409() {
        Evenement e = mock(Evenement.class);
        when(evenementRepository.findById(1L)).thenReturn(Optional.of(e));
        when(e.getStatut()).thenReturn(StatutEvenement.ANNULE);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.create(1L, mock(PromotionRequestDTO.class)));
        assertEquals(409, ex.getStatusCode().value());
    }

    @Test
    void create_codeDejaExistant_409() {
        Evenement e = mock(Evenement.class);
        PromotionRequestDTO dto = mock(PromotionRequestDTO.class);
        when(evenementRepository.findById(1L)).thenReturn(Optional.of(e));
        when(e.getStatut()).thenReturn(StatutEvenement.PUBLIE);
        when(dto.code()).thenReturn(" sum20 ");
        when(promotionRepository.existsByCodeIgnoreCaseAndEvenementId("SUM20", 1L)).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> service.create(1L, dto));
        verify(promotionRepository, never()).save(any());
    }

    @Test
    void create_finApresLEvenement_validationException() {
        Evenement e = mock(Evenement.class);
        PromotionRequestDTO dto = mock(PromotionRequestDTO.class);
        when(evenementRepository.findById(1L)).thenReturn(Optional.of(e));
        when(e.getStatut()).thenReturn(StatutEvenement.PUBLIE);
        when(e.getDateFin()).thenReturn(LocalDateTime.now().plusDays(1));
        when(dto.code()).thenReturn("SUM20");
        when(dto.dateFin()).thenReturn(LocalDate.now().plusDays(10));

        assertThrows(ValidationException.class, () -> service.create(1L, dto));
    }

    @Test
    void create_valide_sauvegarde() {
        Evenement e = mock(Evenement.class);
        PromotionRequestDTO dto = mock(PromotionRequestDTO.class);
        Promotion promo = mock(Promotion.class);
        PromotionResponseDTO response = mock(PromotionResponseDTO.class);
        when(evenementRepository.findById(1L)).thenReturn(Optional.of(e));
        when(e.getStatut()).thenReturn(StatutEvenement.PUBLIE);
        when(dto.code()).thenReturn("sum20");
        when(promotionMapper.toEntity(dto)).thenReturn(promo);
        when(promotionRepository.save(promo)).thenReturn(promo);
        when(promotionMapper.toResponse(promo)).thenReturn(response);

        assertSame(response, service.create(1L, dto));
        verify(promo).setEvenement(e);
    }

    // ---------- lectures ----------

    @Test
    void findByEvenement_ok() {
        List<Promotion> liste = List.of(mock(Promotion.class));
        List<PromotionResponseDTO> dtos = List.of(mock(PromotionResponseDTO.class));
        when(evenementRepository.findById(1L)).thenReturn(Optional.of(mock(Evenement.class)));
        when(promotionRepository.findByEvenementId(1L)).thenReturn(liste);
        when(promotionMapper.toResponseList(liste)).thenReturn(dtos);

        assertSame(dtos, service.findByEvenement(1L));
    }

    @Test
    void findOne_inconnue_404() {
        when(promotionRepository.findByIdAndEvenementId(5L, 1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.findOne(1L, 5L));
    }

    @Test
    void findOne_ok() {
        Promotion p = mock(Promotion.class);
        PromotionResponseDTO dto = mock(PromotionResponseDTO.class);
        when(promotionRepository.findByIdAndEvenementId(5L, 1L)).thenReturn(Optional.of(p));
        when(promotionMapper.toResponse(p)).thenReturn(dto);

        assertSame(dto, service.findOne(1L, 5L));
    }

    @Test
    void findActives_ok() {
        List<Promotion> liste = List.of(mock(Promotion.class));
        List<PromotionResponseDTO> dtos = List.of(mock(PromotionResponseDTO.class));
        when(evenementRepository.findById(1L)).thenReturn(Optional.of(mock(Evenement.class)));
        when(promotionRepository.findActives(eq(1L), any(LocalDate.class))).thenReturn(liste);
        when(promotionMapper.toResponseList(liste)).thenReturn(dtos);

        assertSame(dtos, service.findActives(1L));
    }

    @Test
    void verifierCode_inconnu_404() {
        when(promotionRepository.findByCodeIgnoreCaseAndEvenementId("X", 1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.verifierCode(1L, " X "));
    }

    @Test
    void verifierCode_nonUtilisable_409() {
        Promotion p = mock(Promotion.class);
        when(promotionRepository.findByCodeIgnoreCaseAndEvenementId("X", 1L)).thenReturn(Optional.of(p));
        when(p.estUtilisable()).thenReturn(false);

        assertThrows(ResponseStatusException.class, () -> service.verifierCode(1L, "X"));
    }

    @Test
    void verifierCode_valide() {
        Promotion p = mock(Promotion.class);
        PromotionResponseDTO dto = mock(PromotionResponseDTO.class);
        when(promotionRepository.findByCodeIgnoreCaseAndEvenementId("X", 1L)).thenReturn(Optional.of(p));
        when(p.estUtilisable()).thenReturn(true);
        when(promotionMapper.toResponse(p)).thenReturn(dto);

        assertSame(dto, service.verifierCode(1L, "X"));
    }

    @Test
    void findByOrganisateur_etEvenementsAvecPromotions_deleguent() {
        List<Promotion> liste = List.of(mock(Promotion.class));
        List<PromotionResponseDTO> dtos = List.of(mock(PromotionResponseDTO.class));
        List<Evenement> evenements = List.of(mock(Evenement.class));
        List<EvenementAvecPromotionsDTO> evDtos = List.of(mock(EvenementAvecPromotionsDTO.class));
        when(promotionRepository.findByOrganisateurId(4L)).thenReturn(liste);
        when(promotionMapper.toResponseList(liste)).thenReturn(dtos);
        when(evenementRepository.findAllWithPromotions()).thenReturn(evenements);
        when(promotionMapper.toEvenementAvecPromotionsList(evenements)).thenReturn(evDtos);

        assertSame(dtos, service.findByOrganisateur(4L));
        assertSame(evDtos, service.findEvenementsAvecPromotions());
    }

    // ---------- update ----------

    @Test
    void update_codeDejaPris_409() {
        Promotion p = mock(Promotion.class);
        PromotionUpdateDTO dto = mock(PromotionUpdateDTO.class);
        when(promotionRepository.findByIdAndEvenementId(5L, 1L)).thenReturn(Optional.of(p));
        when(dto.code()).thenReturn("abc");
        when(promotionRepository.existsByCodeIgnoreCaseAndEvenementIdAndIdNot("ABC", 1L, 5L)).thenReturn(true);

        assertThrows(ResponseStatusException.class, () -> service.update(1L, 5L, dto));
    }

    @Test
    void update_limiteInferieureAuxUtilisations_validationException() {
        Promotion p = mock(Promotion.class);
        PromotionUpdateDTO dto = mock(PromotionUpdateDTO.class);
        when(promotionRepository.findByIdAndEvenementId(5L, 1L)).thenReturn(Optional.of(p));
        when(dto.code()).thenReturn("abc");
        when(dto.limiteUtilisation()).thenReturn(3);
        when(p.getNombreUtilisations()).thenReturn(10);

        assertThrows(ValidationException.class, () -> service.update(1L, 5L, dto));
    }

    @Test
    void update_reductionModifieeAlorsQuePromoDejaUtilisee_409() {
        Promotion p = mock(Promotion.class);
        PromotionUpdateDTO dto = mock(PromotionUpdateDTO.class);
        when(promotionRepository.findByIdAndEvenementId(5L, 1L)).thenReturn(Optional.of(p));
        when(dto.code()).thenReturn("abc");
        when(dto.limiteUtilisation()).thenReturn(100);
        when(dto.typeReduction()).thenReturn(TypeReduction.POURCENTAGE);
        when(p.getNombreUtilisations()).thenReturn(5);
        when(p.getTypeReduction()).thenReturn(TypeReduction.MONTANT_FIXE);

        assertThrows(ResponseStatusException.class, () -> service.update(1L, 5L, dto));
        verify(promotionMapper, never()).updateEntity(any(), any());
    }

    @Test
    void update_valide_appliqueLesChangements() {
        Promotion p = mock(Promotion.class);
        Evenement e = mock(Evenement.class);
        PromotionUpdateDTO dto = mock(PromotionUpdateDTO.class);
        PromotionResponseDTO response = mock(PromotionResponseDTO.class);
        when(promotionRepository.findByIdAndEvenementId(5L, 1L)).thenReturn(Optional.of(p));
        when(dto.code()).thenReturn("abc");
        when(dto.limiteUtilisation()).thenReturn(100);
        when(dto.typeReduction()).thenReturn(TypeReduction.POURCENTAGE);
        when(dto.valeurReduction()).thenReturn(new BigDecimal("20"));
        when(p.getNombreUtilisations()).thenReturn(0);
        when(p.getTypeReduction()).thenReturn(TypeReduction.POURCENTAGE);
        when(p.getValeurReduction()).thenReturn(new BigDecimal("20.00"));
        when(p.getEvenement()).thenReturn(e);
        when(promotionMapper.toResponse(p)).thenReturn(response);

        assertSame(response, service.update(1L, 5L, dto));
        verify(promotionMapper).updateEntity(dto, p);
    }

    // ---------- delete ----------

    @Test
    void delete_dejaUtilisee_409() {
        Promotion p = mock(Promotion.class);
        when(promotionRepository.findByIdAndEvenementId(5L, 1L)).thenReturn(Optional.of(p));
        when(p.getNombreUtilisations()).thenReturn(2);

        assertThrows(ResponseStatusException.class, () -> service.delete(1L, 5L));
        verify(promotionRepository, never()).delete(any());
    }

    @Test
    void delete_jamaisUtilisee_supprime() {
        Promotion p = mock(Promotion.class);
        when(promotionRepository.findByIdAndEvenementId(5L, 1L)).thenReturn(Optional.of(p));
        when(p.getNombreUtilisations()).thenReturn(0);

        service.delete(1L, 5L);

        verify(promotionRepository).delete(p);
    }
}
