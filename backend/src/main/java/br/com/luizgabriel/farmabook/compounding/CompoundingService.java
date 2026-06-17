package br.com.luizgabriel.farmabook.compounding;

import br.com.luizgabriel.farmabook.auth.User;
import br.com.luizgabriel.farmabook.compounding.dto.CompoundingGetResponse;
import br.com.luizgabriel.farmabook.compounding.dto.CompoundingPostRequest;
import br.com.luizgabriel.farmabook.compounding.dto.CompoundingPostResponse;
import br.com.luizgabriel.farmabook.compounding.dto.CompoundingPutRequest;
import br.com.luizgabriel.farmabook.compounding.pharmacy.CompoundingPharmacyService;
import br.com.luizgabriel.farmabook.customer.CustomerService;
import br.com.luizgabriel.farmabook.exception.ConflictException;
import br.com.luizgabriel.farmabook.exception.NotFoundException;
import br.com.luizgabriel.farmabook.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompoundingService {

    private final CompoundingRepository repository;
    private final CompoundingMapper mapper;
    private final CustomerService customerService;
    private final CompoundingPharmacyService pharmacyService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public Page<CompoundingGetResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(mapper::toCompoundingGetResponse);
    }

    @Transactional(readOnly = true)
    public CompoundingGetResponse findById(UUID id) {
        var compounding = findByIdOrThrowNotFound(id);

        return mapper.toCompoundingGetResponse(compounding);
    }

    @Transactional
    public CompoundingPostResponse save(CompoundingPostRequest request, User actor) {
        var customer = customerService.findByIdOrThrowNotFound(request.customerId());
        var pharmacy = pharmacyService.findByIdOrThrowNotFound(request.pharmacyId());

        var compounding = mapper.toCompounding(request, customer, pharmacy, actor);

        if (request.paymentStatus() != null) {
            compounding.setPaymentChangedById(actor.getId());
            compounding.setPaymentChangedByName(actor.getName());
            compounding.setPaymentChangedAt(Instant.now());
        }

        var saved = repository.save(compounding);

        return mapper.toCompoundingPostResponse(saved);
    }

    @Transactional
    public CompoundingGetResponse update(UUID id, CompoundingPutRequest request) {
        var compounding = findByIdOrThrowNotFound(id);

        ensureNotDelivered(compounding);

        var customer = customerService.findByIdOrThrowNotFound(request.customerId());
        var pharmacy = pharmacyService.findByIdOrThrowNotFound(request.pharmacyId());

        compounding.setQuantity(request.quantity());
        compounding.setCustomerId(customer.getId());
        compounding.setCustomerName(customer.getName());
        compounding.setPharmacyId(pharmacy.getId());
        compounding.setPharmacyName(pharmacy.getName());
        compounding.setPharmacyCity(pharmacy.getCity());
        compounding.setValue(request.value());
        compounding.setObservations(request.observations());

        var updated = repository.save(compounding);

        return mapper.toCompoundingGetResponse(updated);
    }

    @Transactional
    public void delete(UUID id) {
        var compounding = findByIdOrThrowNotFound(id);

        ensureNotDelivered(compounding);

        repository.delete(compounding);
    }

    @Transactional
    public void markAsOrdered(UUID id, User actor) {
        var compounding = findByIdOrThrowNotFound(id);

        if (compounding.getStatus() == CompoundingStatus.DELIVERED) {
            throw new ConflictException("Compounding with id '" + id + "' is DELIVERED and cannot be modified");
        }

        if (compounding.getStatus() == CompoundingStatus.ORDERED) {
            return;
        }

        compounding.setReceivedById(null);
        compounding.setReceivedByName(null);
        compounding.setReceivedAt(null);

        compounding.setStatus(CompoundingStatus.ORDERED);
        compounding.setOrderedById(actor.getId());
        compounding.setOrderedByName(actor.getName());
        compounding.setOrderedAt(Instant.now());

        repository.save(compounding);
    }

    @Transactional
    public void markAsReceived(UUID id, User actor) {
        var compounding = findByIdOrThrowNotFound(id);

        if (compounding.getStatus() == CompoundingStatus.PENDING) {
            throw new ConflictException(
                    "Compounding with id '" + id + "' cannot be marked as RECEIVED from PENDING");
        }

        if (compounding.getStatus() == CompoundingStatus.DELIVERED) {
            throw new ConflictException(
                    "Compounding with id '" + id + "' is DELIVERED and cannot be modified");
        }

        if (compounding.getStatus() == CompoundingStatus.RECEIVED) {
            return;
        }

        compounding.setStatus(CompoundingStatus.RECEIVED);
        compounding.setReceivedById(actor.getId());
        compounding.setReceivedByName(actor.getName());
        compounding.setReceivedAt(Instant.now());

        repository.save(compounding);

        if (compounding.getNotifiedAt() == null) {
            notificationService.generateForCompoundingReceived(compounding)
                    .ifPresent(notification -> {
                        compounding.setNotifiedAt(notification.sentAt());
                        repository.save(compounding);
                    });
        }
    }

    @Transactional
    public void markAsDelivered(UUID id, User actor) {
        var compounding = findByIdOrThrowNotFound(id);

        if (compounding.getStatus() != CompoundingStatus.RECEIVED) {
            throw new ConflictException(
                    "Compounding with id '" + id + "' cannot be marked as DELIVERED from status " + compounding.getStatus());
        }

        compounding.setStatus(CompoundingStatus.DELIVERED);
        compounding.setDeliveredById(actor.getId());
        compounding.setDeliveredByName(actor.getName());
        compounding.setDeliveredAt(Instant.now());

        repository.save(compounding);
    }

    @Transactional
    public void markAsPaid(UUID id, User actor) {
        var compounding = findByIdOrThrowNotFound(id);

        if (compounding.getPaymentStatus() == PaymentStatus.NOTED) {
            throw new ConflictException(
                    "Compounding with id '" + id + "' payment is NOTED and cannot be changed");
        }

        compounding.setPaymentStatus(PaymentStatus.PAID);
        stampPaymentChange(compounding, actor);

        repository.save(compounding);
    }

    @Transactional
    public void markAsToPay(UUID id, User actor) {
        var compounding = findByIdOrThrowNotFound(id);

        if (compounding.getPaymentStatus() != PaymentStatus.MAKE_NOTE) {
            throw new ConflictException(
                    "Compounding with id '" + id + "' payment must be MAKE_NOTE to revert to TO_PAY, but is " + compounding.getPaymentStatus());
        }

        compounding.setPaymentStatus(PaymentStatus.TO_PAY);
        stampPaymentChange(compounding, actor);

        repository.save(compounding);
    }

    @Transactional
    public void markAsMakeNote(UUID id, User actor) {
        var compounding = findByIdOrThrowNotFound(id);

        if (compounding.getPaymentStatus() == PaymentStatus.PAID) {
            throw new ConflictException(
                    "Compounding with id '" + id + "' payment is PAID and cannot be changed");
        }

        if (compounding.getPaymentStatus() == PaymentStatus.NOTED) {
            throw new ConflictException(
                    "Compounding with id '" + id + "' payment is NOTED and cannot be changed");
        }

        compounding.setPaymentStatus(PaymentStatus.MAKE_NOTE);
        stampPaymentChange(compounding, actor);

        repository.save(compounding);
    }

    @Transactional
    public void markAsNoted(UUID id, User actor) {
        var compounding = findByIdOrThrowNotFound(id);

        if (compounding.getPaymentStatus() != PaymentStatus.MAKE_NOTE) {
            throw new ConflictException(
                    "Compounding with id '" + id + "' payment must be MAKE_NOTE to transition to NOTED, but is " + compounding.getPaymentStatus());
        }

        compounding.setPaymentStatus(PaymentStatus.NOTED);
        stampPaymentChange(compounding, actor);

        repository.save(compounding);
    }

    private void stampPaymentChange(Compounding compounding, User actor) {
        compounding.setPaymentChangedById(actor.getId());
        compounding.setPaymentChangedByName(actor.getName());
        compounding.setPaymentChangedAt(Instant.now());
    }

    private Compounding findByIdOrThrowNotFound(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Compounding with id '" + id + "' not found"));
    }

    private void ensureNotDelivered(Compounding compounding) {
        if (compounding.getStatus() == CompoundingStatus.DELIVERED) {
            throw new ConflictException(
                    "Compounding with id '" + compounding.getId() + "' is DELIVERED and cannot be modified");
        }
    }
}
