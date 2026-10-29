package pe.edu.upeu.autoandes.dto;
import java.math.BigDecimal;
public record DetalleOrdenResponseDTO(Long id, Long servicioId, String codigo, String servicio,
                                      Integer cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {}

