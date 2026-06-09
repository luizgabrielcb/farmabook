package br.com.luizgabriel.farmabook.stock.distributor;
import br.com.luizgabriel.farmabook.exception.NotFoundException;
import br.com.luizgabriel.farmabook.stock.distributor.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DistributorService {
    private final DistributorRepository repository;
    private final DistributorMapper mapper;

    @Transactional(readOnly = true)
    public Page<DistributorGetResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::toDistributorGetResponse);
    }

    @Transactional
    public DistributorGetResponse save(DistributorPostRequest request) {
        var distributor = mapper.toDistributor(request);
        return mapper.toDistributorGetResponse(repository.save(distributor));
    }

    @Transactional
    public DistributorGetResponse update(UUID id, DistributorPostRequest request) {
        var distributor = findByIdOrThrowNotFound(id);
        distributor.setName(request.name());
        return mapper.toDistributorGetResponse(repository.save(distributor));
    }

    @Transactional
    public void delete(UUID id) {
        var distributor = findByIdOrThrowNotFound(id);
        repository.delete(distributor);
    }

    public Distributor findByIdOrThrowNotFound(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Distributor with id '" + id + "' not found"));
    }
}
