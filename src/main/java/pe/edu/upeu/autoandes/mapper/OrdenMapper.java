package pe.edu.upeu.autoandes.mapper;

import org.springframework.stereotype.Component;
import pe.edu.upeu.autoandes.dto.*;
import pe.edu.upeu.autoandes.entity.OrdenServicio;

@Component
public class OrdenMapper {
    public OrdenResponseDTO toResponse(OrdenServicio o) {
        var detalles = o.getDetalles().stream().map(d -> new DetalleOrdenResponseDTO(
                d.getId(), d.getServicio().getId(), d.getServicio().getCodigo(), d.getServicio().getNombre(),
                d.getCantidad(), d.getPrecioUnitario(), d.getSubtotal())).toList();
        return new OrdenResponseDTO(o.getId(), o.getNumero(), o.getFecha(), o.getVehiculo().getId(),
                o.getVehiculo().getPlaca(), o.getVehiculo().getCliente().getNombres(), o.getKilometraje(),
                o.getEstado(), o.getTotal(), o.getUsuarioRegistro(), o.getMotivoAnulacion(), detalles);
    }
}

