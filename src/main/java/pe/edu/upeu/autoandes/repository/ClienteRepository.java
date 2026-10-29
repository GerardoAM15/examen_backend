package pe.edu.upeu.autoandes.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.edu.upeu.autoandes.entity.Cliente;
public interface ClienteRepository extends JpaRepository<Cliente, Long> {}

