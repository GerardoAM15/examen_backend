package pe.edu.upeu.autoandes.service.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.autoandes.dto.*;
import pe.edu.upeu.autoandes.entity.*;
import pe.edu.upeu.autoandes.exception.*;
import pe.edu.upeu.autoandes.mapper.VehiculoMapper;
import pe.edu.upeu.autoandes.repository.*;
import pe.edu.upeu.autoandes.service.VehiculoService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehiculoServiceImpl implements VehiculoService {
    private static final Logger log = LoggerFactory.getLogger(VehiculoServiceImpl.class);
    private final VehiculoRepository repository;
    private final ClienteRepository clientes;
    private final VehiculoMapper mapper;

    public List<VehiculoResponseDTO> listar(Long clienteId) {
        if (clienteId != null) cliente(clienteId);
        List<Vehiculo> vehiculos = clienteId == null ? repository.findAll(Sort.by("placa")) :
                repository.findByClienteIdOrderByPlacaAsc(clienteId);
        return vehiculos.stream().map(mapper::toResponse).toList();
    }
    public VehiculoResponseDTO buscar(Long id) { return mapper.toResponse(obtener(id)); }

    @Transactional
    public VehiculoResponseDTO crear(VehiculoRequestDTO r) {
        if (repository.existsByPlaca(r.placa())) throw new ReglaNegocioException("Ya existe la placa " + r.placa());
        Vehiculo vehiculo = new Vehiculo();
        mapper.actualizar(vehiculo, r, cliente(r.clienteId()));
        repository.saveAndFlush(vehiculo);
        log.info("Vehiculo creado | id={} | placa={}", vehiculo.getId(), vehiculo.getPlaca());
        return mapper.toResponse(vehiculo);
    }

    @Transactional
    public VehiculoResponseDTO actualizar(Long id, VehiculoRequestDTO r) {
        Vehiculo vehiculo = bloquear(id);
        if (repository.existsByPlacaAndIdNot(r.placa(), id)) throw new ReglaNegocioException("Ya existe la placa " + r.placa());
        mapper.actualizar(vehiculo, r, cliente(r.clienteId()));
        repository.flush();
        log.info("Vehiculo actualizado | id={}", id);
        return mapper.toResponse(vehiculo);
    }

    @Transactional
    public void eliminar(Long id) {
        Vehiculo vehiculo = bloquear(id);
        if (repository.contarOrdenes(id) > 0) throw new ReglaNegocioException("El vehiculo tiene ordenes y no se puede eliminar");
        repository.delete(vehiculo);
        repository.flush();
        log.info("Vehiculo eliminado | id={}", id);
    }

    private Cliente cliente(Long id) {
        return clientes.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Cliente no encontrado: " + id));
    }
    private Vehiculo obtener(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Vehiculo no encontrado: " + id));
    }
    private Vehiculo bloquear(Long id) {
        return repository.buscarParaActualizar(id).orElseThrow(() -> new RecursoNoEncontradoException("Vehiculo no encontrado: " + id));
    }
}

