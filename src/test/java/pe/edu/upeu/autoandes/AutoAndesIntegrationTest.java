package pe.edu.upeu.autoandes;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.*;
import pe.edu.upeu.autoandes.config.JwtService;
import pe.edu.upeu.autoandes.mapper.OrdenMapper;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnabledIfEnvironmentVariable(named = "RUN_ORACLE_TESTS", matches = "true")
class AutoAndesIntegrationTest {
    @Value("${local.server.port}") int port;
    @Value("${app.jwt.secret}") String secret;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @MockitoSpyBean OrdenMapper ordenMapper;
    private final HttpClient http = HttpClient.newHttpClient();
    private final List<Long> ordenesCreadas = new ArrayList<>();
    private final List<Long> serviciosCreados = new ArrayList<>();
    private final List<Long> vehiculosCreados = new ArrayList<>();
    private String admin;
    private String asesor;
    private String jefe;

    @BeforeEach void autenticar() throws Exception {
        admin = login("admin"); asesor = login("asesor"); jefe = login("jefe");
    }
    @AfterEach void limpiarSoloDatosCreadosPorLaPrueba() {
        for (Long id : ordenesCreadas) {
            jdbc.update("delete from detalle_orden where orden_id = ?", id);
            jdbc.update("delete from orden_servicio where id = ?", id);
        }
        for (Long id : vehiculosCreados) jdbc.update("delete from vehiculo where id = ?", id);
        for (Long id : serviciosCreados) jdbc.update("delete from servicio where id = ?", id);
    }

    @Test void loginContrasenasYToken() throws Exception {
        var correcto = call("POST", "/auth/login", null, Map.of("username", "admin", "password", "admin123"), 200);
        assertThat(correcto.path("tipo").asText()).isEqualTo("Bearer");
        assertThat(correcto.path("expiraEnSegundos").asLong()).isEqualTo(3600);
        assertThat(correcto.has("password")).isFalse();
        var desconocido = call("POST", "/auth/login", null, Map.of("username", "nadie", "password", "nadie123"), 401);
        var incorrecto = call("POST", "/auth/login", null, Map.of("username", "admin", "password", "incorrecta"), 401);
        var inactivo = call("POST", "/auth/login", null, Map.of("username", "inactivo", "password", "inactivo123"), 401);
        assertThat(desconocido.path("mensaje").asText()).isEqualTo(incorrecto.path("mensaje").asText())
                .isEqualTo(inactivo.path("mensaje").asText());
        call("POST", "/auth/login", null, Map.of(), 400);
        for (String ruta : List.of("/clientes", "/servicios", "/vehiculos", "/ordenes", "/reportes/ingresos-por-servicio", "/auth/me"))
            call("GET", ruta, null, null, 401);
        call("GET", "/auth/me", "token.alterado.invalido", null, 401);
        var pasado = new JwtService(secret, Clock.offset(Clock.systemUTC(), Duration.ofHours(-2)));
        String vencido = pasado.generarToken("admin", List.of("ADMIN"), List.of("REPORTE_VER"));
        call("GET", "/auth/me", vencido, null, 401);
        var actual = call("GET", "/auth/me", asesor, null, 200);
        assertThat(actual.path("username").asText()).isEqualTo("asesor");
        assertThat(actual.has("password")).isFalse();
        for (String hash : jdbc.queryForList("select password from usuarios", String.class))
            assertThat(hash).startsWith("$2a$").hasSize(60);
    }

    @Test void permisosSeVerificanEnBackend() throws Exception {
        call("GET", "/reportes/ingresos-por-servicio", asesor, null, 403);
        call("PATCH", "/ordenes/1/anular", asesor, Map.of("motivo", "Cliente cancelo la visita"), 403);
        call("POST", "/ordenes", jefe, Map.of(), 403);
        call("POST", "/servicios", asesor, Map.of(), 403);
        call("POST", "/vehiculos", jefe, Map.of(), 403);
        call("GET", "/clientes", jefe, null, 200);
        call("GET", "/servicios", asesor, null, 200);
    }

