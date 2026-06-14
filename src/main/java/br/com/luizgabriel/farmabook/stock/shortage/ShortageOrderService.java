package br.com.luizgabriel.farmabook.stock.shortage;

import br.com.luizgabriel.farmabook.auth.User;
import br.com.luizgabriel.farmabook.exception.ConflictException;
import br.com.luizgabriel.farmabook.exception.NotFoundException;
import br.com.luizgabriel.farmabook.stock.distributor.DistributorService;
import br.com.luizgabriel.farmabook.stock.shortage.dto.ShortageGetResponse;
import br.com.luizgabriel.farmabook.stock.shortage.dto.ShortageOrderGetResponse;
import br.com.luizgabriel.farmabook.stock.shortage.dto.ShortageOrderListResponse;
import br.com.luizgabriel.farmabook.stock.shortage.dto.ShortageOrderPostRequest;
import br.com.luizgabriel.farmabook.stock.shortage.dto.ShortageOrderPutRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShortageOrderService {

    private final ShortageOrderRepository repository;
    private final ShortageRepository shortageRepository;
    private final DistributorService distributorService;
    private final ShortageOrderMapper mapper;
    private final ShortageMapper shortageMapper;

    @Transactional
    public ShortageOrderGetResponse save(ShortageOrderPostRequest request, User createdBy) {
        var distributor = distributorService.findByIdOrThrowNotFound(request.distributorId());

        var order = mapper.toShortageOrder(request, distributor.getName(), createdBy);
        var saved = repository.save(order);

        var shortages = request.items().stream()
                .map(item -> {
                    var shortage = Shortage.builder()
                            .product(item.product())
                            .category(item.category())
                            .quantity(item.quantity())
                            .shortageType(request.shortageType())
                            .status(ShortageStatus.PENDING)
                            .createdById(createdBy.getId())
                            .createdByName(createdBy.getName())
                            .shortageOrderId(saved.getId())
                            .costPrice(item.costPrice())
                            .build();
                    return shortageRepository.save(shortage);
                })
                .toList();

        var shortageResponses = shortages.stream()
                .map(shortageMapper::toShortageGetResponse)
                .toList();

        return mapper.toShortageOrderGetResponse(saved, shortageResponses);
    }

    @Transactional(readOnly = true)
    public Page<ShortageOrderListResponse> findAll(ShortageType shortageType, UUID distributorId, Pageable pageable) {
        if (distributorId != null) {
            return repository.findByShortageTypeAndDistributorId(shortageType, distributorId, pageable)
                    .map(mapper::toShortageOrderListResponse);
        }
        return repository.findByShortageType(shortageType, pageable)
                .map(mapper::toShortageOrderListResponse);
    }

    @Transactional(readOnly = true)
    public ShortageOrderGetResponse findById(UUID id) {
        var order = findByIdOrThrowNotFound(id);
        List<ShortageGetResponse> shortageResponses = shortageRepository.findAllByShortageOrderId(id)
                .stream()
                .map(shortageMapper::toShortageGetResponse)
                .toList();
        return mapper.toShortageOrderGetResponse(order, shortageResponses);
    }

    @Transactional
    public ShortageOrderGetResponse update(UUID id, ShortageOrderPutRequest request) {
        var order = findByIdOrThrowNotFound(id);
        if (order.getStatus() != ShortageOrderStatus.PENDING) {
            throw new ConflictException("Shortage order with id '" + id + "' is not PENDING and cannot be modified");
        }

        var distributor = distributorService.findByIdOrThrowNotFound(request.distributorId());
        order.setDistributorId(distributor.getId());
        order.setDistributorName(distributor.getName());
        order.setObservations(request.observations());
        repository.save(order);

        List<ShortageGetResponse> shortageResponses = shortageRepository.findAllByShortageOrderId(id)
                .stream()
                .map(shortageMapper::toShortageGetResponse)
                .toList();
        return mapper.toShortageOrderGetResponse(order, shortageResponses);
    }

    @Transactional
    public void delete(UUID id) {
        var order = findByIdOrThrowNotFound(id);
        if (order.getStatus() != ShortageOrderStatus.PENDING) {
            throw new ConflictException("Shortage order with id '" + id + "' is not PENDING and cannot be deleted");
        }
        shortageRepository.findAllByShortageOrderId(id).forEach(shortageRepository::delete);
        repository.delete(order);
    }

    @Transactional
    public void markAsOrdered(UUID id, User actor) {
        var order = findByIdOrThrowNotFound(id);
        if (order.getStatus() == ShortageOrderStatus.ORDERED) {
            throw new ConflictException("Shortage order with id '" + id + "' is already ordered");
        }

        order.setStatus(ShortageOrderStatus.ORDERED);
        order.setOrderedById(actor.getId());
        order.setOrderedByName(actor.getName());
        order.setOrderedAt(Instant.now());
        repository.save(order);

        var now = Instant.now();
        shortageRepository.findAllByShortageOrderId(id).forEach(shortage -> {
            if (shortage.getStatus() == ShortageStatus.PENDING) {
                shortage.setStatus(ShortageStatus.ORDERED);
                shortage.setOrderedById(actor.getId());
                shortage.setOrderedByName(actor.getName());
                shortage.setOrderedAt(now);
                shortageRepository.save(shortage);
            }
        });
    }

    private ShortageOrder findByIdOrThrowNotFound(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Shortage order with id '" + id + "' not found"));
    }
}
