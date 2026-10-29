package pe.edu.upeu.autoandes.security;

import java.util.*;
import static pe.edu.upeu.autoandes.security.Permiso.*;

public final class PermisosPorRol {
    private PermisosPorRol() {}
    private static final Map<String, Set<Permiso>> MATRIZ = Map.of(
            "ADMIN", Set.copyOf(EnumSet.allOf(Permiso.class)),
            "ASESOR", Set.of(SERVICIO_LEER, VEHICULO_LEER, VEHICULO_ESCRIBIR, ORDEN_LEER, ORDEN_REGISTRAR),
            "JEFE_TALLER", Set.of(SERVICIO_LEER, SERVICIO_ESCRIBIR, VEHICULO_LEER, ORDEN_LEER, ORDEN_ANULAR, REPORTE_VER));

    public static List<String> permisos(Collection<String> roles) {
        return roles.stream().flatMap(rol -> MATRIZ.getOrDefault(rol, Set.of()).stream())
                .map(Enum::name).distinct().sorted().toList();
    }
}

