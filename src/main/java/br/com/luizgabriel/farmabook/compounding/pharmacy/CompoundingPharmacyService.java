package br.com.luizgabriel.farmabook.compounding.pharmacy;

import br.com.luizgabriel.farmabook.compounding.pharmacy.dto.*;
import br.com.luizgabriel.farmabook.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CompoundingPharmacyService {

    private final CompoundingPharmacyRepository repository;
    private final CompoundingPharmacyMapper mapper;

    @Transactional(readOnly = true)
    public Page<CompoundingPharmacyGetResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable)
                .map(mapper::toCompoundingPharmacyGetResponse);
    }

    @Transactional(readOnly = true)
    public CompoundingPharmacyGetResponse findById(UUID id) {
        var pharmacy = findByIdOrThrowNotFound(id);

        return mapper.toCompoundingPharmacyGetResponse(pharmacy);
    }

    @Transactional
    public CompoundingPharmacyPostResponse save(CompoundingPharmacyPostRequest request) {
        var pharmacy = mapper.toCompoundingPharmacy(request);

        var saved = repository.save(pharmacy);

        return mapper.toCompoundingPharmacyPostResponse(saved);
    }

    @Transactional
    public CompoundingPharmacyPutResponse update(UUID id, CompoundingPharmacyPutRequest request) {
        var pharmacy = findByIdOrThrowNotFound(id);

        pharmacy.setName(request.name());
        pharmacy.setCity(request.city());

        var updated = repository.save(pharmacy);

        return mapper.toCompoundingPharmacyPutResponse(updated);
    }

    @Transactional
    public void delete(UUID id) {
        var pharmacy = findByIdOrThrowNotFound(id);

        repository.delete(pharmacy);
    }

    public CompoundingPharmacy findByIdOrThrowNotFound(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("CompoundingPharmacy with id '" + id + "' not found"));
    }
}
