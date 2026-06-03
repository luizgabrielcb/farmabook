package br.com.luizgabriel.farmaorder.stock.shortage;

import br.com.luizgabriel.farmaorder.commons.ShortageUtils;
import br.com.luizgabriel.farmaorder.commons.UserUtils;
import br.com.luizgabriel.farmaorder.exception.ConflictException;
import br.com.luizgabriel.farmaorder.exception.NotFoundException;
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
class ShortageServiceTest {

    @InjectMocks
    private ShortageService service;

    @InjectMocks
    private ShortageUtils utils;

    @InjectMocks
    private UserUtils userUtils;

    @Mock
    private ShortageRepository repository;

    @Mock
    private ShortageMapper mapper;

    @Test
    @DisplayName("save should return ShortagePostResponse when successful")
    void save_ReturnsShortagePostResponse_WhenSuccessful() {
        var actor = userUtils.newUser();
        var request = utils.newShortagePostRequest();
        var shortage = utils.newShortage();
        var response = utils.newShortagePostResponse(shortage);

        BDDMockito.when(mapper.toShortage(request, actor)).thenReturn(shortage);
        BDDMockito.when(repository.save(shortage)).thenReturn(shortage);
        BDDMockito.when(mapper.toShortagePostResponse(shortage)).thenReturn(response);

        var result = service.save(request, actor);

        assertThat(result).isEqualTo(response);
        BDDMockito.then(repository).should().save(shortage);
    }

    @Test
    @DisplayName("findAll should return a page of shortages when successful")
    void findAll_ReturnsPageOfShortages_WhenSuccessful() {
        var pageable = Pageable.ofSize(10);
        var shortage = utils.newShortage();
        var response = utils.newShortageGetResponse(shortage);
        var page = new PageImpl<>(List.of(shortage));

        BDDMockito.when(repository.findAll(pageable)).thenReturn(page);
        BDDMockito.when(mapper.toShortageGetResponse(shortage)).thenReturn(response);

        var result = service.findAll(pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    @DisplayName("findAll should return an empty page when no shortages exist")
    void findAll_ReturnsEmptyPage_WhenNoShortagesExist() {
        var pageable = Pageable.ofSize(10);

        BDDMockito.when(repository.findAll(pageable)).thenReturn(Page.empty());

        var result = service.findAll(pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("findById should return ShortageGetResponse when shortage is found")
    void findById_ReturnsShortageGetResponse_WhenSuccessful() {
        var shortage = utils.newShortage();
        var response = utils.newShortageGetResponse(shortage);

        BDDMockito.when(repository.findById(shortage.getId())).thenReturn(Optional.of(shortage));
        BDDMockito.when(mapper.toShortageGetResponse(shortage)).thenReturn(response);

        var result = service.findById(shortage.getId());

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("findById should throw NotFoundException when shortage is not found")
    void findById_ThrowsNotFoundException_WhenShortageNotFound() {
        var id = UUID.randomUUID();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    @DisplayName("update should return ShortagePutResponse when successful")
    void update_ReturnsShortagePutResponse_WhenSuccessful() {
        var shortage = utils.newShortage();
        var request = utils.newShortagePutRequest();
        var response = utils.newShortagePutResponse(shortage);

        BDDMockito.when(repository.findById(shortage.getId())).thenReturn(Optional.of(shortage));
        BDDMockito.when(repository.save(shortage)).thenReturn(shortage);
        BDDMockito.when(mapper.toShortagePutResponse(shortage)).thenReturn(response);

        var result = service.update(shortage.getId(), request);

        assertThat(result).isEqualTo(response);
        BDDMockito.then(repository).should().save(shortage);
    }

    @Test
    @DisplayName("update should throw NotFoundException when shortage is not found")
    void update_ThrowsNotFoundException_WhenShortageNotFound() {
        var id = UUID.randomUUID();
        var request = utils.newShortagePutRequest();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Shortage.class));
    }

    @Test
    @DisplayName("update should throw ConflictException when shortage is already ordered")
    void update_ThrowsConflictException_WhenShortageIsAlreadyOrdered() {
        var shortage = utils.newOrderedShortage();
        var request = utils.newShortagePutRequest();

        BDDMockito.when(repository.findById(shortage.getId())).thenReturn(Optional.of(shortage));

        assertThatThrownBy(() -> service.update(shortage.getId(), request))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Shortage.class));
    }

    @Test
    @DisplayName("delete should delete the shortage when successful")
    void delete_DeletesShortage_WhenSuccessful() {
        var shortage = utils.newShortage();

        BDDMockito.when(repository.findById(shortage.getId())).thenReturn(Optional.of(shortage));

        service.delete(shortage.getId());

        BDDMockito.then(repository).should().delete(shortage);
    }

    @Test
    @DisplayName("delete should throw NotFoundException when shortage is not found")
    void delete_ThrowsNotFoundException_WhenShortageNotFound() {
        var id = UUID.randomUUID();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).delete(ArgumentMatchers.any(Shortage.class));
    }

    @Test
    @DisplayName("delete should throw ConflictException when shortage is already ordered")
    void delete_ThrowsConflictException_WhenShortageIsAlreadyOrdered() {
        var shortage = utils.newOrderedShortage();

        BDDMockito.when(repository.findById(shortage.getId())).thenReturn(Optional.of(shortage));

        assertThatThrownBy(() -> service.delete(shortage.getId()))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).delete(ArgumentMatchers.any(Shortage.class));
    }

    @Test
    @DisplayName("markAsOrdered should set ORDERED status and stamp actor fields when successful")
    void markAsOrdered_SetsOrderedStatus_WhenSuccessful() {
        var shortage = utils.newShortage();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findById(shortage.getId())).thenReturn(Optional.of(shortage));
        BDDMockito.when(repository.save(shortage)).thenReturn(shortage);

        service.markAsOrdered(shortage.getId(), actor);

        assertThat(shortage.getStatus()).isEqualTo(ShortageStatus.ORDERED);
        assertThat(shortage.getOrderedById()).isEqualTo(actor.getId());
        assertThat(shortage.getOrderedByName()).isEqualTo(actor.getName());
        assertThat(shortage.getOrderedAt()).isNotNull();
        BDDMockito.then(repository).should().save(shortage);
    }

    @Test
    @DisplayName("markAsOrdered should throw NotFoundException when shortage is not found")
    void markAsOrdered_ThrowsNotFoundException_WhenShortageNotFound() {
        var id = UUID.randomUUID();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markAsOrdered(id, actor))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Shortage.class));
    }

    @Test
    @DisplayName("markAsOrdered should throw ConflictException when shortage is already ordered")
    void markAsOrdered_ThrowsConflictException_WhenShortageIsAlreadyOrdered() {
        var shortage = utils.newOrderedShortage();
        var actor = userUtils.newUser();

        BDDMockito.when(repository.findById(shortage.getId())).thenReturn(Optional.of(shortage));

        assertThatThrownBy(() -> service.markAsOrdered(shortage.getId(), actor))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(Shortage.class));
    }
}
