package pe.edu.upeu.autoandes.security;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PermisosPorRolTest {
    @Test void adminTieneTodosLosPermisos() {
        assertThat(PermisosPorRol.permisos(List.of("ADMIN"))).hasSize(Permiso.values().length);
    }
    @Test void asesorNoPuedeAnularNiVerReportes() {
        assertThat(PermisosPorRol.permisos(List.of("ASESOR"))).contains("ORDEN_REGISTRAR", "VEHICULO_ESCRIBIR")
                .doesNotContain("ORDEN_ANULAR", "REPORTE_VER", "SERVICIO_ESCRIBIR");
    }
    @Test void jefeNoRegistraOrdenesNiModificaVehiculos() {
        assertThat(PermisosPorRol.permisos(List.of("JEFE_TALLER"))).contains("ORDEN_ANULAR", "REPORTE_VER")
                .doesNotContain("ORDEN_REGISTRAR", "VEHICULO_ESCRIBIR");
    }
    @Test void variosRolesRecibenLaUnionSinDuplicados() {
        assertThat(PermisosPorRol.permisos(List.of("ASESOR", "JEFE_TALLER")))
                .containsExactlyElementsOf(PermisosPorRol.permisos(List.of("ADMIN")));
        assertThat(PermisosPorRol.permisos(List.of("DESCONOCIDO"))).isEmpty();
    }
}

