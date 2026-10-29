package pe.edu.upeu.autoandes.dto;
import jakarta.validation.constraints.*;
public record AnularOrdenRequestDTO(@NotBlank @Size(min = 10, max = 200) String motivo) {}

