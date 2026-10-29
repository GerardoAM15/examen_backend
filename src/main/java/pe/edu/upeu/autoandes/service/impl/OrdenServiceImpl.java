package pe.edu.upeu.autoandes.service.impl;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.autoandes.dto.*;
import pe.edu.upeu.autoandes.entity.*;
import pe.edu.upeu.autoandes.exception.*;
import pe.edu.upeu.autoandes.mapper.OrdenMapper;
import pe.edu.upeu.autoandes.repository.*;
import pe.edu.upeu.autoandes.service.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrdenServiceImpl implements OrdenService {
    private static final Logger log = LoggerFactory.getLogger(OrdenServiceImpl.class);
    private final OrdenServicioRepository ordenes;
    private final VehiculoRepository vehiculos;
    private final ServicioRepository servicios;
    private final OrdenMapper mapper;
    private final Clock clock;

    @Transactional
    public OrdenResponseDTO registrar(OrdenRequestDTO request, String username) {
        // Este bloqueo serializa registros y anulaciones del mismo vehiculo (RN-04).
        Vehiculo vehiculo = vehiculos.buscarParaActualizar(request.vehiculoId()).orElseThrow(
                () -> new RecursoNoEncontradoException("Vehiculo no encontrado: " + request.vehiculoId()));
        validarDetalles(request.detalles());
        ordenes.findFirstByVehiculoIdAndEstadoOrderByFechaDescIdDesc(vehiculo.getId(), EstadoOrden.REGISTRADA)
                .ifPresent(ultima -> {
                    if (request.kilometraje() < ultima.getKilometraje())
                        throw new ReglaNegocioException("El kilometraje no puede ser menor al ultimo registrado: " + ultima.getKilometraje());
                });

        // Bloquear servicios en orden estable evita cambios de precio/estado durante el registro.
        Map<Long, Servicio> catalogo = new HashMap<>();
        request.detalles().stream().map(DetalleOrdenRequestDTO::servicioId).sorted().forEach(id -> {
            Servicio servicio = servicios.buscarParaActualizar(id).orElseThrow(
                    () -> new RecursoNoEncontradoException("Servicio no encontrado: " + id));
            if (!Boolean.TRUE.equals(servicio.getActivo()))
                throw new ReglaNegocioException("El servicio " + servicio.getCodigo() + " esta inactivo");
            catalogo.put(id, servicio);
        });

        OrdenServicio orden = new OrdenServicio();
        Long correlativo = ordenes.siguienteNumero();
        if (correlativo > 999999) throw new ReglaNegocioException("Se agoto el correlativo de ordenes");
        orden.setNumero("OS-%06d".formatted(correlativo));
        orden.setFecha(LocalDateTime.now(clock));
        orden.setVehiculo(vehiculo);
        orden.setKilometraje(request.kilometraje());
        orden.setEstado(EstadoOrden.REGISTRADA);
        orden.setUsuarioRegistro(username);
        BigDecimal total = BigDecimal.ZERO;
        for (DetalleOrdenRequestDTO linea : request.detalles()) {
            Servicio servicio = catalogo.get(linea.servicioId());
            DetalleOrden detalle = new DetalleOrden();
            detalle.setOrden(orden);
            detalle.setServicio(servicio);
            detalle.setCantidad(linea.cantidad());
            detalle.setPrecioUnitario(servicio.getPrecio());
            detalle.setSubtotal(servicio.getPrecio().multiply(BigDecimal.valueOf(linea.cantidad())));
            orden.getDetalles().add(detalle);
            total = total.add(detalle.getSubtotal());
        }
        if (total.compareTo(new BigDecimal("99999999.99")) > 0)
            throw new ReglaNegocioException("El total supera el importe maximo permitido");
        orden.setTotal(total);
        // Cascade guarda cabecera y detalles. Cualquier excepcion revierte toda la transaccion.
        ordenes.saveAndFlush(orden);
        log.info("Orden registrada | numero={} | usuario={} | total={}", orden.getNumero(), username, total);
        return mapper.toResponse(orden);
    }

    public List<OrdenResponseDTO> listar(String placa, EstadoOrden estado, LocalDate desde, LocalDate hasta) {
        RangoFechas.validar(desde, hasta);
        return ordenes.filtrar(placa == null || placa.isBlank() ? null : placa.trim(), estado,
                desde == null ? null : desde.atStartOfDay(), hasta == null ? null : hasta.plusDays(1).atStartOfDay())
                .stream().map(mapper::toResponse).toList();
    }

    public OrdenResponseDTO buscar(Long id) { return mapper.toResponse(obtener(id)); }

    @Transactional
    public OrdenResponseDTO anular(Long id, AnularOrdenRequestDTO request) {
        // La consulta escalar evita cargar un estado anterior mientras se espera el bloqueo.
        Long vehiculoId = ordenes.buscarVehiculoId(id).orElseThrow(
                () -> new RecursoNoEncontradoException("Orden no encontrada: " + id));
        vehiculos.buscarParaActualizar(vehiculoId).orElseThrow(
                () -> new RecursoNoEncontradoException("Vehiculo no encontrado: " + vehiculoId));
        OrdenServicio orden = ordenes.buscarParaActualizar(id).orElseThrow(
                () -> new RecursoNoEncontradoException("Orden no encontrada: " + id));
        if (orden.getEstado() != EstadoOrden.REGISTRADA)
            throw new ReglaNegocioException("Solo se puede anular una orden REGISTRADA");
        if (!orden.getFecha().toLocalDate().equals(LocalDate.now(clock)))
            throw new ReglaNegocioException("Solo se puede anular una orden el mismo dia de su registro");
        String motivo = request.motivo().trim();
        if (motivo.length() < 10 || motivo.length() > 200)
            throw new DatosInvalidosException("El motivo debe tener entre 10 y 200 caracteres sin espacios exteriores");
        orden.setEstado(EstadoOrden.ANULADA);
        orden.setMotivoAnulacion(motivo);
        ordenes.flush();
        log.info("Orden anulada | numero={}", orden.getNumero());
        return mapper.toResponse(orden);
    }

    private OrdenServicio obtener(Long id) {
        return ordenes.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Orden no encontrada: " + id));
    }

    private void validarDetalles(List<DetalleOrdenRequestDTO> detalles) {
        if (detalles == null || detalles.isEmpty() || detalles.size() > 10)
            throw new DatosInvalidosException("La orden debe tener entre 1 y 10 detalles");
        Set<Long> ids = new HashSet<>();
        for (DetalleOrdenRequestDTO d : detalles) {
            if (d == null || d.servicioId() == null || d.cantidad() == null || d.cantidad() < 1 || d.cantidad() > 5)
                throw new DatosInvalidosException("Cada detalle requiere servicio y cantidad entre 1 y 5");
            if (!ids.add(d.servicioId()))
                throw new DatosInvalidosException("No se puede repetir el servicio " + d.servicioId());
        }
    }
}

