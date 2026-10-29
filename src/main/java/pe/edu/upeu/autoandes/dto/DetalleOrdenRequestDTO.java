package pe.edu.upeu.autoandes.dto;
import jakarta.validation.constraints.*;
public record DetalleOrdenRequestDTO(@NotNull @Positive Long servicioId, @NotNull @Min(1) @Max(5) Integer cantidad) {}

