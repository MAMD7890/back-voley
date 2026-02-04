package galacticos_app_back.galacticos.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import galacticos_app_back.galacticos.dto.ProfesorAttendanceDto;
import galacticos_app_back.galacticos.dto.ProfesorAttendanceMarkRequest;
import galacticos_app_back.galacticos.dto.ProfesorPagoDto;
import galacticos_app_back.galacticos.entity.AsistenciaProfesor;
import galacticos_app_back.galacticos.service.AsistenciaProfesorService;

@RestController
@RequestMapping("/api/profesor/asistencia")
@CrossOrigin(origins = "*", maxAge = 3600)
public class AsistenciaProfesorController {

    private final AsistenciaProfesorService service;

    public AsistenciaProfesorController(AsistenciaProfesorService service) {
        this.service = service;
    }

    @GetMapping("/list")
    public ResponseEntity<List<ProfesorAttendanceDto>> listar(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer sedeId,
            @RequestParam(required = false) Integer equipoId,
            @RequestParam(required = false) String nombre) {
        List<ProfesorAttendanceDto> res = service.listarProfesoresConFiltroYAsistencia(date, sedeId, equipoId, nombre);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/registrar")
    public ResponseEntity<List<Map<String, Object>>> registrar(@RequestBody List<ProfesorAttendanceMarkRequest> marcas) {
        List<Map<String, Object>> res = service.registrarAsistenciasProfesores(marcas);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/marcas")
    public ResponseEntity<List<AsistenciaProfesor>> marcasPorFecha(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<AsistenciaProfesor> res = service.obtenerTodos().stream().filter(a -> a.getFecha().equals(date)).toList();
        return ResponseEntity.ok(res);
    }

    @GetMapping("/pagos")
    public ResponseEntity<List<ProfesorPagoDto>> calcularPagos(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        List<ProfesorPagoDto> res = service.calcularPagoProfesores(desde, hasta);
        return ResponseEntity.ok(res);
    }
}
