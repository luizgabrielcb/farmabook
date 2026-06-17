package br.com.luizgabriel.farmabook.distributor;

import br.com.luizgabriel.farmabook.commons.DistributorUtils;
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
class DistributorServiceTest {

    @InjectMocks
    private DistributorService service;

    @InjectMocks
    private DistributorUtils utils;

    @Mock
    private DistributorRepository repository;

    @Mock
    private DistributorMapper mapper;

    @Test
    @DisplayName("findAll should return a page of distributors when successful")
    void findAll_ReturnsPageOfDistributors_WhenSuccessful() {
        var pageable = Pageable.ofSize(10);
        var distributor = utils.newDistributor();
        var response = utils.newDistributorGetResponse(distributor);
        var page = new PageImpl<>(List.of(distributor));

        BDDMockito.when(repository.findAll(pageable)).thenReturn(page);
        BDDMockito.when(mapper.toDistributorGetResponse(distributor)).thenReturn(response);

        var result = service.findAll(pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    @DisplayName("findAll should return an empty page when no distributors exist")
    void findAll_ReturnsEmptyPage_WhenNoDistributorsExist() {
        var pageable = Pageable.ofSize(10);

        BDDMockito.when(repository.findAll(pageable)).thenReturn(Page.empty());

        var result = service.findAll(pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("save should return DistributorGetResponse when successful")
    void save_ReturnsDistributorGetResponse_WhenSuccessful() {
        var request = utils.newDistributorPostRequest();
        var distributor = utils.newDistributor();
        var response = utils.newDistributorGetResponse(distributor);

        BDDMockito.when(mapper.toDistributor(request)).thenReturn(distributor);
        BDDMockito.when(repository.save(distributor)).thenReturn(distributor);
        BDDMockito.when(mapper.toDistributorGetResponse(distributor)).thenReturn(response);

        var result = service.save(request);

        assertThat(result).isEqualTo(response);
        BDDMockito.then(repository).should().save(distributor);
    }

    @Test
    @DisplayName("update should return DistributorGetResponse when successful")
    void update_ReturnsDistributorGetResponse_WhenSuccessful() {
        var distributor = utils.newDistributor();
        var request = utils.newDistributorPutRequest();
        var response = utils.newDistributorGetResponse(distributor);

        BDDMockito.when(repository.findById(distributor.getId())).thenReturn(Optional.of(distributor));
        BDDMockito.when(repository.save(distributor)).thenReturn(distributor);
        BDDMockito.when(mapper.toDistributorGetResponse(distributor)).thenReturn(response);

        var result = service.update(distributor.getId(), request);

        assertThat(result).isEqualTo(response);
        assertThat(distributor.getName()).isEqualTo(request.name());
        BDDMockito.then(repository).should().save(distributor);
    }

    @Test
    @DisplayName("update should throw NotFoundException when distributor is not found")
    void update_ThrowsNotFoundException_WhenNotFound() {
        var id = UUID.randomUUID();
        var request = utils.newDistributorPutRequest();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Distributor.class));
    }

    @Test
    @DisplayName("delete should delete distributor when successful")
    void delete_DeletesDistributor_WhenSuccessful() {
        var distributor = utils.newDistributor();

        BDDMockito.when(repository.findById(distributor.getId())).thenReturn(Optional.of(distributor));

        service.delete(distributor.getId());

        BDDMockito.then(repository).should().delete(distributor);
    }

    @Test
    @DisplayName("delete should throw NotFoundException when distributor is not found")
    void delete_ThrowsNotFoundException_WhenNotFound() {
        var id = UUID.randomUUID();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).delete(ArgumentMatchers.any(Distributor.class));
    }
}
