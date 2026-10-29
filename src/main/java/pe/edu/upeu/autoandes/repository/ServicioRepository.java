package pe.edu.upeu.autoandes.repository;

import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.autoandes.entity.Servicio;

public interface ServicioRepository extends JpaRepository<Servicio, Long> {
    boolean existsByCodigo(String codigo);
    boolean existsByCodigoAndIdNot(String codigo, Long id);
    @Query(value = "select count(*) from detalle_orden where servicio_id = :id", nativeQuery = true)
    long contarUsos(@Param("id") Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Servicio s where s.id = :id")
    Optional<Servicio> buscarParaActualizar(@Param("id") Long id);
}

