package galacticos_app_back.galacticos.controller;

import galacticos_app_back.galacticos.dto.ReportePagoDocentesDto;
import galacticos_app_back.galacticos.service.ReportePagoDocenteService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reportes/pagos-docentes")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ReportePagoDocenteController {

    private final ReportePagoDocenteService service;

    public ReportePagoDocenteController(ReportePagoDocenteService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ReportePagoDocentesDto> generarReporte(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Integer idDocente) {
        ReportePagoDocentesDto reporte = service.generarReporte(desde, hasta, nombre, idDocente);
        return ResponseEntity.ok(reporte);
    }
}
