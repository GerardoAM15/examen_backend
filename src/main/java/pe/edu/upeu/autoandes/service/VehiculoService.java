package pe.edu.upeu.autoandes.service;
import java.util.List;
import pe.edu.upeu.autoandes.dto.*;
public interface VehiculoService {
    List<VehiculoResponseDTO> listar(Long clienteId);
    VehiculoResponseDTO buscar(Long id);
    VehiculoResponseDTO crear(VehiculoRequestDTO request);
    VehiculoResponseDTO actualizar(Long id, VehiculoRequestDTO request);
    void eliminar(Long id);
}

