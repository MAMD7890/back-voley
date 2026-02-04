package galacticos_app_back.galacticos.controller;

import galacticos_app_back.galacticos.dto.AttendanceRequest;
import galacticos_app_back.galacticos.dto.AttendanceResult;
import galacticos_app_back.galacticos.dto.StudentExistenceDto;
import galacticos_app_back.galacticos.entity.Asistencia;
import galacticos_app_back.galacticos.service.AsistenciaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/asistencia")
public class AsistenciaController {

    private final AsistenciaService asistenciaService;

    public AsistenciaController(AsistenciaService asistenciaService) {
        this.asistenciaService = asistenciaService;
    }

    @PostMapping("/registrar")
    public ResponseEntity<List<AttendanceResult>> registrar(@RequestBody List<AttendanceRequest> requests) {
        List<AttendanceResult> res = asistenciaService.registrarAsistencias(requests);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/estudiante/{id}")
    public ResponseEntity<List<Asistencia>> getAsistenciasPorEstudiante(@PathVariable Integer id) {
        List<Asistencia> res = asistenciaService.getAsistenciasPorEstudiante(id);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/fecha")
    public ResponseEntity<List<StudentExistenceDto>> existenciasPorFecha(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<StudentExistenceDto> res = asistenciaService.existenciasPorFecha(date);
        return ResponseEntity.ok(res);
    }
}
