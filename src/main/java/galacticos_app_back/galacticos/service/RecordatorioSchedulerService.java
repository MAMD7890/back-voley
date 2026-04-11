package galacticos_app_back.galacticos.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Fachada del servicio de recordatorios para uso desde el controlador admin.
 * Delega la lógica real en TareasInternasService.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RecordatorioSchedulerService {

    private final TareasInternasService tareasInternasService;

    /**
     * Ejecuta manualmente el envío de recordatorios WhatsApp.
     * Equivale a la tarea programada de las 10:00 AM.
     */
    public Map<String, Object> ejecutarManualmente() {
        log.info("⚡ Ejecución manual de recordatorios iniciada");
        return tareasInternasService.ejecutarEnvioRecordatorios();
    }

    /**
     * Retorna estadísticas del servicio de recordatorios.
     */
    public Map<String, Object> obtenerEstadisticas() {
        return tareasInternasService.obtenerEstadisticas();
    }
}
