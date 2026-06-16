package br.com.luizgabriel.farmabook.shortage;

import br.com.luizgabriel.farmabook.auth.User;
import br.com.luizgabriel.farmabook.exception.ConflictException;
import br.com.luizgabriel.farmabook.exception.NotFoundException;
import br.com.luizgabriel.farmabook.shortage.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShortageService {

    private final ShortageRepository repository;
    private final ShortageOrderRepository shortageOrderRepository;
    private final ShortageMapper mapper;

    @Transactional
    public ShortagePostResponse save(ShortagePostRequest request, User createdBy) {
        var shortage = mapper.toShortage(request, createdBy);

        var savedShortage = repository.save(shortage);

        return mapper.toShortagePostResponse(savedShortage);
    }

    @Transactional(readOnly = true)
    public Page<ShortageGetResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(mapper::toShortageGetResponse);
    }

    @Transactional(readOnly = true)
    public ShortageGetResponse findById(UUID id) {
        var shortage = findByIdOrThrowNotFound(id);

        return mapper.toShortageGetResponse(shortage);
    }

    @Transactional
    public ShortagePutResponse update(UUID id, ShortagePutRequest request) {
        var shortage = findByIdOrThrowNotFound(id);

        ensureNotOrdered(shortage);

        shortage.setProduct(request.product());
        shortage.setCategory(request.category());
        shortage.setQuantity(request.quantity());
        shortage.setCostPrice(request.costPrice());

        var updatedShortage = repository.save(shortage);

        return mapper.toShortagePutResponse(updatedShortage);
    }

    @Transactional
    public void delete(UUID id) {
        var shortage = findByIdOrThrowNotFound(id);

        ensureNotOrdered(shortage);

        repository.delete(shortage);
    }

    @Transactional
    public void markAsOrdered(UUID id, User actor) {
        var shortage = findByIdOrThrowNotFound(id);

        if (shortage.getStatus() == ShortageStatus.ORDERED) {
            throw new ConflictException("Shortage with id '" + id + "' is already ordered");
        }

        shortage.setStatus(ShortageStatus.ORDERED);
        shortage.setOrderedById(actor.getId());
        shortage.setOrderedByName(actor.getName());
        shortage.setOrderedAt(Instant.now());

        repository.save(shortage);

        if (shortage.getShortageOrderId() != null) {
            recalculateShortageOrderStatus(shortage.getShortageOrderId(), actor);
        }
    }

    private void recalculateShortageOrderStatus(UUID shortageOrderId, User actor) {
        var order = shortageOrderRepository.findById(shortageOrderId).orElse(null);
        if (order == null || order.getStatus() == ShortageOrderStatus.ORDERED) return;

        var allOrdered = repository.findAllByShortageOrderId(shortageOrderId)
                .stream()
                .allMatch(s -> s.getStatus() == ShortageStatus.ORDERED);

        if (allOrdered) {
            order.setStatus(ShortageOrderStatus.ORDERED);
            order.setOrderedById(actor.getId());
            order.setOrderedByName(actor.getName());
            order.setOrderedAt(Instant.now());
            shortageOrderRepository.save(order);
        }
    }

    private Shortage findByIdOrThrowNotFound(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Shortage with id '" + id + "' not found"));
    }

    private void ensureNotOrdered(Shortage shortage) {
        if (shortage.getStatus() == ShortageStatus.ORDERED) {
            throw new ConflictException("Shortage with id '" + shortage.getId() + "' has already been ordered and cannot be modified");
        }
    }
}