    @Test void crudServicioConErroresYBaja() throws Exception {
        var payload = new LinkedHashMap<String, Object>(Map.of("codigo", "SRV-990", "nombre", "Servicio prueba",
                "precio", 125.50, "duracionMin", 30, "activo", true));
        var creado = call("POST", "/servicios", jefe, payload, 201);
        long id = creado.path("id").asLong(); serviciosCreados.add(id);
        call("POST", "/servicios", admin, payload, 409);
        payload.put("nombre", "Servicio actualizado");
        assertThat(call("PUT", "/servicios/" + id, jefe, payload, 200).path("nombre").asText()).isEqualTo("Servicio actualizado");
        assertThat(call("PATCH", "/servicios/" + id + "/estado", jefe, Map.of("activo", false), 200).path("activo").asBoolean()).isFalse();
        call("GET", "/servicios/" + id, asesor, null, 200);
        call("DELETE", "/servicios/" + id, jefe, null, 204);
        call("GET", "/servicios/" + id, admin, null, 404);
        payload.put("codigo", "incorrecto"); payload.put("precio", 0); payload.put("duracionMin", 1);
        var invalido = call("POST", "/servicios", admin, payload, 400);
        assertThat(invalido.path("campos").has("codigo")).isTrue();
        long usado = id("select id from servicio where codigo = 'SRV-001'");
        call("DELETE", "/servicios/" + usado, jefe, null, 409);
        call("PATCH", "/servicios/999999999/estado", admin, Map.of("activo", false), 404);
    }

    @Test void crudVehiculoYCliente() throws Exception {
        long cliente = id("select id from cliente where documento = '70000001'");
        var payload = new LinkedHashMap<String, Object>(Map.of("placa", "ZZZ-999", "marca", "Toyota", "modelo", "Yaris",
                "anio", 2026, "clienteId", cliente));
        var creado = call("POST", "/vehiculos", asesor, payload, 201);
        long id = creado.path("id").asLong(); vehiculosCreados.add(id);
        assertThat(creado.path("clienteNombre").asText()).isEqualTo("Ana Torres");
        assertThat(creado.has("cliente")).isFalse();
        call("POST", "/vehiculos", asesor, payload, 409);
        payload.put("modelo", "Corolla");
        call("PUT", "/vehiculos/" + id, asesor, payload, 200);
        var filtrados = call("GET", "/vehiculos?clienteId=" + cliente, asesor, null, 200);
        assertThat(filtrados.isArray()).isTrue();
        call("DELETE", "/vehiculos/" + id, asesor, null, 204);
        call("GET", "/vehiculos/" + id, asesor, null, 404);
        payload.put("clienteId", 999999999);
        call("POST", "/vehiculos", asesor, payload, 404);
        payload.put("placa", "MAL"); payload.put("anio", 1980);
        var error = call("POST", "/vehiculos", asesor, payload, 400);
        assertThat(error.path("campos").has("placa")).isTrue();
        call("DELETE", "/vehiculos/" + id("select id from vehiculo where placa='AAA-111'"), asesor, null, 409);
        call("GET", "/vehiculos?clienteId=999999999", asesor, null, 404);
    }

