package galacticos_app_back.galacticos.controller;

import galacticos_app_back.galacticos.dto.MembresiaCoreDTO;
import galacticos_app_back.galacticos.entity.Pago;
import galacticos_app_back.galacticos.service.MembresiaCoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
public class MembresiaCoreController {

    @Autowired
    private MembresiaCoreService membresiaCoreService;

    @Value("${app.internal.api-key:#{null}}")
    private String internalApiKey;

    // ─── Endpoint público (autenticado por JWT) ───────────────────────────────

    @GetMapping("/api/membresias-core/estudiante/{id}/historico")
    public ResponseEntity<List<MembresiaCoreDTO>> obtenerHistorico(@PathVariable Integer id) {
        return ResponseEntity.ok(membresiaCoreService.obtenerHistorico(id));
    }

    @GetMapping("/api/membresias-core/estudiante/{id}/activa")
    public ResponseEntity<MembresiaCoreDTO> obtenerActiva(@PathVariable Integer id) {
        return membresiaCoreService.obtenerMembresiaActiva(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ─── Endpoints internos (protegidos por API Key) ──────────────────────────

    @PostMapping("/api/internal/membresias-core/jobs/activar-periodos")
    public ResponseEntity<Map<String, Object>> job1ActivarPeriodos(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!validarApiKey(apiKey)) return unauthorized();
        return ResponseEntity.ok(membresiaCoreService.ejecutarJob1ActivarPeriodosFuturos());
    }

    @PostMapping("/api/internal/membresias-core/jobs/detectar-vencimientos")
    public ResponseEntity<Map<String, Object>> job2DetectarVencimientos(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!validarApiKey(apiKey)) return unauthorized();
        return ResponseEntity.ok(membresiaCoreService.ejecutarJob2DetectarVencimientos());
    }

