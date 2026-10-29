package pe.edu.upeu.autoandes.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
public record OrdenRequestDTO(
        @NotNull @Positive Long vehiculoId,
        @NotNull @Min(0) @Max(9999999) Integer kilometraje,
        @NotNull @Size(min = 1, max = 10) List<@NotNull @Valid DetalleOrdenRequestDTO> detalles) {}

