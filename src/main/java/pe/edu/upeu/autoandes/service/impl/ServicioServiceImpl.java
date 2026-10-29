package pe.edu.upeu.autoandes.service.impl;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.autoandes.dto.*;
import pe.edu.upeu.autoandes.entity.Servicio;
import pe.edu.upeu.autoandes.exception.*;
import pe.edu.upeu.autoandes.mapper.ServicioMapper;
import pe.edu.upeu.autoandes.repository.ServicioRepository;
import pe.edu.upeu.autoandes.service.ServicioService;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ServicioServiceImpl implements ServicioService {
    private static final Logger log = LoggerFactory.getLogger(ServicioServiceImpl.class);
    private final ServicioRepository repository;
    private final ServicioMapper mapper;

    public List<ServicioResponseDTO> listar() {
        return repository.findAll(Sort.by("codigo")).stream().map(mapper::toResponse).toList();
    }
    public ServicioResponseDTO buscar(Long id) { return mapper.toResponse(obtener(id)); }

    @Transactional
    public ServicioResponseDTO crear(ServicioRequestDTO r) {
        if (repository.existsByCodigo(r.codigo())) throw new ReglaNegocioException("Ya existe el codigo " + r.codigo());
        Servicio servicio = new Servicio();
        mapper.actualizar(servicio, r);
        repository.saveAndFlush(servicio);
        log.info("Servicio creado | id={} | codigo={}", servicio.getId(), servicio.getCodigo());
        return mapper.toResponse(servicio);
    }

    @Transactional
    public ServicioResponseDTO actualizar(Long id, ServicioRequestDTO r) {
        Servicio servicio = bloquear(id);
        if (repository.existsByCodigoAndIdNot(r.codigo(), id)) throw new ReglaNegocioException("Ya existe el codigo " + r.codigo());
        mapper.actualizar(servicio, r);
        repository.flush();
        log.info("Servicio actualizado | id={}", id);
        return mapper.toResponse(servicio);
    }

    @Transactional
    public void eliminar(Long id) {
        Servicio servicio = bloquear(id);
        if (repository.contarUsos(id) > 0) throw new ReglaNegocioException(
                "El servicio tiene ordenes; use PATCH /api/v1/servicios/" + id + "/estado para darlo de baja");
        repository.delete(servicio);
        repository.flush();
        log.info("Servicio eliminado | id={}", id);
    }

    @Transactional
    public ServicioResponseDTO cambiarEstado(Long id, Boolean activo) {
        Servicio servicio = bloquear(id);
        servicio.setActivo(activo);
        repository.flush();
        log.info("Estado de servicio cambiado | id={} | activo={}", id, activo);
        return mapper.toResponse(servicio);
    }

    private Servicio obtener(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado: " + id));
    }
    private Servicio bloquear(Long id) {
        return repository.buscarParaActualizar(id).orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado: " + id));
    }
}