    @Test void ordenConTercerDetalleInactivoNoDejaCabeceraNiDetalles() throws Exception {
        long vehiculo = id("select id from vehiculo where placa='AAA-111'");
        long s1 = id("select id from servicio where codigo='SRV-001'");
        long s2 = id("select id from servicio where codigo='SRV-002'");
        long s8 = id("select id from servicio where codigo='SRV-008'");
        long cabeceras = id("select count(*) from orden_servicio");
        long detalles = id("select count(*) from detalle_orden");
        var lista = List.of(Map.of("servicioId", s1, "cantidad", 1), Map.of("servicioId", s2, "cantidad", 1),
                Map.of("servicioId", s8, "cantidad", 1));
        var rechazo = call("POST", "/ordenes", asesor, Map.of("vehiculoId", vehiculo, "kilometraje", 45000, "detalles", lista), 409);
        assertThat(rechazo.path("mensaje").asText()).contains("SRV-008");
        assertThat(id("select count(*) from orden_servicio")).isEqualTo(cabeceras);
        assertThat(id("select count(*) from detalle_orden")).isEqualTo(detalles);
        call("POST", "/ordenes", asesor, Map.of("vehiculoId", vehiculo, "kilometraje", 45000, "detalles",
                List.of(Map.of("servicioId", s1, "cantidad", 1), Map.of("servicioId", s1, "cantidad", 2))), 400);
        var kilometraje = call("POST", "/ordenes", asesor, Map.of("vehiculoId", vehiculo, "kilometraje", 44999,
                "detalles", List.of(Map.of("servicioId", s1, "cantidad", 1))), 409);
        assertThat(kilometraje.path("mensaje").asText()).contains("45000");
        call("POST", "/ordenes", asesor, Map.of("vehiculoId", vehiculo, "kilometraje", 45000, "detalles", List.of()), 400);
        call("POST", "/ordenes", asesor, Map.of("vehiculoId", vehiculo, "kilometraje", 45000,
                "detalles", List.of(Map.of("servicioId", s1, "cantidad", 6))), 400);
        call("POST", "/ordenes", asesor, Map.of("vehiculoId", vehiculo, "kilometraje", 45000,
                "detalles", Arrays.asList((Object) null)), 400);
    }

    @Test void falloDespuesDelFlushRevierteCabeceraYDetalles() throws Exception {
        long cabeceras = id("select count(*) from orden_servicio");
        long detalles = id("select count(*) from detalle_orden");
        long vehiculo = id("select id from vehiculo where placa='BBB-222'");
        long servicio = id("select id from servicio where codigo='SRV-001'");
        // Forzar un error despues de saveAndFlush demuestra rollback real, incluso tras INSERT.
        doThrow(new IllegalStateException("Fallo controlado despues de escribir")).when(ordenMapper).toResponse(any());
        call("POST", "/ordenes", asesor, Map.of("vehiculoId", vehiculo, "kilometraje", 200,
                "detalles", List.of(Map.of("servicioId", servicio, "cantidad", 1))), 500);
        assertThat(id("select count(*) from orden_servicio")).isEqualTo(cabeceras);
        assertThat(id("select count(*) from detalle_orden")).isEqualTo(detalles);
    }

