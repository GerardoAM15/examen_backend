package pe.edu.upeu.autoandes.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.autoandes.dto.*;
import pe.edu.upeu.autoandes.entity.EstadoOrden;
import pe.edu.upeu.autoandes.service.OrdenService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ordenes")
public class OrdenController {
    private final OrdenService service;
    @PostMapping
    public ResponseEntity<OrdenResponseDTO> registrar(@Valid @RequestBody OrdenRequestDTO request, Principal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrar(request, principal.getName()));
    }
    @GetMapping
    public List<OrdenResponseDTO> listar(@RequestParam(required = false) String placa,
            @RequestParam(required = false) EstadoOrden estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return service.listar(placa, estado, desde, hasta);
    }
    @GetMapping("/{id}")
    public OrdenResponseDTO buscar(@PathVariable Long id) { return service.buscar(id); }
    @PatchMapping("/{id}/anular")
    public OrdenResponseDTO anular(@PathVariable Long id, @Valid @RequestBody AnularOrdenRequestDTO request) {
        return service.anular(id, request);
    }
}

