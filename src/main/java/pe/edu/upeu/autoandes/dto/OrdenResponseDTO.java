package pe.edu.upeu.autoandes.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import pe.edu.upeu.autoandes.entity.EstadoOrden;
public record OrdenResponseDTO(Long id, String numero, LocalDateTime fecha, Long vehiculoId, String placa,
        String clienteNombre, Integer kilometraje, EstadoOrden estado, BigDecimal total,
        String usuarioRegistro, String motivoAnulacion, List<DetalleOrdenResponseDTO> detalles) {}

