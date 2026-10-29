package pe.edu.upeu.autoandes.mapper;
import org.springframework.stereotype.Component;
import pe.edu.upeu.autoandes.dto.ClienteResponseDTO;
import pe.edu.upeu.autoandes.entity.Cliente;
@Component
public class ClienteMapper {
    public ClienteResponseDTO toResponse(Cliente c) {
        return new ClienteResponseDTO(c.getId(), c.getDocumento(), c.getNombres(), c.getTelefono());
    }
}

