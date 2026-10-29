package pe.edu.upeu.autoandes.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.autoandes.dto.*;
import pe.edu.upeu.autoandes.service.VehiculoService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/vehiculos")
public class VehiculoController {
    private final VehiculoService service;
    @GetMapping
    public List<VehiculoResponseDTO> listar(@RequestParam(required = false) Long clienteId) {
        return service.listar(clienteId);
    }
    @GetMapping("/{id}")
    public VehiculoResponseDTO buscar(@PathVariable Long id) { return service.buscar(id); }
    @PostMapping
    public ResponseEntity<VehiculoResponseDTO> crear(@Valid @RequestBody VehiculoRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(request));
    }
    @PutMapping("/{id}")
    public VehiculoResponseDTO actualizar(@PathVariable Long id, @Valid @RequestBody VehiculoRequestDTO request) {
        return service.actualizar(id, request);
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}

