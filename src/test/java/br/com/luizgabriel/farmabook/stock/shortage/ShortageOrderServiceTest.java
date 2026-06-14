package br.com.luizgabriel.farmabook.stock.shortage;

import br.com.luizgabriel.farmabook.commons.ShortageOrderUtils;
import br.com.luizgabriel.farmabook.commons.ShortageUtils;
import br.com.luizgabriel.farmabook.exception.ConflictException;
import br.com.luizgabriel.farmabook.exception.NotFoundException;
import br.com.luizgabriel.farmabook.stock.distributor.Distributor;
import br.com.luizgabriel.farmabook.stock.distributor.DistributorService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.BDDMockito;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.ArgumentMatchers;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ShortageOrderServiceTest {

    @InjectMocks
    private ShortageOrderService service;

    @InjectMocks
    private ShortageOrderUtils utils;

    @InjectMocks
    private ShortageUtils shortageUtils;

    @Mock
    private ShortageOrderRepository repository;

    @Mock
    private ShortageRepository shortageRepository;

    @Mock
    private DistributorService distributorService;

    @Mock
    private ShortageOrderMapper mapper;

    @Mock
    private ShortageMapper shortageMapper;

    // --- update ---

    @Test
    @DisplayName("update should return ShortageOrderGetResponse with updated distributor and observations when successful")
    void update_ReturnsShortageOrderGetResponse_WhenSuccessful() {
        var order = utils.newShortageOrder();
        var request = utils.newShortageOrderPutRequest();
        var newDistributor = Distributor.builder()
                .id(ShortageOrderUtils.DISTRIBUTOR_ID)
                .name("Updated Distributor")
                .build();
        var shortage = shortageUtils.newShortage();
        var shortageResponse = shortageUtils.newShortageGetResponse(shortage);
        var response = utils.newShortageOrderGetResponse(order, List.of(shortageResponse));

        BDDMockito.when(repository.findById(order.getId())).thenReturn(Optional.of(order));
        BDDMockito.when(distributorService.findByIdOrThrowNotFound(request.distributorId())).thenReturn(newDistributor);
        BDDMockito.when(shortageRepository.findAllByShortageOrderId(order.getId())).thenReturn(List.of(shortage));
        BDDMockito.when(shortageMapper.toShortageGetResponse(shortage)).thenReturn(shortageResponse);
        BDDMockito.when(mapper.toShortageOrderGetResponse(order, List.of(shortageResponse))).thenReturn(response);

        var result = service.update(order.getId(), request);

        assertThat(result).isEqualTo(response);
        assertThat(order.getDistributorId()).isEqualTo(newDistributor.getId());
        assertThat(order.getDistributorName()).isEqualTo(newDistributor.getName());
        assertThat(order.getObservations()).isEqualTo(request.observations());
        BDDMockito.then(repository).should().save(order);
    }

    @Test
    @DisplayName("update should throw NotFoundException when shortage order is not found")
    void update_ThrowsNotFoundException_WhenShortageOrderNotFound() {
        var id = UUID.randomUUID();
        var request = utils.newShortageOrderPutRequest();

        BDDMockito.when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, request))
                .isInstanceOf(NotFoundException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(ShortageOrder.class));
    }

    @Test
    @DisplayName("update should throw ConflictException when shortage order is already ORDERED")
    void update_ThrowsConflictException_WhenShortageOrderIsOrdered() {
        var order = utils.newOrderedShortageOrder();
        var request = utils.newShortageOrderPutRequest();

        BDDMockito.when(repository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.update(order.getId(), request))
                .isInstanceOf(ConflictException.class);

        BDDMockito.then(repository).should(Mockito.never()).save(ArgumentMatchers.any(ShortageOrder.class));
    }
}
