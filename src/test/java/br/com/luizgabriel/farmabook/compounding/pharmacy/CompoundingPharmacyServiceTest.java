package br.com.luizgabriel.farmabook.compounding.pharmacy;

import br.com.luizgabriel.farmabook.commons.CompoundingPharmacyUtils;
import br.com.luizgabriel.farmabook.exception.NotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class CompoundingPharmacyServiceTest {

    @InjectMocks
    private CompoundingPharmacyService service;

    @InjectMocks
    private CompoundingPharmacyUtils utils;

    @Mock
    private CompoundingPharmacyRepository repository;

    @Mock
    private CompoundingPharmacyMapper mapper;

    @Test
    @DisplayName("findAll should return a page of pharmacies when successful")
    void findAll_ReturnsPageOfPharmacies_WhenSuccessful() {
        var pageable = Pageable.ofSize(10);
        var pharmacy = utils.newPharmacy();
        var response = utils.newPharmacyGetResponse(pharmacy);
        var page = new PageImpl<>(List.of(pharmacy));

        BDDMockito.when(repository.findAll(pageable)).thenReturn(page);
        BDDMockito.when(mapper.toCompoundingPharmacyGetResponse(pharmacy)).thenReturn(response);

        var result = service.findAll(pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    @DisplayName("findAll should return an empty page when no pharmacies exist")
    void findAll_ReturnsEmptyPage_WhenNoPharmaciesExist() {
        var pageable = Pageable.ofSize(10);

        BDDMockito.when(repository.findAll(pageable)).thenReturn(Page.empty());

        var result = service.findAll(pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("findById should return CompoundingPharmacyGetResponse when found")
    void findById_ReturnsGetResponse_WhenSuccessful() {
        var pharmacy = utils.newPharmacy();
        var response = utils.newPharmacyGetResponse(pharmacy);

        BDDMockito.when(repository.findById(pharmacy.getId())).thenReturn(Optional.of(pharmacy));
        BDDMockito.when(mapper.toCompoundingPharmacyGetResponse(pharmacy)).thenReturn(response);

        var result = service.findById(pharmacy.getId());

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("findById should throw NotFoundException when pharmacy is not found")
    void findById_ThrowsNotFoundException_WhenNotFound() {
        var id = UUID.randomUUID();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("save should return CompoundingPharmacyPostResponse when successful")
    void save_ReturnsPostResponse_WhenSuccessful() {
        var request = utils.newPharmacyPostRequest();
        var pharmacy = utils.newPharmacy();
        var response = utils.newPharmacyPostResponse(pharmacy);

        BDDMockito.when(mapper.toCompoundingPharmacy(request)).thenReturn(pharmacy);
        BDDMockito.when(repository.save(pharmacy)).thenReturn(pharmacy);
        BDDMockito.when(mapper.toCompoundingPharmacyPostResponse(pharmacy)).thenReturn(response);

        var result = service.save(request);

        assertThat(result).isEqualTo(response);
        BDDMockito.then(repository).should().save(pharmacy);
    }

    @Test
    @DisplayName("update should return CompoundingPharmacyPutResponse when successful")
    void update_ReturnsPutResponse_WhenSuccessful() {
        var pharmacy = utils.newPharmacy();
        var request = utils.newPharmacyPutRequest();
        var response = utils.newPharmacyPutResponse(pharmacy);

        BDDMockito.when(repository.findById(pharmacy.getId())).thenReturn(Optional.of(pharmacy));
        BDDMockito.when(repository.save(pharmacy)).thenReturn(pharmacy);
        BDDMockito.when(mapper.toCompoundingPharmacyPutResponse(pharmacy)).thenReturn(response);

        var result = service.update(pharmacy.getId(), request);

        assertThat(result).isEqualTo(response);
        BDDMockito.then(repository).should().save(pharmacy);
    }

    @Test
    @DisplayName("update should throw NotFoundException when pharmacy is not found")
    void update_ThrowsNotFoundException_WhenNotFound() {
        var id = UUID.randomUUID();
        var request = utils.newPharmacyPutRequest();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(CompoundingPharmacy.class));
    }

    @Test
    @DisplayName("delete should delete pharmacy when successful")
    void delete_DeletesPharmacy_WhenSuccessful() {
        var pharmacy = utils.newPharmacy();

        BDDMockito.when(repository.findById(pharmacy.getId())).thenReturn(Optional.of(pharmacy));

        service.delete(pharmacy.getId());

        BDDMockito.then(repository).should().delete(pharmacy);
    }

    @Test
    @DisplayName("delete should throw NotFoundException when pharmacy is not found")
    void delete_ThrowsNotFoundException_WhenNotFound() {
        var id = UUID.randomUUID();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).delete(ArgumentMatchers.any(CompoundingPharmacy.class));
    }
}
