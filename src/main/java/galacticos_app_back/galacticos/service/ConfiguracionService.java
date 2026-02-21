package galacticos_app_back.galacticos.service;

import galacticos_app_back.galacticos.dto.ConfiguracionDTO;
import galacticos_app_back.galacticos.entity.Configuracion;
import galacticos_app_back.galacticos.repository.ConfiguracionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ConfiguracionService {
    
    @Autowired
    private ConfiguracionRepository configuracionRepository;
    
    // Constantes de claves de configuración
    public static final String PRECIO_MATRICULA = "PRECIO_MATRICULA";
    public static final String PRECIO_MENSUALIDAD = "PRECIO_MENSUALIDAD";
    public static final String NOMBRE_INSTITUCION = "NOMBRE_INSTITUCION";
    public static final String TELEFONO_CONTACTO = "TELEFONO_CONTACTO";
    public static final String EMAIL_CONTACTO = "EMAIL_CONTACTO";
    
    /**
     * Obtener configuración por clave
     */
    public Optional<ConfiguracionDTO> obtenerConfiguracion(String clave) {
        return configuracionRepository.findByClave(clave)
                .map(this::convertirADTO);
    }
    
    /**
     * Obtener todas las configuraciones
     */
    public List<ConfiguracionDTO> obtenerTodasLasConfiguraciones() {
        return configuracionRepository.findAll()
                .stream()
                .map(this::convertirADTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtener precio de matrícula
     */
    public BigDecimal obtenerPrecioMatricula() {
        return obtenerConfiguracion(PRECIO_MATRICULA)
                .map(config -> new BigDecimal(config.getValor()))
                .orElse(BigDecimal.ZERO);
    }
    
    /**
     * Actualizar o crear configuración
     */
    @Transactional
    public ConfiguracionDTO guardarConfiguracion(ConfiguracionDTO configDTO) {
        if (configDTO.getClave() == null || configDTO.getClave().trim().isEmpty()) {
            throw new IllegalArgumentException("La clave de configuración es requerida");
        }
        
        Configuracion config = configuracionRepository.findByClave(configDTO.getClave())
                .orElse(new Configuracion());
        
        config.setClave(configDTO.getClave());
        config.setDescripcion(configDTO.getDescripcion());
        config.setValor(configDTO.getValor());
        config.setTipo(configDTO.getTipo() != null ? configDTO.getTipo() : "STRING");
        
        Configuracion configGuardada = configuracionRepository.save(config);
        return convertirADTO(configGuardada);
    }
    
    /**
     * Actualizar precio de matrícula
     */
    @Transactional
    public ConfiguracionDTO actualizarPrecioMatricula(BigDecimal nuevoprecio) {
        if (nuevoprecio == null || nuevoprecio.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio debe ser válido");
        }
        
        ConfiguracionDTO configDTO = new ConfiguracionDTO();
        configDTO.setClave(PRECIO_MATRICULA);
        configDTO.setDescripcion("Precio de matrícula para nuevos estudiantes");
        configDTO.setValor(nuevoprecio.toString());
        configDTO.setTipo("BIGDECIMAL");
        
        return guardarConfiguracion(configDTO);
    }
    
    /**
     * Eliminar configuración
     */
    @Transactional
    public void eliminarConfiguracion(Integer idConfiguracion) {
        Configuracion config = configuracionRepository.findById(idConfiguracion)
                .orElseThrow(() -> new IllegalArgumentException("Configuración no encontrada"));
        configuracionRepository.delete(config);
    }
    
    /**
     * Convertir entidad a DTO
     */
    private ConfiguracionDTO convertirADTO(Configuracion config) {
        return ConfiguracionDTO.builder()
                .idConfiguracion(config.getIdConfiguracion())
                .clave(config.getClave())
                .descripcion(config.getDescripcion())
                .valor(config.getValor())
                .tipo(config.getTipo())
                .build();
    }
}