    @Test void registroConsultaReporteYAnulacionContraOracle() throws Exception {
        long vehiculo = id("select id from vehiculo where placa='BBB-222'");
        long servicio = id("select id from servicio where codigo='SRV-001'");
        var antes = jdbc.queryForObject("select nvl(sum(total),0) from orden_servicio where estado='REGISTRADA'", BigDecimal.class);
        var orden = call("POST", "/ordenes", asesor, Map.of("vehiculoId", vehiculo, "kilometraje", 100,
                "detalles", List.of(Map.of("servicioId", servicio, "cantidad", 2)), "total", 1), 201);
        long id = orden.path("id").asLong(); ordenesCreadas.add(id);
        assertThat(orden.path("numero").asText()).matches("OS-[0-9]{6}");
        assertThat(orden.path("usuarioRegistro").asText()).isEqualTo("asesor");
        assertThat(orden.path("total").decimalValue()).isEqualByComparingTo("240");
        assertThat(jdbc.queryForObject("select total from orden_servicio where id=?", BigDecimal.class, id)).isEqualByComparingTo("240");
        assertThat(jdbc.queryForObject("select count(*) from detalle_orden where orden_id=?", Long.class, id)).isEqualTo(1);
        call("GET", "/ordenes/" + id, jefe, null, 200);
        String hoy = LocalDate.now(ZoneId.of("America/Lima")).toString();
        var filtradas = call("GET", "/ordenes?placa=BBB-222&estado=REGISTRADA&desde=" + hoy + "&hasta=" + hoy, asesor, null, 200);
        assertThat(filtradas.size()).isEqualTo(1);
        assertThat(call("GET", "/ordenes?placa=NO-EXISTE", asesor, null, 200).isEmpty()).isTrue();
        var reporte = call("GET", "/reportes/ingresos-por-servicio", jefe, null, 200);
        BigDecimal suma = BigDecimal.ZERO;
        for (JsonNode fila : reporte) suma = suma.add(fila.path("importeTotal").decimalValue());
        assertThat(suma).isEqualByComparingTo(antes.add(new BigDecimal("240")));
        call("PATCH", "/ordenes/" + id + "/anular", jefe, Map.of("motivo", "corto"), 400);
        var anulada = call("PATCH", "/ordenes/" + id + "/anular", jefe, Map.of("motivo", "Cliente cancelo la atencion"), 200);
        assertThat(anulada.path("estado").asText()).isEqualTo("ANULADA");
        call("PATCH", "/ordenes/" + id + "/anular", jefe, Map.of("motivo", "Cliente cancelo la atencion"), 409);
        var reporteDespues = call("GET", "/reportes/ingresos-por-servicio", jefe, null, 200);
        BigDecimal sumaDespues = BigDecimal.ZERO;
        for (JsonNode fila : reporteDespues) sumaDespues = sumaDespues.add(fila.path("importeTotal").decimalValue());
        assertThat(sumaDespues).isEqualByComparingTo(antes);
        long previa = id("select id from orden_servicio where numero='OS-000001'");
        call("PATCH", "/ordenes/" + previa + "/anular", jefe, Map.of("motivo", "Cliente cancelo la atencion"), 409);
        call("GET", "/ordenes?estado=OTRO", asesor, null, 400);
        call("GET", "/ordenes?desde=2026-11-01&hasta=2026-10-01", asesor, null, 400);
        call("GET", "/reportes/ingresos-por-servicio?desde=2026-11-01&hasta=2026-10-01", jefe, null, 400);
    }

    @Test void corsYSwaggerDisponibles() throws Exception {
        var preflight = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/ordenes"))
                .header("Origin", "http://localhost:4200")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type")
                .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build();
        var res = http.send(preflight, HttpResponse.BodyHandlers.ofString());
        assertThat(res.statusCode()).isEqualTo(200);
        assertThat(res.headers().firstValue("Access-Control-Allow-Origin")).contains("http://localhost:4200");
        var swagger = http.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v3/api-docs")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertThat(swagger.statusCode()).isEqualTo(200);
        assertThat(mapper.readTree(swagger.body()).path("paths").has("/api/v1/ordenes")).isTrue();
        call("GET", "/ruta-inexistente", admin, null, 404);
    }

    private long id(String sql) { return jdbc.queryForObject(sql, Long.class); }
    private String login(String username) throws Exception {
        return call("POST", "/auth/login", null, Map.of("username", username, "password", username + "123"), 200).path("token").asText();
    }
    private JsonNode call(String method, String ruta, String token, Object body, int status) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1" + ruta))
                .header("Content-Type", "application/json");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() :
                HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body)));
        var response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as(method + " " + ruta + " -> " + response.body()).isEqualTo(status);
        JsonNode json = response.body().isBlank() ? mapper.nullNode() : mapper.readTree(response.body());
        if (status >= 400) {
            assertThat(json.path("status").asInt()).isEqualTo(status);
            assertThat(json.has("timestamp") && json.has("error") && json.has("mensaje") && json.has("ruta")).isTrue();
            if (status != 400) assertThat(json.has("campos")).isFalse();
        }
        return json;
    }
}

