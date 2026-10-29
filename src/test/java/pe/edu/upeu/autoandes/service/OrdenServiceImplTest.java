package pe.edu.upeu.autoandes.service;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.edu.upeu.autoandes.dto.*;
import pe.edu.upeu.autoandes.entity.*;
import pe.edu.upeu.autoandes.exception.*;
import pe.edu.upeu.autoandes.mapper.OrdenMapper;
import pe.edu.upeu.autoandes.repository.*;
import pe.edu.upeu.autoandes.service.impl.OrdenServiceImpl;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdenServiceImplTest {
    @Mock OrdenServicioRepository ordenes;
    @Mock VehiculoRepository vehiculos;
    @Mock ServicioRepository servicios;
    private OrdenServiceImpl service;
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-29T15:00:00Z"), ZoneId.of("America/Lima"));

    @BeforeEach void preparar() {
        service = new OrdenServiceImpl(ordenes, vehiculos, servicios, new OrdenMapper(), clock);
    }
    private Vehiculo vehiculo() {
        var cliente = new Cliente(); cliente.setId(1L); cliente.setNombres("Ana");
        var v = new Vehiculo(); v.setId(1L); v.setPlaca("AAA-111"); v.setCliente(cliente);
        return v;
    }
    private OrdenServicio orden(EstadoOrden estado, LocalDateTime fecha) {
        var o = new OrdenServicio(); o.setId(1L); o.setVehiculo(vehiculo()); o.setKilometraje(45000);
        o.setEstado(estado); o.setFecha(fecha); return o;
    }
    private void prepararAnulacion(OrdenServicio o) {
        when(ordenes.buscarVehiculoId(1L)).thenReturn(Optional.of(1L));
        when(vehiculos.buscarParaActualizar(1L)).thenReturn(Optional.of(vehiculo()));
        when(ordenes.buscarParaActualizar(1L)).thenReturn(Optional.of(o));
    }
    @Test void rechazaServicioRepetidoAntesDeGuardar() {
        when(vehiculos.buscarParaActualizar(1L)).thenReturn(Optional.of(vehiculo()));
        var r = new OrdenRequestDTO(1L, 45000, List.of(new DetalleOrdenRequestDTO(1L, 1), new DetalleOrdenRequestDTO(1L, 2)));
        assertThatThrownBy(() -> service.registrar(r, "asesor")).isInstanceOf(DatosInvalidosException.class);
        verify(ordenes, never()).saveAndFlush(any());
    }
    @Test void kilometrajeMenorInformaElUltimoValor() {
        when(vehiculos.buscarParaActualizar(1L)).thenReturn(Optional.of(vehiculo()));
        when(ordenes.findFirstByVehiculoIdAndEstadoOrderByFechaDescIdDesc(1L, EstadoOrden.REGISTRADA))
                .thenReturn(Optional.of(orden(EstadoOrden.REGISTRADA, LocalDateTime.now(clock))));
        assertThatThrownBy(() -> service.registrar(new OrdenRequestDTO(1L, 44999, List.of(new DetalleOrdenRequestDTO(1L, 1))), "asesor"))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("45000");
        verify(ordenes, never()).saveAndFlush(any());
    }
    @Test void calculaPrecioYTotalConElCatalogo() {
        when(vehiculos.buscarParaActualizar(1L)).thenReturn(Optional.of(vehiculo()));
        when(ordenes.findFirstByVehiculoIdAndEstadoOrderByFechaDescIdDesc(1L, EstadoOrden.REGISTRADA)).thenReturn(Optional.empty());
        var s = new Servicio(); s.setId(1L); s.setCodigo("SRV-001"); s.setNombre("Aceite");
        s.setPrecio(new BigDecimal("120.50")); s.setActivo(true);
        when(servicios.buscarParaActualizar(1L)).thenReturn(Optional.of(s));
        when(ordenes.siguienteNumero()).thenReturn(5L);
        var respuesta = service.registrar(new OrdenRequestDTO(1L, 45000, List.of(new DetalleOrdenRequestDTO(1L, 3))), "asesor");
        assertThat(respuesta.total()).isEqualByComparingTo("361.50");
        assertThat(respuesta.numero()).isEqualTo("OS-000005");
        assertThat(respuesta.usuarioRegistro()).isEqualTo("asesor");
        assertThat(respuesta.detalles().getFirst().precioUnitario()).isEqualByComparingTo("120.50");
        verify(ordenes).saveAndFlush(any());
    }
    @Test void rechazaOrdenDeOtroDia() {
        prepararAnulacion(orden(EstadoOrden.REGISTRADA, LocalDateTime.now(clock).minusDays(1)));
        assertThatThrownBy(() -> service.anular(1L, new AnularOrdenRequestDTO("Motivo de prueba valido")))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("mismo dia");
    }
    @Test void rechazaOrdenYaAnulada() {
        prepararAnulacion(orden(EstadoOrden.ANULADA, LocalDateTime.now(clock)));
        assertThatThrownBy(() -> service.anular(1L, new AnularOrdenRequestDTO("Motivo de prueba valido")))
                .isInstanceOf(ReglaNegocioException.class).hasMessageContaining("REGISTRADA");
    }
    @Test void anulaElMismoDiaConMotivo() {
        prepararAnulacion(orden(EstadoOrden.REGISTRADA, LocalDateTime.now(clock)));
        var respuesta = service.anular(1L, new AnularOrdenRequestDTO("  Cliente cancelo la visita  "));
        assertThat(respuesta.estado()).isEqualTo(EstadoOrden.ANULADA);
        assertThat(respuesta.motivoAnulacion()).isEqualTo("Cliente cancelo la visita");
    }
    @Test void rechazaMotivoRellenadoConEspacios() {
        prepararAnulacion(orden(EstadoOrden.REGISTRADA, LocalDateTime.now(clock)));
        assertThatThrownBy(() -> service.anular(1L, new AnularOrdenRequestDTO("         x         ")))
                .isInstanceOf(DatosInvalidosException.class);
    }
}

