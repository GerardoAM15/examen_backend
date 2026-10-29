package pe.edu.upeu.autoandes.dto;
import java.math.BigDecimal;
public record ServicioResponseDTO(Long id, String codigo, String nombre, BigDecimal precio, Integer duracionMin, Boolean activo) {}

