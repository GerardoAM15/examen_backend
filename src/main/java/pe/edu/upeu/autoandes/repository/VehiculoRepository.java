package pe.edu.upeu.autoandes.repository;

import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.autoandes.entity.Vehiculo;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {
    boolean existsByPlaca(String placa);
    boolean existsByPlacaAndIdNot(String placa, Long id);
    List<Vehiculo> findByClienteIdOrderByPlacaAsc(Long clienteId);
    @Query(value = "select count(*) from orden_servicio where vehiculo_id = :id", nativeQuery = true)
    long contarOrdenes(@Param("id") Long id);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vehiculo v where v.id = :id")
    Optional<Vehiculo> buscarParaActualizar(@Param("id") Long id);
}

