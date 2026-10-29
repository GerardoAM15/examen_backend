package pe.edu.upeu.autoandes.repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.autoandes.entity.DetalleOrden;
import pe.edu.upeu.autoandes.dto.IngresoPorServicioDTO;

public interface DetalleOrdenRepository extends JpaRepository<DetalleOrden, Long> {
    // La agregacion la ejecuta Oracle; no se cargan los detalles para sumarlos en Java.
    @Query("""
            select new pe.edu.upeu.autoandes.dto.IngresoPorServicioDTO(
                s.codigo, s.nombre, sum(d.cantidad), sum(d.subtotal))
            from DetalleOrden d join d.servicio s join d.orden o
            where o.estado = pe.edu.upeu.autoandes.entity.EstadoOrden.REGISTRADA
              and (:desde is null or o.fecha >= :desde)
              and (:hasta is null or o.fecha < :hasta)
            group by s.codigo, s.nombre
            order by sum(d.subtotal) desc, s.codigo asc
            """)
    List<IngresoPorServicioDTO> ingresos(@Param("desde") LocalDateTime desde,
                                        @Param("hasta") LocalDateTime hastaExclusivo);
}

