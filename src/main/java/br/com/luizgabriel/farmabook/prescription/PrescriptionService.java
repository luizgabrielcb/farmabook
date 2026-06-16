package br.com.luizgabriel.farmabook.prescription;

import br.com.luizgabriel.farmabook.auth.User;
import br.com.luizgabriel.farmabook.customer.CustomerService;
import br.com.luizgabriel.farmabook.exception.ConflictException;
import br.com.luizgabriel.farmabook.exception.NotFoundException;
import br.com.luizgabriel.farmabook.prescription.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository repository;
    private final PrescriptionItemRepository itemRepository;
    private final PrescriptionMapper mapper;
    private final CustomerService customerService;

    @Transactional
    public PrescriptionPostResponse save(PrescriptionPostRequest request, User actor) {
        var customer = request.customerId() != null
                ? customerService.findByIdOrThrowNotFound(request.customerId())
                : null;

        var prescription = mapper.toPrescription(request, customer, actor);

        var items = mapper.toPrescriptionItems(request.items());
        items.forEach(item -> item.setPrescription(prescription));
        prescription.setItems(items);

        var saved = repository.save(prescription);

        return mapper.toPrescriptionPostResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<PrescriptionGetResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toPrescriptionGetResponse);
    }

    @Transactional(readOnly = true)
    public PrescriptionGetResponse findById(UUID id) {
        var prescription = findByIdWithItemsOrThrowNotFound(id);
        return mapper.toPrescriptionGetResponse(prescription);
    }

    @Transactional
    public PrescriptionPutResponse update(UUID id, PrescriptionPutRequest request) {
        var prescription = findByIdWithItemsOrThrowNotFound(id);

        ensureMutable(prescription);

        var customer = request.customerId() != null
                ? customerService.findByIdOrThrowNotFound(request.customerId())
                : null;
        prescription.setCustomerId(customer != null ? customer.getId() : null);
        prescription.setCustomerName(customer != null ? customer.getName() : null);
        prescription.setObservations(request.observations());

        return mapper.toPrescriptionPutResponse(prescription);
    }

    @Transactional
    public void delete(UUID id) {
        var prescription = findByIdWithItemsOrThrowNotFound(id);
        ensureMutable(prescription);
        repository.delete(prescription);
    }

    @Transactional
    public PrescriptionItemGetResponse addItem(UUID prescriptionId, PrescriptionItemPostRequest request) {
        var prescription = findByIdWithItemsOrThrowNotFound(prescriptionId);

        ensureMutable(prescription);

        var item = mapper.toPrescriptionItem(request);
        item.setPrescription(prescription);

        var saved = itemRepository.save(item);
        prescription.getItems().add(saved);

        return mapper.toPrescriptionItemGetResponse(saved);
    }

    @Transactional
    public PrescriptionItemGetResponse updateItem(UUID prescriptionId, UUID itemId, PrescriptionItemPutRequest request) {
        var prescription = findByIdWithItemsOrThrowNotFound(prescriptionId);

        ensureMutable(prescription);

        var item = findItemOrThrowNotFound(prescription, itemId);

        if (item.getStatus() == PrescriptionItemStatus.RECEIVED) {
            throw new ConflictException("Item with id '" + itemId + "' is RECEIVED and cannot be modified");
        }

        item.setProduct(request.product());
        item.setQuantity(request.quantity());
        item.setBatch(request.batch());
        item.setExpiry(request.expiry());

        return mapper.toPrescriptionItemGetResponse(item);
    }

    @Transactional
    public void deleteItem(UUID prescriptionId, UUID itemId) {
        var prescription = findByIdWithItemsOrThrowNotFound(prescriptionId);

        ensureMutable(prescription);

        var item = findItemOrThrowNotFound(prescription, itemId);

        if (item.getStatus() == PrescriptionItemStatus.RECEIVED) {
            throw new ConflictException("Item with id '" + itemId + "' is RECEIVED and cannot be deleted");
        }

        prescription.getItems().remove(item);
        repository.saveAndFlush(prescription);
    }

    @Transactional
    public void markItemAsReceived(UUID prescriptionId, UUID itemId, User actor) {
        var prescription = findByIdWithItemsOrThrowNotFound(prescriptionId);

        ensureMutable(prescription);

        var item = findItemOrThrowNotFound(prescription, itemId);

        if (item.getStatus() == PrescriptionItemStatus.RECEIVED) {
            throw new ConflictException("Item with id '" + itemId + "' is already RECEIVED");
        }

        applyMarkAsReceived(item, actor);

        recalculatePrescriptionStatus(prescription);
    }

    @Transactional
    public void markAllAsReceived(UUID prescriptionId, User actor) {
        var prescription = findByIdWithItemsOrThrowNotFound(prescriptionId);

        ensureMutable(prescription);

        prescription.getItems().stream()
                .filter(i -> i.getStatus() == PrescriptionItemStatus.PENDING)
                .forEach(i -> applyMarkAsReceived(i, actor));

        recalculatePrescriptionStatus(prescription);
    }

    public Prescription findByIdOrThrowNotFound(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Prescription with id '" + id + "' not found"));
    }

    private void applyMarkAsReceived(PrescriptionItem item, User actor) {
        item.setStatus(PrescriptionItemStatus.RECEIVED);
        item.setReceivedById(actor.getId());
        item.setReceivedByName(actor.getName());
        item.setReceivedAt(Instant.now());
    }

    private void recalculatePrescriptionStatus(Prescription prescription) {
        var allReceived = prescription.getItems().stream()
                .allMatch(i -> i.getStatus() == PrescriptionItemStatus.RECEIVED);

        prescription.setStatus(allReceived ? PrescriptionStatus.FINISHED : PrescriptionStatus.PENDING);
        repository.save(prescription);
    }

    private Prescription findByIdWithItemsOrThrowNotFound(UUID id) {
        return repository.findWithItemsById(id)
                .orElseThrow(() -> new NotFoundException("Prescription with id '" + id + "' not found"));
    }

    private PrescriptionItem findItemOrThrowNotFound(Prescription prescription, UUID itemId) {
        return prescription.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Item with id '" + itemId + "' not found in prescription '" + prescription.getId() + "'"));
    }

    private void ensureMutable(Prescription prescription) {
        if (prescription.getStatus() == PrescriptionStatus.FINISHED) {
            throw new ConflictException(
                    "Prescription with id '" + prescription.getId() + "' is FINISHED and cannot be modified");
        }
    }
}
