package pe.edu.upeu.autoandes.repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.autoandes.entity.*;

public interface OrdenServicioRepository extends JpaRepository<OrdenServicio, Long> {
    Optional<OrdenServicio> findFirstByVehiculoIdAndEstadoOrderByFechaDescIdDesc(Long vehiculoId, EstadoOrden estado);

    @Query("""
            select o from OrdenServicio o join o.vehiculo v
            where (:placa is null or upper(v.placa) like concat('%', concat(upper(:placa), '%')))
              and (:estado is null or o.estado = :estado)
              and (:desde is null or o.fecha >= :desde)
              and (:hasta is null or o.fecha < :hasta)
            order by o.fecha desc, o.id desc
            """)
    List<OrdenServicio> filtrar(@Param("placa") String placa, @Param("estado") EstadoOrden estado,
            @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hastaExclusivo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from OrdenServicio o where o.id = :id")
    Optional<OrdenServicio> buscarParaActualizar(@Param("id") Long id);

    @Query(value = "select orden_numero_seq.nextval from dual", nativeQuery = true)
    Long siguienteNumero();
}

