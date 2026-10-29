package pe.edu.upeu.autoandes.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ServicioRequestDTO(
        @NotBlank @Pattern(regexp = "SRV-[0-9]{3}", message = "El codigo debe tener el formato SRV-000") String codigo,
        @NotBlank @Size(min = 3, max = 80) String nombre,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 8, fraction = 2) BigDecimal precio,
        @NotNull @Min(15) @Max(480) Integer duracionMin,
        @NotNull Boolean activo) {}

