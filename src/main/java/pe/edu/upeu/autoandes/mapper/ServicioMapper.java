package pe.edu.upeu.autoandes.mapper;
import org.springframework.stereotype.Component;
import pe.edu.upeu.autoandes.dto.*;
import pe.edu.upeu.autoandes.entity.Servicio;
@Component
public class ServicioMapper {
    public ServicioResponseDTO toResponse(Servicio s) {
        return new ServicioResponseDTO(s.getId(), s.getCodigo(), s.getNombre(), s.getPrecio(), s.getDuracionMin(), s.getActivo());
    }
    public void actualizar(Servicio s, ServicioRequestDTO r) {
        s.setCodigo(r.codigo()); s.setNombre(r.nombre().trim()); s.setPrecio(r.precio());
        s.setDuracionMin(r.duracionMin()); s.setActivo(r.activo());
    }
}

