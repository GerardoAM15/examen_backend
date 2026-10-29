package pe.edu.upeu.autoandes.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.autoandes.dto.*;
import pe.edu.upeu.autoandes.service.ServicioService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/servicios")
public class ServicioController {
    private final ServicioService service;
    @GetMapping
    public List<ServicioResponseDTO> listar() {
        return service.listar();
    }
    @GetMapping("/{id}")
    public ServicioResponseDTO buscar(@PathVariable Long id) { return service.buscar(id); }
    @PostMapping
    public ResponseEntity<ServicioResponseDTO> crear(@Valid @RequestBody ServicioRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }
    @PutMapping("/{id}")
    public ServicioResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody ServicioRequestDTO request) {
        return service.actualizar(id, request);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
    @PatchMapping("/{id}/estado")
    public ServicioResponseDTO cambiarEstado(@PathVariable Long id, @Valid @RequestBody EstadoServicioRequestDTO request) {
        return service.cambiarEstado(id, request.activo());
    }
}

