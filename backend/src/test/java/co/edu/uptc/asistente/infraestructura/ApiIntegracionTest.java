package co.edu.uptc.asistente.infraestructura;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración de la API REST: Spring + seguridad + JPA + Flyway sobre H2 (modo PostgreSQL).
 * Verifican el contrato que consume el frontend (README › Contrato de la API).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PlnFalsoConfig.class)
class ApiIntegracionTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    private JsonNode cuerpo(MvcResult r) throws Exception {
        return json.readTree(r.getResponse().getContentAsString());
    }

    private MockHttpServletRequestBuilder conJson(MockHttpServletRequestBuilder b, Object cuerpo) throws Exception {
        return b.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(cuerpo));
    }

    private String sesion() throws Exception {
        MvcResult r = mvc.perform(conJson(post("/api/chat/sesiones"), java.util.Map.of("canal", "web")))
                .andExpect(status().isCreated()).andReturn();
        return cuerpo(r).get("sesionId").asText();
    }

    private String token(String correo, String clave) throws Exception {
        MvcResult r = mvc.perform(conJson(post("/api/admin/auth/login"),
                java.util.Map.of("correo", correo, "password", clave))).andExpect(status().isOk()).andReturn();
        return "Bearer " + cuerpo(r).get("token").asText();
    }

    private String admin() throws Exception {
        return token("admin@uptc.edu.co", "Admin2026*");
    }

    // ------------------------------------------------------------------ chat (IF-01)
    @Test
    void respondeUnaConsultaConConfianzaAlta() throws Exception {
        String s = sesion();
        mvc.perform(conJson(post("/api/chat/consultas"), java.util.Map.of("sesionId", s, "texto",
                        "¿Cuándo son las matrículas?")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("respuesta"))
                .andExpect(jsonPath("$.intent").value("consulta_matricula"))
                .andExpect(jsonPath("$.categoria").value("académico"))
                .andExpect(jsonPath("$.texto", containsString("matrícula")));
    }

    @Test
    void escalaConConfianzaBajaYEntregaLaDependencia() throws Exception {
        String s = sesion();
        mvc.perform(conJson(post("/api/chat/consultas"), java.util.Map.of("sesionId", s, "texto", "algo ambiguo")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("escalamiento"))
                .andExpect(jsonPath("$.dependencia.clave").value("registro"))
                .andExpect(jsonPath("$.dependencia.nombre").value("Registro y Control Académico"));
    }

    @Test
    void fueraDeDominioEscalaAAtencionAlCiudadano() throws Exception {
        String s = sesion();
        mvc.perform(conJson(post("/api/chat/consultas"), java.util.Map.of("sesionId", s, "texto",
                        "¿quién ganó el partido?")))
                .andExpect(jsonPath("$.tipo").value("escalamiento"))
                .andExpect(jsonPath("$.dependencia.clave").value("general"))
                .andExpect(jsonPath("$.texto", containsString("fuera de los temas")));
    }

    @Test
    void validaLongitudYSesionCerrada() throws Exception {
        String s = sesion();
        mvc.perform(conJson(post("/api/chat/consultas"), java.util.Map.of("sesionId", s, "texto", "a".repeat(501))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("texto"));
        mvc.perform(post("/api/chat/sesiones/" + s + "/cerrar")).andExpect(status().isOk());
        mvc.perform(conJson(post("/api/chat/consultas"), java.util.Map.of("sesionId", s, "texto", "hola")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SESION_CERRADA"));
    }

    @Test
    void siElPlnSeCaeResponde503ConDatosDeContacto() throws Exception {
        String s = sesion();
        mvc.perform(conJson(post("/api/chat/consultas"), java.util.Map.of("sesionId", s, "texto", "pln caido")))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.codigo").value("PLN_NO_DISPONIBLE"))
                .andExpect(jsonPath("$.dependencia.clave").value("general"));
    }

    @Test
    void temasRapidos() throws Exception {
        mvc.perform(get("/api/chat/temas")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(5)));
    }

    // ------------------------------------------------------------------ panel (IF-05)
    @Test
    void elPanelExigeToken() throws Exception {
        mvc.perform(get("/api/admin/metricas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/metricas").header("Authorization", "Bearer token.falso.x"))
                .andExpect(status().isUnauthorized());
        mvc.perform(conJson(post("/api/admin/auth/login"),
                        java.util.Map.of("correo", "admin@uptc.edu.co", "password", "incorrecta")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Usuario o contraseña incorrectos"));
    }

    @Test
    void loginDevuelveJwtConLosDatosQueUsaElFrontend() throws Exception {
        MvcResult r = mvc.perform(conJson(post("/api/admin/auth/login"),
                        java.util.Map.of("correo", "admin@uptc.edu.co", "password", "Admin2026*")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.rol").value("superadministrador"))
                .andReturn();
        String[] partes = cuerpo(r).get("token").asText().split("\\.");
        JsonNode payload = json.readTree(java.util.Base64.getUrlDecoder().decode(partes[1]));
        assertEquals("admin@uptc.edu.co", payload.get("correo").asText());
        assertTrue(payload.get("exp").asLong() > payload.get("iat").asLong());
    }

    @Test
    void metricasYExportacion() throws Exception {
        String s = sesion();
        mvc.perform(conJson(post("/api/chat/consultas"), java.util.Map.of("sesionId", s, "texto", "matrícula")));
        String t = admin();
        mvc.perform(get("/api/admin/metricas?rango=7d").header("Authorization", t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.serie", hasSize(7)))
                .andExpect(jsonPath("$.slaDisponibilidad").value(0.95));
        mvc.perform(get("/api/admin/consultas?rango=7d").header("Authorization", t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].sesionId").exists());
        mvc.perform(get("/api/admin/consultas/exportar?rango=7d").header("Authorization", t))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString(".csv")))
                .andExpect(content().contentTypeCompatibleWith("text/csv"));
    }

    @Test
    void baseDeConocimientoCompleta() throws Exception {
        String t = admin();
        mvc.perform(get("/api/admin/intenciones?pageSize=50").header("Authorization", t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", greaterThanOrEqualTo(34)));

        MvcResult creada = mvc.perform(conJson(post("/api/admin/intenciones").header("Authorization", t),
                        java.util.Map.of("nombre", "prueba_integracion", "categoria", "administrativo",
                                "descripcion", "Creada en la prueba", "dependencia", "general",
                                "respuesta", "Respuesta de prueba",
                                "ejemplos", java.util.List.of("frase uno", "frase dos", "frase tres"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ejemplos", hasSize(3)))
                .andReturn();
        long id = cuerpo(creada).get("id").asLong();
        long respuestaId = cuerpo(creada).get("respuestas").get(0).get("id").asLong();

        mvc.perform(conJson(post("/api/admin/intenciones/" + id + "/respuestas").header("Authorization", t),
                        java.util.Map.of("texto", "Otra forma de decirlo")))
                .andExpect(jsonPath("$.respuestas", hasSize(2)));
        mvc.perform(conJson(patch("/api/admin/intenciones/" + id + "/respuestas/" + respuestaId)
                        .header("Authorization", t), java.util.Map.of("activa", false)))
                .andExpect(jsonPath("$.respuestas[0].activa").value(false));
        mvc.perform(get("/api/admin/intenciones/" + id + "/historial").header("Authorization", t))
                .andExpect(jsonPath("$[0].tipoEvento").value("IntencionCreada"))
                .andExpect(jsonPath("$[2].tipoEvento").value("RespuestaDesactivada"));

        mvc.perform(conJson(post("/api/admin/intenciones").header("Authorization", t),
                        java.util.Map.of("nombre", "prueba_integracion", "categoria", "administrativo",
                                "respuesta", "x", "ejemplos", java.util.List.of("a", "b", "c"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.campo").value("nombre"));
    }

    @Test
    void permisosPorRol() throws Exception {
        String gestor = token("gestor@uptc.edu.co", "Gestor2026*");
        mvc.perform(get("/api/admin/usuarios").header("Authorization", gestor)).andExpect(status().isForbidden());
        mvc.perform(conJson(put("/api/admin/configuracion").header("Authorization", gestor),
                java.util.Map.of("umbralConfianza", 0.7))).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/usuarios").header("Authorization", admin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
    }

    @Test
    void configuracionRespetaLosRangos() throws Exception {
        String t = admin();
        mvc.perform(conJson(put("/api/admin/configuracion").header("Authorization", t),
                        java.util.Map.of("umbralConfianza", 0.9)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("umbralConfianza"));
        mvc.perform(conJson(put("/api/admin/configuracion").header("Authorization", t),
                        java.util.Map.of("umbralConfianza", 0.6, "inactividadSesionMin", 30)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.umbralConfianza").value(0.6));
        mvc.perform(get("/api/admin/auditoria").header("Authorization", t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accion").exists());
    }

    @Test
    void saludDelServicio() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
    }
}
