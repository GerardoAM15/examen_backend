package pe.edu.upeu.autoandes.service;
import java.util.List;
import pe.edu.upeu.autoandes.dto.*;
public interface ServicioService {
    List<ServicioResponseDTO> listar();
    ServicioResponseDTO buscar(Long id);
    ServicioResponseDTO crear(ServicioRequestDTO request);
    ServicioResponseDTO actualizar(Long id, ServicioRequestDTO request);
    void eliminar(Long id);
    ServicioResponseDTO cambiarEstado(Long id, Boolean activo);
}

