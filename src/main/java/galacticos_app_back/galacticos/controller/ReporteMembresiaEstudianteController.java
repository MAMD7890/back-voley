package galacticos_app_back.galacticos.controller;

import galacticos_app_back.galacticos.dto.ReporteMembresiaEstudianteDto;
import galacticos_app_back.galacticos.service.ReporteMembresiaEstudianteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reportes/membresias")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ReporteMembresiaEstudianteController {

    private final ReporteMembresiaEstudianteService service;

    public ReporteMembresiaEstudianteController(ReporteMembresiaEstudianteService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ReporteMembresiaEstudianteDto> generarReporte(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) Integer equipoId,
            @RequestParam(required = false) Integer sedeId) {
        ReporteMembresiaEstudianteDto reporte = service.generarReporte(desde, hasta, equipoId, sedeId);
        return ResponseEntity.ok(reporte);
    }
}
