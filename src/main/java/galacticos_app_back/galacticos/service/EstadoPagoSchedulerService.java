package galacticos_app_back.galacticos.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Servicio programado para verificar y actualizar los estados de pago de los estudiantes
 * Se ejecuta automáticamente según la configuración del cron
 */
@Service
@Slf4j
public class EstadoPagoSchedulerService {
    
    @Autowired
    private EstudianteService estudianteService;
    
    @Autowired
    private WompiService wompiService;
    
    /**
     * Sincroniza pagos con Wompi cada 5 minutos.
     * Consulta las transacciones aprobadas en Wompi y actualiza los estudiantes
     * cuyo pago no se registró correctamente.
     */
    @Scheduled(fixedRate = 300000) // 5 minutos = 300,000 ms
    public void sincronizarPagosWompi() {
        log.info("🔄 === INICIO: Sincronización automática con Wompi ===");
        try {
            Map<String, Object> resultado = wompiService.sincronizarDesdeTransaccionesWompi();
            
            int vinculados = (int) resultado.getOrDefault("vinculados", 0);
            int yaExistentes = (int) resultado.getOrDefault("yaExistentes", 0);
            int errores = (int) resultado.getOrDefault("errores", 0);
            
            if (vinculados > 0) {
                log.info("✅ Sincronización con Wompi: {} pagos vinculados, {} ya existentes, {} errores", 
                    vinculados, yaExistentes, errores);
            } else {
                log.debug("Sincronización con Wompi: sin nuevos pagos para vincular");
            }
            
        } catch (Exception e) {
            log.error("❌ Error en sincronización automática con Wompi: {}", e.getMessage());
        }
        log.info("🔄 === FIN: Sincronización automática con Wompi ===");
    }
    
    /**
     * Verifica los estados de pago de todos los estudiantes
     * Se ejecuta todos los días a las 6:00 AM
     */
    @Scheduled(cron = "0 0 6 * * ?")
    public void verificarEstadosPagoDiario() {
        log.info("=== INICIO: Verificación automática de estados de pago ===");
        try {
            estudianteService.verificarEstadosPago();
            log.info("=== FIN: Verificación automática completada exitosamente ===");
        } catch (Exception e) {
            log.error("Error en la verificación automática de estados de pago: {}", e.getMessage(), e);
        }
    }
    
    /**
     * Verifica los estados de pago cada primer día del mes a las 00:01
     * Este verifica específicamente los pagos del mes anterior
     */
    @Scheduled(cron = "0 1 0 1 * ?")
    public void verificarEstadosPagoMensual() {
        log.info("=== INICIO: Verificación mensual de estados de pago ===");
        try {
            estudianteService.verificarEstadosPago();
            log.info("=== FIN: Verificación mensual completada exitosamente ===");
        } catch (Exception e) {
            log.error("Error en la verificación mensual de estados de pago: {}", e.getMessage(), e);
        }
    }
}
