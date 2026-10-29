package pe.edu.upeu.autoandes.service;
import java.time.LocalDate;
import java.util.List;
import pe.edu.upeu.autoandes.dto.IngresoPorServicioDTO;
public interface ReporteService { List<IngresoPorServicioDTO> ingresos(LocalDate desde, LocalDate hasta); }