    @PostMapping("/api/internal/membresias-core/jobs/cancelar-gracias")
    public ResponseEntity<Map<String, Object>> job3CancelarGracias(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!validarApiKey(apiKey)) return unauthorized();
        return ResponseEntity.ok(membresiaCoreService.ejecutarJob3CancelarGraciasVencidas());
    }

    @PostMapping("/api/internal/membresias-core/jobs/cancelar-acuerdos")
    public ResponseEntity<Map<String, Object>> job4CancelarAcuerdos(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!validarApiKey(apiKey)) return unauthorized();
        return ResponseEntity.ok(membresiaCoreService.ejecutarJob4CancelarAcuerdosVencidos());
    }

    @GetMapping("/api/internal/membresias-core/jobs/revertir-membresias-indebidas/preview")
    public ResponseEntity<Map<String, Object>> previewRevertirMembresiasIndebidas(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate fecha) {
        if (!validarApiKey(apiKey)) return unauthorized();
        return ResponseEntity.ok(membresiaCoreService.previsualizarReversionIndebida(fecha));
    }

    @PostMapping("/api/internal/membresias-core/jobs/revertir-membresias-indebidas")
    public ResponseEntity<Map<String, Object>> jobRevertirMembresiasIndebidas(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) LocalDate fecha) {
        if (!validarApiKey(apiKey)) return unauthorized();
        return ResponseEntity.ok(membresiaCoreService.revertirMembresiasCreadasIndebidamente(fecha));
    }

    @GetMapping("/api/internal/membresias-core/jobs/corregir-al-dia-sin-membresia/preview")
    public ResponseEntity<Map<String, Object>> previewCorregirAlDiaSinMembresia(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!validarApiKey(apiKey)) return unauthorized();
        return ResponseEntity.ok(membresiaCoreService.previsualizarCorreccionAlDiaSinMembresia());
    }

    @PostMapping("/api/internal/membresias-core/jobs/corregir-al-dia-sin-membresia")
    public ResponseEntity<Map<String, Object>> jobCorregirAlDiaSinMembresia(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!validarApiKey(apiKey)) return unauthorized();
        return ResponseEntity.ok(membresiaCoreService.ejecutarJobCorregirAlDiaSinMembresia());
    }

    @GetMapping("/api/internal/membresias-core/jobs/recuperar-membresias-faltantes/preview")
    public ResponseEntity<Map<String, Object>> previewRecuperarMembresiasFaltantes(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!validarApiKey(apiKey)) return unauthorized();
        return ResponseEntity.ok(membresiaCoreService.previsualizarRecuperarMembresiasFaltantes());
    }

    @PostMapping("/api/internal/membresias-core/jobs/recuperar-membresias-faltantes")
    public ResponseEntity<Map<String, Object>> jobRecuperarMembresiasFaltantes(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!validarApiKey(apiKey)) return unauthorized();
        return ResponseEntity.ok(membresiaCoreService.ejecutarJobRecuperarMembresiasFaltantes());
    }

    @GetMapping("/api/internal/membresias-core/jobs/corregir-fechas-diapago/preview")
    public ResponseEntity<Map<String, Object>> previewCorregirFechasDiaPago(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!validarApiKey(apiKey)) return unauthorized();
        return ResponseEntity.ok(membresiaCoreService.previsualizarCorreccionFechas());
    }

    @PostMapping("/api/internal/membresias-core/jobs/corregir-fechas-diapago")
    public ResponseEntity<Map<String, Object>> jobCorregirFechasDiaPago(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!validarApiKey(apiKey)) return unauthorized();
        return ResponseEntity.ok(membresiaCoreService.corregirFechasCalculadasConDiaPago());
    }

    @PostMapping("/api/internal/membresias-core/jobs/sincronizar-estados")
    public ResponseEntity<Map<String, Object>> job5SincronizarEstados(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!validarApiKey(apiKey)) return unauthorized();
        return ResponseEntity.ok(membresiaCoreService.ejecutarJobSincronizarEstados());
    }

    @PostMapping("/api/internal/membresias-core/jobs/corregir-duplicados")
    public ResponseEntity<Map<String, Object>> jobCorregirDuplicados(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!validarApiKey(apiKey)) return unauthorized();
        return ResponseEntity.ok(membresiaCoreService.corregirPagosDuplicados());
    }

    @PostMapping("/api/internal/membresias-core/jobs/migrar-acuerdos")
    public ResponseEntity<Map<String, Object>> jobMigrarAcuerdos(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!validarApiKey(apiKey)) return unauthorized();
        return ResponseEntity.ok(membresiaCoreService.migrarAcuerdosPendientes());
    }

    @PostMapping("/api/internal/membresias-core/migracion")
    public ResponseEntity<Map<String, Object>> migrar(
            @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {
        if (!validarApiKey(apiKey)) return unauthorized();
        return ResponseEntity.ok(membresiaCoreService.migrarMembresiasExistentes());
    }

    // ─── Pagos del estudiante ─────────────────────────────────────────────────

    @GetMapping("/api/membresias-core/estudiante/{id}/pagos")
    public ResponseEntity<List<Pago>> obtenerPagos(@PathVariable Integer id) {
        return ResponseEntity.ok(membresiaCoreService.obtenerPagosEstudiante(id));
    }

    // ─── Vigencia activa del estudiante ──────────────────────────────────────

    @GetMapping("/api/membresias-core/estudiante/{id}/vigencia")
    public ResponseEntity<?> obtenerVigencia(@PathVariable Integer id) {
        Map<String, Object> vigencia = membresiaCoreService.obtenerVigencia(id);
        if (vigencia == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(vigencia);
    }

    // ─── Cambiar fechaInicio y/o fechaFin de una membresía (admin) ───────────

    @PatchMapping("/api/membresias-core/{id}/fechas")
    public ResponseEntity<?> cambiarFechas(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        try {
            String rawInicio = body.get("fechaInicio");
            String rawFin    = body.get("fechaFin");
            if (rawInicio == null && rawFin == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Debe enviar al menos fechaInicio o fechaFin"));
            }
            LocalDate nuevaFechaInicio = rawInicio != null ? LocalDate.parse(rawInicio) : null;
            LocalDate nuevaFechaFin    = rawFin    != null ? LocalDate.parse(rawFin)    : null;
            MembresiaCoreDTO actualizada = membresiaCoreService.cambiarFechas(id, nuevaFechaInicio, nuevaFechaFin);
            return ResponseEntity.ok(actualizada);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Endpoint para crear acuerdo de pago (autenticado por JWT, admin) ─────

    @PostMapping("/api/membresias-core/acuerdo-pago")
    public ResponseEntity<?> crearAcuerdoPago(@RequestBody Map<String, Object> body) {
        try {
            Integer idEstudiante = (Integer) body.get("idEstudiante");
            LocalDate fechaLimite = LocalDate.parse((String) body.get("fechaLimiteCompromiso"));
            String observacion = (String) body.get("observacion");
            MembresiaCoreDTO acuerdo = membresiaCoreService.crearAcuerdoPago(
                    idEstudiante, fechaLimite, observacion);
            return ResponseEntity.ok(acuerdo);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Cambiar fechas por idEstudiante ──────────────────────────────────────

    @PatchMapping("/api/membresias-core/estudiante/{idEstudiante}/fechas")
    public ResponseEntity<?> cambiarFechasPorEstudiante(
            @PathVariable Integer idEstudiante,
            @RequestBody Map<String, String> body) {
        try {
            String rawInicio = body.get("fechaInicio");
            String rawFin    = body.get("fechaFin");
            if (rawInicio == null && rawFin == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Debe enviar al menos fechaFin"));
            }
            LocalDate nuevaFechaInicio = rawInicio != null ? LocalDate.parse(rawInicio) : null;
            LocalDate nuevaFechaFin    = rawFin    != null ? LocalDate.parse(rawFin)    : null;
            MembresiaCoreDTO resultado = membresiaCoreService.cambiarFechasPorEstudiante(
                    idEstudiante, nuevaFechaInicio, nuevaFechaFin);
            return ResponseEntity.ok(resultado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private boolean validarApiKey(String apiKey) {
        if (internalApiKey == null || internalApiKey.isBlank()) return true;
        return internalApiKey.equals(apiKey);
    }

    private ResponseEntity<Map<String, Object>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "API Key inválida o ausente"));
    }
}
