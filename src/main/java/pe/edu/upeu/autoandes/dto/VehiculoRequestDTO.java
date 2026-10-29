package pe.edu.upeu.autoandes.dto;
import jakarta.validation.constraints.*;
public record VehiculoRequestDTO(
        @NotBlank @Pattern(regexp = "[A-Z]{3}-[0-9]{3}", message = "La placa debe tener el formato ABC-123") String placa,
        @NotBlank @Size(max = 40) String marca,
        @NotBlank @Size(max = 40) String modelo,
        @NotNull @Min(1990) @Max(2027) Integer anio,
        @NotNull @Positive Long clienteId) {}

