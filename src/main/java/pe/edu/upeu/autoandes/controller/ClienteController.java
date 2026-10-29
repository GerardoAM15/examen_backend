package pe.edu.upeu.autoandes.controller;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.autoandes.dto.ClienteResponseDTO;
import pe.edu.upeu.autoandes.service.ClienteService;
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/clientes")
public class ClienteController {
    private final ClienteService service;
    @GetMapping
    public List<ClienteResponseDTO> listar() { return service.listar(); }
}

