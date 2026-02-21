package galacticos_app_back.galacticos.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import galacticos_app_back.galacticos.dto.ActualizarEstudianteDTO;
import galacticos_app_back.galacticos.dto.CambioEstadoPagoDTO;
import galacticos_app_back.galacticos.dto.CambioPasswordEstudianteDTO;
import galacticos_app_back.galacticos.dto.EstudianteConEstadoPagoDTO;
import galacticos_app_back.galacticos.dto.ExcelEstudianteImportDTO;
import galacticos_app_back.galacticos.dto.ExcelImportResponseDTO;
import galacticos_app_back.galacticos.dto.ExcelImportResultado;
import galacticos_app_back.galacticos.dto.RegistroPagoEfectivoDTO;
import galacticos_app_back.galacticos.dto.auth.AuthResponse;
import galacticos_app_back.galacticos.dto.auth.RegisterRequest;
import galacticos_app_back.galacticos.entity.Estudiante;
import galacticos_app_back.galacticos.entity.Membresia;
import galacticos_app_back.galacticos.entity.Pago;
import galacticos_app_back.galacticos.entity.Usuario;
import galacticos_app_back.galacticos.repository.EstudianteRepository;
import galacticos_app_back.galacticos.repository.MembresiaRepository;
import galacticos_app_back.galacticos.repository.PagoRepository;
import galacticos_app_back.galacticos.repository.SedeRepository;
import galacticos_app_back.galacticos.repository.UsuarioRepository;

@Service
public class EstudianteService {
    
    @Autowired
    private EstudianteRepository estudianteRepository;
    
    @Autowired
    private AuthService authService;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    @Autowired
    private MembresiaRepository membresiaRepository;
    
    @Autowired
    private PagoRepository pagoRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private FileStorageService fileStorageService;
    
    @Autowired
    private ExcelImportService excelImportService;
    
    @Autowired
    private SedeRepository sedeRepository;
    
    @Autowired
    private galacticos_app_back.galacticos.repository.RolRepository rolRepository;
    
    // Obtener todos los estudiantes
    public List<Estudiante> obtenerTodos() {
        return estudianteRepository.findAll();
    }
    
    // Obtener estudiante por ID
    public Optional<Estudiante> obtenerPorId(Integer id) {
        return estudianteRepository.findById(id);
    }
    
    // Obtener estudiantes activos
    public List<Estudiante> obtenerActivos() {
        return estudianteRepository.findByEstado(true);
    }
    
    // Obtener estudiantes por nivel
    public List<Estudiante> obtenerPorNivel(Estudiante.NivelActual nivel) {
        return estudianteRepository.findByNivelActual(nivel);
    }
    
    // Obtener estudiantes por sede
    public List<Estudiante> obtenerPorSede(Integer idSede) {
        return estudianteRepository.findBySedeIdSede(idSede);
    }
    
    // Crear nuevo estudiante (método original para uso interno)
    public Estudiante crear(Estudiante estudiante) {
        // Establecer estado de pago inicial como PENDIENTE
        if (estudiante.getEstadoPago() == null) {
            estudiante.setEstadoPago(Estudiante.EstadoPago.PENDIENTE);
        }
        return estudianteRepository.save(estudiante);
    }
    
    // Crear nuevo estudiante con usuario automático
    @Transactional
    public AuthResponse crearConUsuario(Estudiante estudiante) {
        // Validar que el correo no esté registrado como usuario
        if (usuarioRepository.findByEmail(estudiante.getCorreoEstudiante()).isPresent()) {
            throw new RuntimeException("Ya existe un usuario registrado con el email: " + estudiante.getCorreoEstudiante());
        }
        
        // Establecer estado de pago inicial como PENDIENTE
        estudiante.setEstadoPago(Estudiante.EstadoPago.PENDIENTE);
        
        // Crear el estudiante
        Estudiante estudianteGuardado = estudianteRepository.save(estudiante);
        System.out.println("Estudiante creado con ID: " + estudianteGuardado.getIdEstudiante() + " - Estado de pago: PENDIENTE");
        
        // Crear membresía automáticamente con estado PENDIENTE_PAGO ANTES del usuario
        Membresia membresia = new Membresia();
        membresia.setEstudiante(estudianteGuardado);
        membresia.setEquipo(null); // Se asignará después de seleccionar equipo
        membresia.setFechaInicio(LocalDate.now());
        membresia.setFechaFin(LocalDate.now().plusMonths(1)); // Membresía mensual
        membresia.setValorMensual(new BigDecimal("50000")); // Valor por defecto
        membresia.setEstado(false); // false = pendiente/inactivo, true = activo/pagado
        
        Membresia membresiaGuardada = membresiaRepository.save(membresia);
        System.out.println("Membresía creada con ID: " + membresiaGuardada.getIdMembresia());
        
        // Crear el registro de usuario automáticamente
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setNombre(estudianteGuardado.getCorreoEstudiante());
        registerRequest.setEmail(estudianteGuardado.getCorreoEstudiante());
        registerRequest.setPassword(estudianteGuardado.getNumeroDocumento());
        registerRequest.setFotoUrl(null);
        registerRequest.setFotoNombre(null);
        
        // Registrar usuario con rol ESTUDIANTE automático
        try {
            AuthResponse authResponse = authService.registerStudent(registerRequest);
            System.out.println("Usuario creado exitosamente");
            
            // Construir información de la membresía
            AuthResponse.MembresiaInfo membresiaInfo = AuthResponse.MembresiaInfo.builder()
                .id(membresiaGuardada.getIdMembresia())
                .estado(membresiaGuardada.getEstado() ? "ACTIVO" : "PENDIENTE")
                .fechaInicio(membresiaGuardada.getFechaInicio())
                .fechaFin(membresiaGuardada.getFechaFin())
                .valorMensual(membresiaGuardada.getValorMensual())
                .equipoNombre(membresiaGuardada.getEquipo() != null ? membresiaGuardada.getEquipo().getNombre() : null)
                .equipoId(membresiaGuardada.getEquipo() != null ? membresiaGuardada.getEquipo().getIdEquipo() : null)
                .build();
                
            // Construir UserInfo completo con membresía
            AuthResponse.UserInfo userInfoCompleto = AuthResponse.UserInfo.builder()
                .id(authResponse.getUser().getId())
                .nombre(authResponse.getUser().getNombre())
                .email(authResponse.getUser().getEmail())
                .fotoUrl(authResponse.getUser().getFotoUrl())
                .rol(authResponse.getUser().getRol())
                .membresia(membresiaInfo)
                .build();
                
            // Construir AuthResponse completa
            return AuthResponse.builder()
                .accessToken(authResponse.getAccessToken())
                .refreshToken(authResponse.getRefreshToken())
                .tokenType(authResponse.getTokenType())
                .expiresIn(authResponse.getExpiresIn())
                .user(userInfoCompleto)
                .build();
                
        } catch (Exception e) {
            System.out.println("Error creando usuario: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
    
    /**
     * Importa masivamente estudiantes desde un archivo Excel
     * Crea automáticamente usuarios con credenciales (email y password)
     * @param dtos Lista de DTOs extraídos del Excel
     * @param sedeId ID de la sede asignada
     * @return Reporte de importación
     */
    @Transactional
    public Map<String, Object> importarEstudiantesDesdeExcel(List<ExcelEstudianteImportDTO> dtos, Integer sedeId) {
        Map<String, Object> reporte = new HashMap<>();
        List<Map<String, Object>> resultados = new ArrayList<>();
        int exitosos = 0;
        int errores = 0;
        
        // Obtener la sede
        Optional<galacticos_app_back.galacticos.entity.Sede> sedeOpt = sedeRepository.findById(sedeId);
        if (!sedeOpt.isPresent()) {
            reporte.put("error", "La sede especificada no existe");
            reporte.put("exitosos", 0);
            reporte.put("errores", dtos.size());
            return reporte;
        }
        
        galacticos_app_back.galacticos.entity.Sede sede = sedeOpt.get();
        
        for (ExcelEstudianteImportDTO dto : dtos) {
            try {
                // Validación básica
                String erroresValidacion = validarDtoEstudiante(dto);
                if (!erroresValidacion.isEmpty()) {
                    resultados.add(Map.of(
                        "fila", dto.getNumeroFila(),
                        "nombre", dto.getNombreCompleto(),
                        "estado", "ERROR",
                        "mensaje", erroresValidacion
                    ));
                    errores++;
                    continue;
                }
                
                // Verificar si el email ya existe
                if (usuarioRepository.findByEmail(dto.getCorreoEstudiante()).isPresent()) {
                    resultados.add(Map.of(
                        "fila", dto.getNumeroFila(),
                        "nombre", dto.getNombreCompleto(),
                        "estado", "ERROR",
                        "mensaje", "El correo ya está registrado en el sistema"
                    ));
                    errores++;
                    continue;
                }
                
                // Verificar si el documento ya existe
                Optional<Estudiante> estudianteExistente = estudianteRepository.findByNumeroDocumento(dto.getNumeroDocumento());
                if (estudianteExistente.isPresent()) {
                    resultados.add(Map.of(
                        "fila", dto.getNumeroFila(),
                        "nombre", dto.getNombreCompleto(),
                        "estado", "ERROR",
                        "mensaje", "El número de documento ya está registrado"
                    ));
                    errores++;
                    continue;
                }
                
                // Crear el estudiante
                Estudiante estudiante = dtoAEstudiante(dto, sede);
                estudiante.setEstadoPago(Estudiante.EstadoPago.PENDIENTE);
                Estudiante estudianteGuardado = estudianteRepository.save(estudiante);
                
                // Crear membresía
                Membresia membresia = new Membresia();
                membresia.setEstudiante(estudianteGuardado);
                membresia.setEquipo(null);
                membresia.setFechaInicio(LocalDate.now());
                membresia.setFechaFin(LocalDate.now().plusMonths(1));
                membresia.setValorMensual(new java.math.BigDecimal("50000"));
                membresia.setEstado(false);
                membresiaRepository.save(membresia);
                
                // Crear usuario
                RegisterRequest registerRequest = new RegisterRequest();
                registerRequest.setNombre(estudianteGuardado.getCorreoEstudiante());
                registerRequest.setEmail(estudianteGuardado.getCorreoEstudiante());
                registerRequest.setPassword(estudianteGuardado.getNumeroDocumento());
                registerRequest.setFotoUrl(null);
                registerRequest.setFotoNombre(null);
                
                authService.registerStudent(registerRequest);
                
                resultados.add(Map.of(
                    "fila", dto.getNumeroFila(),
                    "nombre", dto.getNombreCompleto(),
                    "estado", "EXITOSO",
                    "mensaje", "Estudiante y usuario creados",
                    "idEstudiante", estudianteGuardado.getIdEstudiante(),
                    "email", estudianteGuardado.getCorreoEstudiante(),
                    "password", estudianteGuardado.getNumeroDocumento()
                ));
                exitosos++;
                
            } catch (Exception e) {
                resultados.add(Map.of(
                    "fila", dto.getNumeroFila(),
                    "nombre", dto.getNombreCompleto(),
                    "estado", "ERROR",
                    "mensaje", "Error al procesar: " + e.getMessage()
                ));
                errores++;
            }
        }
        
        reporte.put("exitosos", exitosos);
        reporte.put("errores", errores);
        reporte.put("total", dtos.size());
        reporte.put("resultados", resultados);
        
        return reporte;
    }
    
    /**
     * Valida un DTO de estudiante del Excel
     */
    private String validarDtoEstudiante(ExcelEstudianteImportDTO dto) {
        List<String> errores = new ArrayList<>();
        
        if (dto.getNombreCompleto() == null || dto.getNombreCompleto().trim().isEmpty()) {
            errores.add("Nombre completo requerido");
        }
        if (dto.getNumeroDocumento() == null || dto.getNumeroDocumento().trim().isEmpty()) {
            errores.add("Número de documento requerido");
        }
        if (dto.getCorreoEstudiante() == null || dto.getCorreoEstudiante().trim().isEmpty()) {
            errores.add("Correo electrónico requerido");
        }
        if (dto.getFechaNacimiento() == null) {
            errores.add("Fecha de nacimiento requerida");
        }
        if (dto.getTipoDocumento() == null || dto.getTipoDocumento().trim().isEmpty()) {
            errores.add("Tipo de documento requerido");
        }
        
        return String.join(", ", errores);
    }
    
    /**
     * Convierte un ExcelEstudianteImportDTO a una entidad Estudiante
     */
    private Estudiante dtoAEstudiante(ExcelEstudianteImportDTO dto, galacticos_app_back.galacticos.entity.Sede sede) {
        Estudiante estudiante = new Estudiante();
        
        // Información personal
        estudiante.setNombreCompleto(dto.getNombreCompleto());
        try {
            estudiante.setTipoDocumento(Estudiante.TipoDocumento.valueOf(dto.getTipoDocumento().toUpperCase()));
        } catch (Exception e) {
            estudiante.setTipoDocumento(Estudiante.TipoDocumento.CC);
        }
        estudiante.setNumeroDocumento(dto.getNumeroDocumento());
        estudiante.setFechaNacimiento(dto.getFechaNacimiento());
        estudiante.setEdad(dto.getEdad());
        
        // Sexo
        try {
            if (dto.getSexo() != null && !dto.getSexo().isEmpty()) {
                estudiante.setSexo(Estudiante.Sexo.valueOf(dto.getSexo().toUpperCase()));
            }
        } catch (Exception e) {
            // Ignorar si no es un valor válido
        }
        
        // Información de contacto
        estudiante.setDireccionResidencia(dto.getDireccionResidencia());
        estudiante.setBarrio(dto.getBarrio());
        estudiante.setCelularEstudiante(dto.getCelularEstudiante());
        estudiante.setWhatsappEstudiante(dto.getWhatsappEstudiante());
        estudiante.setCorreoEstudiante(dto.getCorreoEstudiante());
        
        // Sede
        estudiante.setSede(sede);
        
        // Información del tutor
        estudiante.setNombreTutor(dto.getNombreTutor());
        estudiante.setParentescoTutor(dto.getParentescoTutor());
        estudiante.setDocumentoTutor(dto.getDocumentoTutor());
        estudiante.setTelefonoTutor(dto.getTelefonoTutor());
        estudiante.setCorreoTutor(dto.getCorreoTutor());
        estudiante.setOcupacionTutor(dto.getOcupacionTutor());
        
        // Información académica
        estudiante.setInstitucionEducativa(dto.getInstitucionEducativa());
        try {
            if (dto.getJornada() != null && !dto.getJornada().isEmpty()) {
                estudiante.setJornada(Estudiante.Jornada.valueOf(dto.getJornada().toUpperCase()));
            }
        } catch (Exception e) {
            // Ignorar si no es un valor válido
        }
        estudiante.setGradoActual(dto.getGradoActual());
        
        // Información médica
        estudiante.setEps(dto.getEps());
        estudiante.setTipoSangre(dto.getTipoSangre());
        estudiante.setAlergias(dto.getAlergias());
        estudiante.setEnfermedadesCondiciones(dto.getEnfermedadesCondiciones());
        estudiante.setMedicamentos(dto.getMedicamentos());
        estudiante.setCertificadoMedicoDeportivo(dto.getCertificadoMedicoDeportivo() != null ? dto.getCertificadoMedicoDeportivo() : false);
        
        // Día de pago
        estudiante.setDiaPagoMes(dto.getDiaPagoMes());
        
        // Información de emergencia
        estudiante.setNombreEmergencia(dto.getNombreEmergencia());
        estudiante.setTelefonoEmergencia(dto.getTelefonoEmergencia());
        estudiante.setParentescoEmergencia(dto.getParentescoEmergencia());
        estudiante.setOcupacionEmergencia(dto.getOcupacionEmergencia());
        estudiante.setCorreoEmergencia(dto.getCorreoEmergencia());
        
        // Poblaciones vulnerables
        estudiante.setPertenecelgbtiq(dto.getPerteneceIgbtiq() != null ? dto.getPerteneceIgbtiq() : false);
        estudiante.setPersonaDiscapacidad(dto.getPersonaDiscapacidad() != null ? dto.getPersonaDiscapacidad() : false);
        estudiante.setCondicionDiscapacidad(dto.getCondicionDiscapacidad());
        estudiante.setMigranteRefugiado(dto.getMigranteRefugiado() != null ? dto.getMigranteRefugiado() : false);
        estudiante.setPoblacionEtnica(dto.getPoblacionEtnica());
        estudiante.setReligion(dto.getReligion());
        
        // Información deportiva
        estudiante.setExperienciaVoleibol(dto.getExperienciaVoleibol());
        estudiante.setOtrasDisciplinas(dto.getOtrasDisciplinas());
        estudiante.setPosicionPreferida(dto.getPosicionPreferida());
        try {
            if (dto.getDominancia() != null && !dto.getDominancia().isEmpty()) {
                estudiante.setDominancia(Estudiante.Dominancia.valueOf(dto.getDominancia().toUpperCase()));
            }
        } catch (Exception e) {
            // Ignorar si no es un valor válido
        }
        try {
            if (dto.getNivelActual() != null && !dto.getNivelActual().isEmpty()) {
                estudiante.setNivelActual(Estudiante.NivelActual.valueOf(dto.getNivelActual().toUpperCase()));
            }
        } catch (Exception e) {
            // Ignorar si no es un valor válido
        }
        estudiante.setClubesAnteriores(dto.getClubesAnteriores());
        
        // Consentimiento
        estudiante.setAceptaConsentimiento(dto.getConsentimientoInformado() != null ? dto.getConsentimientoInformado() : false);
        estudiante.setFirmaDigital(dto.getFirmaDigital());
        estudiante.setFechaDiligenciamiento(dto.getFechaDiligenciamiento());
        
        // Estado por defecto
        estudiante.setEstado(true);
        
        return estudiante;
    }
    
    /**
     * Importa estudiantes desde un archivo Excel
     */
    @Transactional
    public Map<String, Object> procesarImportacionExcel(java.io.InputStream inputStream, Integer sedeId) throws Exception {
        List<ExcelEstudianteImportDTO> dtos = excelImportService.leerExcel(inputStream);
        return importarEstudiantesDesdeExcel(dtos, sedeId);
    }
    
    // ===================== MÉTODO ACTUALIZAR - CORREGIDO =====================
    /**
     * Actualiza TODOS los datos de un estudiante
     * Este es el método usado por el PUT /api/estudiantes/{id}
     * 
     * IMPORTANTE: Si cambia el correo o documento, también actualiza las credenciales del usuario asociado
     * - Credenciales generadas por: username = correoEstudiante, password = numeroDocumento
     */
    @Transactional
    public Estudiante actualizar(Integer id, Estudiante estudianteActualizado) {
        System.out.println("=== INICIANDO ACTUALIZACIÓN DE ESTUDIANTE ID: " + id + " ===");
        
        // Obtener el estudiante existente
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado con ID: " + id));
        
        // Guardar valores antiguos para detectar cambios en credenciales
        String emailAntiguo = estudiante.getCorreoEstudiante();
        String documentoAntiguo = estudiante.getNumeroDocumento();
        
        System.out.println("Nombre anterior: " + estudiante.getNombreCompleto());
        System.out.println("Nombre nuevo: " + estudianteActualizado.getNombreCompleto());
        System.out.println("Documento anterior: " + documentoAntiguo);
        System.out.println("Documento nuevo: " + estudianteActualizado.getNumeroDocumento());
        System.out.println("Email anterior: " + emailAntiguo);
        System.out.println("Email nuevo: " + estudianteActualizado.getCorreoEstudiante());
        
        // ============ ACTUALIZAR TODOS LOS CAMPOS ============
        
        // Información personal
        estudiante.setNombreCompleto(estudianteActualizado.getNombreCompleto());
        estudiante.setTipoDocumento(estudianteActualizado.getTipoDocumento());
        estudiante.setNumeroDocumento(estudianteActualizado.getNumeroDocumento());
        estudiante.setFechaNacimiento(estudianteActualizado.getFechaNacimiento());
        estudiante.setEdad(estudianteActualizado.getEdad());
        estudiante.setSexo(estudianteActualizado.getSexo());
        
        // Información de contacto
        estudiante.setDireccionResidencia(estudianteActualizado.getDireccionResidencia());
        estudiante.setBarrio(estudianteActualizado.getBarrio());
        estudiante.setCelularEstudiante(estudianteActualizado.getCelularEstudiante());
        estudiante.setWhatsappEstudiante(estudianteActualizado.getWhatsappEstudiante());
        estudiante.setCorreoEstudiante(estudianteActualizado.getCorreoEstudiante());
        
        // ============ SEDE - CAMPO CRÍTICO ============
        if (estudianteActualizado.getSede() != null) {
            System.out.println("Sede anterior: " + (estudiante.getSede() != null ? estudiante.getSede().getNombre() : "null"));
            System.out.println("Sede nueva: " + estudianteActualizado.getSede().getNombre() + " (ID: " + estudianteActualizado.getSede().getIdSede() + ")");
            estudiante.setSede(estudianteActualizado.getSede());
        }
        
        // Información del tutor
        estudiante.setNombreTutor(estudianteActualizado.getNombreTutor());
        estudiante.setParentescoTutor(estudianteActualizado.getParentescoTutor());
        estudiante.setDocumentoTutor(estudianteActualizado.getDocumentoTutor());
        estudiante.setTelefonoTutor(estudianteActualizado.getTelefonoTutor());
        estudiante.setCorreoTutor(estudianteActualizado.getCorreoTutor());
        estudiante.setOcupacionTutor(estudianteActualizado.getOcupacionTutor());
        
        // Información académica
        estudiante.setInstitucionEducativa(estudianteActualizado.getInstitucionEducativa());
        estudiante.setJornada(estudianteActualizado.getJornada());
        estudiante.setGradoActual(estudianteActualizado.getGradoActual());
        
        // Información médica
        estudiante.setEps(estudianteActualizado.getEps());
        estudiante.setTipoSangre(estudianteActualizado.getTipoSangre());
        estudiante.setAlergias(estudianteActualizado.getAlergias());
        estudiante.setEnfermedadesCondiciones(estudianteActualizado.getEnfermedadesCondiciones());
        estudiante.setMedicamentos(estudianteActualizado.getMedicamentos());
        estudiante.setCertificadoMedicoDeportivo(estudianteActualizado.getCertificadoMedicoDeportivo());
        
        // Día de pago
        estudiante.setDiaPagoMes(estudianteActualizado.getDiaPagoMes());
        
        // Información de emergencia
        estudiante.setNombreEmergencia(estudianteActualizado.getNombreEmergencia());
        estudiante.setTelefonoEmergencia(estudianteActualizado.getTelefonoEmergencia());
        estudiante.setParentescoEmergencia(estudianteActualizado.getParentescoEmergencia());
        estudiante.setOcupacionEmergencia(estudianteActualizado.getOcupacionEmergencia());
        estudiante.setCorreoEmergencia(estudianteActualizado.getCorreoEmergencia());
        
        // Poblaciones vulnerables
        estudiante.setPertenecelgbtiq(estudianteActualizado.getPertenecelgbtiq());
        estudiante.setPersonaDiscapacidad(estudianteActualizado.getPersonaDiscapacidad());
        estudiante.setCondicionDiscapacidad(estudianteActualizado.getCondicionDiscapacidad());
        estudiante.setMigranteRefugiado(estudianteActualizado.getMigranteRefugiado());
        estudiante.setPoblacionEtnica(estudianteActualizado.getPoblacionEtnica());
        estudiante.setReligion(estudianteActualizado.getReligion());
        
        // Información deportiva
        estudiante.setExperienciaVoleibol(estudianteActualizado.getExperienciaVoleibol());
        estudiante.setOtrasDisciplinas(estudianteActualizado.getOtrasDisciplinas());
        estudiante.setPosicionPreferida(estudianteActualizado.getPosicionPreferida());
        estudiante.setDominancia(estudianteActualizado.getDominancia());
        estudiante.setNivelActual(estudianteActualizado.getNivelActual());
        estudiante.setClubesAnteriores(estudianteActualizado.getClubesAnteriores());
        
        // Camiseta
        estudiante.setNombreCamiseta(estudianteActualizado.getNombreCamiseta());
        estudiante.setNumeroCamiseta(estudianteActualizado.getNumeroCamiseta());
        
        // Consentimiento y firma
        estudiante.setAceptaConsentimiento(estudianteActualizado.getAceptaConsentimiento());
        estudiante.setFirmaDigital(estudianteActualizado.getFirmaDigital());
        estudiante.setFechaDiligenciamiento(estudianteActualizado.getFechaDiligenciamiento());
        
        // Fotos
        estudiante.setFotoUrl(estudianteActualizado.getFotoUrl());
        estudiante.setFotoNombre(estudianteActualizado.getFotoNombre());
        
        // Estado y estado de pago
        estudiante.setEstado(estudianteActualizado.getEstado());
        if (estudianteActualizado.getEstadoPago() != null) {
            estudiante.setEstadoPago(estudianteActualizado.getEstadoPago());
        }
        
        // ============ ACTUALIZAR USUARIO ASOCIADO SI CAMBIARON CREDENCIALES ============
        // Las credenciales son: username = correoEstudiante, password = numeroDocumento
        boolean cambioEmail = !emailAntiguo.equals(estudianteActualizado.getCorreoEstudiante());
        boolean cambioDocumento = !documentoAntiguo.equals(estudianteActualizado.getNumeroDocumento());
        
        if (cambioEmail || cambioDocumento) {
            System.out.println("\n⚠️  CAMBIO DETECTADO EN CREDENCIALES");
            System.out.println("   Cambio email: " + cambioEmail);
            System.out.println("   Cambio documento: " + cambioDocumento);
            
            // Buscar el usuario por email antiguo (ya que el email es el username)
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(emailAntiguo);
            
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                System.out.println("\n✅ Usuario encontrado - ID: " + usuario.getIdUsuario());
                
                // Actualizar email si cambió
                if (cambioEmail) {
                    // Verificar que el nuevo email no esté en uso por otro usuario
                    if (!estudianteActualizado.getCorreoEstudiante().equals(emailAntiguo) &&
                        usuarioRepository.findByEmail(estudianteActualizado.getCorreoEstudiante()).isPresent()) {
                        throw new RuntimeException("El correo " + estudianteActualizado.getCorreoEstudiante() + 
                                                 " ya está registrado por otro usuario");
                    }
                    
                    usuario.setEmail(estudianteActualizado.getCorreoEstudiante());
                    usuario.setUsername(estudianteActualizado.getCorreoEstudiante());
                    System.out.println("   ✓ Email actualizado: " + emailAntiguo + " → " + 
                                     estudianteActualizado.getCorreoEstudiante());
                }
                
                // Actualizar contraseña si cambió el documento
                if (cambioDocumento) {
                    String newPassword = passwordEncoder.encode(estudianteActualizado.getNumeroDocumento());
                    usuario.setPassword(newPassword);
                    usuario.setRequiereChangioPassword(true); // Marcar para cambio en próximo login
                    System.out.println("   ✓ Contraseña actualizada (basada en nuevo documento)");
                    System.out.println("   ✓ Usuario deberá cambiar contraseña en próximo login");
                }
                
                // Actualizar nombre si cambió
                usuario.setNombre(estudianteActualizado.getNombreCompleto());
                usuario.setTipoDocumento(estudianteActualizado.getTipoDocumento() != null ? 
                        estudianteActualizado.getTipoDocumento().name() : null);
                usuario.setNumeroDocumento(estudianteActualizado.getNumeroDocumento());
                
                // Guardar cambios del usuario
                usuarioRepository.save(usuario);
                System.out.println("   ✓ Usuario actualizado en BD");
            } else {
                System.out.println("⚠️  ADVERTENCIA: No se encontró usuario con email: " + emailAntiguo);
            }
        }
        
        // ============ GUARDAR EN LA BD ============
        Estudiante estudianteGuardado = estudianteRepository.save(estudiante);
        
        System.out.println("✅ Estudiante actualizado exitosamente");
        System.out.println("Nuevo nombre: " + estudianteGuardado.getNombreCompleto());
        System.out.println("Nuevo documento: " + estudianteGuardado.getNumeroDocumento());
        System.out.println("Nuevo email: " + estudianteGuardado.getCorreoEstudiante());
        System.out.println("Nueva sede: " + (estudianteGuardado.getSede() != null ? estudianteGuardado.getSede().getNombre() : "Sin sede"));
        
        if (cambioEmail || cambioDocumento) {
            System.out.println("\n✅ Usuario asociado también fue sincronizado");
            if (cambioEmail) {
                System.out.println("   Email usuario: " + estudianteGuardado.getCorreoEstudiante());
            }
            if (cambioDocumento) {
                System.out.println("   Contraseña: Actualizada (basada en nuevo documento)");
                System.out.println("   ⚠️  Usuario deberá cambiar contraseña en próximo login");
            }
        }
        
        System.out.println("=== FIN DE ACTUALIZACIÓN ===");
        
        return estudianteGuardado;
    }
    
    // Eliminar estudiante
    public void eliminar(Integer id) {
        estudianteRepository.deleteById(id);
    }
    
    // Desactivar estudiante
    public Estudiante desactivar(Integer id) {
        Optional<Estudiante> estudiante = estudianteRepository.findById(id);
        if (estudiante.isPresent()) {
            estudiante.get().setEstado(false);
            return estudianteRepository.save(estudiante.get());
        }
        return null;
    }
    
    // ================== NUEVOS MÉTODOS PARA ACTUALIZACIÓN DE ESTUDIANTES ==================
    
    /**
     * Cambia la contraseña del estudiante
     * @param id ID del estudiante
     * @param cambioPasswordDTO DTO con las contraseñas
     * @return true si el cambio fue exitoso
     * @throws RuntimeException si hay algún error de validación
     */
    @Transactional
    public boolean cambiarPassword(Integer id, CambioPasswordEstudianteDTO cambioPasswordDTO) {
        // Buscar estudiante
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));
        
        // Buscar usuario asociado al estudiante por su correo
        Usuario usuario = usuarioRepository.findByEmail(estudiante.getCorreoEstudiante())
                .orElseThrow(() -> new RuntimeException("Usuario asociado al estudiante no encontrado"));
        
        // Verificar que la contraseña actual sea correcta
        if (!passwordEncoder.matches(cambioPasswordDTO.getPasswordActual(), usuario.getPassword())) {
            throw new RuntimeException("La contraseña actual es incorrecta");
        }
        
        // Verificar que las contraseñas nuevas coincidan
        if (!cambioPasswordDTO.getPasswordNueva().equals(cambioPasswordDTO.getConfirmarPassword())) {
            throw new RuntimeException("Las contraseñas no coinciden");
        }
        
        // Verificar longitud mínima
        if (cambioPasswordDTO.getPasswordNueva().length() < 8) {
            throw new RuntimeException("La nueva contraseña debe tener al menos 8 caracteres");
        }
        
        // Encriptar y guardar la nueva contraseña
        usuario.setPassword(passwordEncoder.encode(cambioPasswordDTO.getPasswordNueva()));
        usuarioRepository.save(usuario);
        
        return true;
    }
    
    /**
     * Sube y actualiza la foto del estudiante
     * @param id ID del estudiante
     * @param foto Archivo de imagen
     * @return FileStorageService.FileInfo con información del archivo
     * @throws RuntimeException si hay algún error
     */
    @Transactional
    public FileStorageService.FileInfo actualizarFoto(Integer id, MultipartFile foto) {
        // Buscar estudiante
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));
        
        // Validar que sea una imagen válida
        if (!fileStorageService.isValidImage(foto)) {
            throw new RuntimeException("Archivo no válido. Solo se permiten imágenes JPG, PNG o WEBP");
        }
        
        // Validar tamaño (5MB = 5 * 1024 * 1024 bytes)
        if (foto.getSize() > 5 * 1024 * 1024) {
            throw new RuntimeException("El archivo excede el tamaño máximo de 5MB");
        }
        
        // Eliminar foto anterior si existe
        if (estudiante.getFotoNombre() != null && !estudiante.getFotoNombre().isEmpty()) {
            try {
                fileStorageService.deleteEstudiantePhoto(estudiante.getFotoNombre());
            } catch (Exception e) {
                System.out.println("No se pudo eliminar la foto anterior: " + e.getMessage());
            }
        }
        
        // Guardar nueva foto
        FileStorageService.FileInfo fileInfo = fileStorageService.storeEstudiantePhoto(foto);
        
        // Actualizar estudiante
        estudiante.setFotoUrl(fileInfo.getFileUrl());
        estudiante.setFotoNombre(fileInfo.getStoredFileName());
        estudianteRepository.save(estudiante);
        
        // También actualizar la foto en el usuario asociado
        usuarioRepository.findByEmail(estudiante.getCorreoEstudiante())
                .ifPresent(usuario -> {
                    usuario.setFotoUrl(fileInfo.getFileUrl());
                    usuario.setFotoNombre(fileInfo.getStoredFileName());
                    usuarioRepository.save(usuario);
                });
        
        return fileInfo;
    }
    
    /**
     * Elimina la foto del estudiante
     * @param id ID del estudiante
     * @throws RuntimeException si el estudiante no existe
     */
    @Transactional
    public void eliminarFoto(Integer id) {
        // Buscar estudiante
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));
        
        // Eliminar archivo si existe
        if (estudiante.getFotoNombre() != null && !estudiante.getFotoNombre().isEmpty()) {
            try {
                fileStorageService.deleteEstudiantePhoto(estudiante.getFotoNombre());
            } catch (Exception e) {
                System.out.println("No se pudo eliminar el archivo: " + e.getMessage());
            }
        }
        
        // Limpiar campos de foto
        estudiante.setFotoUrl(null);
        estudiante.setFotoNombre(null);
        estudianteRepository.save(estudiante);
        
        // También limpiar foto en el usuario asociado
        usuarioRepository.findByEmail(estudiante.getCorreoEstudiante())
                .ifPresent(usuario -> {
                    usuario.setFotoUrl(null);
                    usuario.setFotoNombre(null);
                    usuarioRepository.save(usuario);
                });
    }
    
    /**
     * Actualiza parcialmente los datos de un estudiante
     * @param id ID del estudiante
     * @param dto DTO con los campos a actualizar
     * @return Estudiante actualizado
     * @throws RuntimeException si el estudiante no existe
     */
    @Transactional
    public Estudiante actualizarParcial(Integer id, ActualizarEstudianteDTO dto) {
        // Buscar estudiante
        Estudiante estudiante = estudianteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado"));
        
        // Actualizar solo los campos que no son null
        if (dto.getNombreCompleto() != null) {
            estudiante.setNombreCompleto(dto.getNombreCompleto());
        }
        if (dto.getCelularEstudiante() != null) {
            estudiante.setCelularEstudiante(dto.getCelularEstudiante());
        }
        if (dto.getWhatsappEstudiante() != null) {
            estudiante.setWhatsappEstudiante(dto.getWhatsappEstudiante());
        }
        if (dto.getCorreoEstudiante() != null) {
            // Verificar que el nuevo correo no esté en uso
            if (!dto.getCorreoEstudiante().equals(estudiante.getCorreoEstudiante())) {
                if (usuarioRepository.findByEmail(dto.getCorreoEstudiante()).isPresent()) {
                    throw new RuntimeException("El correo ya está registrado por otro usuario");
                }
                // Actualizar también el email del usuario asociado
                usuarioRepository.findByEmail(estudiante.getCorreoEstudiante())
                        .ifPresent(usuario -> {
                            usuario.setEmail(dto.getCorreoEstudiante());
                            usuarioRepository.save(usuario);
                        });
            }
            estudiante.setCorreoEstudiante(dto.getCorreoEstudiante());
        }
        if (dto.getDireccionResidencia() != null) {
            estudiante.setDireccionResidencia(dto.getDireccionResidencia());
        }
        if (dto.getBarrio() != null) {
            estudiante.setBarrio(dto.getBarrio());
        }
        if (dto.getEps() != null) {
            estudiante.setEps(dto.getEps());
        }
        if (dto.getTipoSangre() != null) {
            estudiante.setTipoSangre(dto.getTipoSangre());
        }
        if (dto.getAlergias() != null) {
            estudiante.setAlergias(dto.getAlergias());
        }
        if (dto.getEnfermedadesCondiciones() != null) {
            estudiante.setEnfermedadesCondiciones(dto.getEnfermedadesCondiciones());
        }
        if (dto.getMedicamentos() != null) {
            estudiante.setMedicamentos(dto.getMedicamentos());
        }
        if (dto.getCertificadoMedicoDeportivo() != null) {
            estudiante.setCertificadoMedicoDeportivo(dto.getCertificadoMedicoDeportivo());
        }
        
        // Datos del tutor
        if (dto.getNombreTutor() != null) {
            estudiante.setNombreTutor(dto.getNombreTutor());
        }
        if (dto.getParentescoTutor() != null) {
            estudiante.setParentescoTutor(dto.getParentescoTutor());
        }
        if (dto.getDocumentoTutor() != null) {
            estudiante.setDocumentoTutor(dto.getDocumentoTutor());
        }
        if (dto.getTelefonoTutor() != null) {
            estudiante.setTelefonoTutor(dto.getTelefonoTutor());
        }
        if (dto.getCorreoTutor() != null) {
            estudiante.setCorreoTutor(dto.getCorreoTutor());
        }
        if (dto.getOcupacionTutor() != null) {
            estudiante.setOcupacionTutor(dto.getOcupacionTutor());
        }
        
        // Contacto de emergencia
        if (dto.getNombreEmergencia() != null) {
            estudiante.setNombreEmergencia(dto.getNombreEmergencia());
        }
        if (dto.getTelefonoEmergencia() != null) {
            estudiante.setTelefonoEmergencia(dto.getTelefonoEmergencia());
        }
        if (dto.getParentescoEmergencia() != null) {
            estudiante.setParentescoEmergencia(dto.getParentescoEmergencia());
        }
        
        // Datos de camiseta
        if (dto.getNombreCamiseta() != null) {
            estudiante.setNombreCamiseta(dto.getNombreCamiseta());
        }
        if (dto.getNumeroCamiseta() != null) {
            estudiante.setNumeroCamiseta(dto.getNumeroCamiseta());
        }
        
        return estudianteRepository.save(estudiante);
    }
    
    // ================== MÉTODOS PARA GESTIÓN DE ESTADO DE PAGO ==================
    
    /**
     * Obtiene todos los estudiantes con su estado de pago
     * @return Lista de estudiantes con información de estado de pago
     */
    public List<EstudianteConEstadoPagoDTO> obtenerTodosConEstadoPago() {
        List<Estudiante> estudiantes = estudianteRepository.findAll();
        return estudiantes.stream()
                .map(this::convertirAEstudianteConEstadoPago)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene estudiantes activos con su estado de pago
     * @return Lista de estudiantes activos con información de estado de pago
     */
    public List<EstudianteConEstadoPagoDTO> obtenerActivosConEstadoPago() {
        List<Estudiante> estudiantes = estudianteRepository.findByEstado(true);
        return estudiantes.stream()
                .map(this::convertirAEstudianteConEstadoPago)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene estudiantes por estado de pago
     * @param estadoPago Estado de pago a filtrar
     * @return Lista de estudiantes con el estado de pago especificado
     */
    public List<EstudianteConEstadoPagoDTO> obtenerPorEstadoPago(Estudiante.EstadoPago estadoPago) {
        List<Estudiante> estudiantes = estudianteRepository.findByEstadoPago(estadoPago);
        return estudiantes.stream()
                .map(this::convertirAEstudianteConEstadoPago)
                .collect(Collectors.toList());
    }
    
    /**
     * Obtiene estudiantes de una sede con su estado de pago
     * @param idSede ID de la sede
     * @return Lista de estudiantes de la sede con información de estado de pago
     */
    public List<EstudianteConEstadoPagoDTO> obtenerPorSedeConEstadoPago(Integer idSede) {
        List<Estudiante> estudiantes = estudianteRepository.findBySedeIdSede(idSede);
        return estudiantes.stream()
                .map(this::convertirAEstudianteConEstadoPago)
                .collect(Collectors.toList());
    }
    
    /**
     * Cambia el estado de pago de un estudiante manualmente
     * Usado para pagos en efectivo o ajustes manuales
     * @param idEstudiante ID del estudiante
     * @param cambioDTO DTO con el nuevo estado y observación
     * @return Estudiante actualizado
     */
    @Transactional
    public Estudiante cambiarEstadoPago(Integer idEstudiante, CambioEstadoPagoDTO cambioDTO) {
        Estudiante estudiante = estudianteRepository.findById(idEstudiante)
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado con ID: " + idEstudiante));
        
        Estudiante.EstadoPago estadoAnterior = estudiante.getEstadoPago();
        estudiante.setEstadoPago(cambioDTO.getNuevoEstado());
        
        Estudiante estudianteActualizado = estudianteRepository.save(estudiante);
        
        System.out.println(String.format("Estado de pago cambiado - Estudiante: %s, De: %s, A: %s, Observación: %s",
                estudiante.getNombreCompleto(), estadoAnterior, cambioDTO.getNuevoEstado(), cambioDTO.getObservacion()));
        
        return estudianteActualizado;
    }
    
    /**
     * Registra un pago en efectivo y actualiza el estado del estudiante
     * @param pagoDTO DTO con la información del pago
     * @return Pago registrado
     */
    @Transactional
    public Pago registrarPagoEfectivo(RegistroPagoEfectivoDTO pagoDTO) {
        Estudiante estudiante = estudianteRepository.findById(pagoDTO.getIdEstudiante())
                .orElseThrow(() -> new RuntimeException("Estudiante no encontrado con ID: " + pagoDTO.getIdEstudiante()));
        
        // Crear el registro de pago
        Pago pago = new Pago();
        pago.setEstudiante(estudiante);
        pago.setMesPagado(pagoDTO.getMesPagado());
        pago.setValor(pagoDTO.getValor());
        pago.setMetodoPago(Pago.MetodoPago.EFECTIVO);
        pago.setReferenciaPago(pagoDTO.getReferenciaPago() != null ? pagoDTO.getReferenciaPago() : 
                "EFECT-" + pagoDTO.getIdEstudiante() + "-" + System.currentTimeMillis());
        pago.setFechaPago(LocalDate.now());
        pago.setHoraPago(LocalTime.now());
        pago.setEstadoPago(Pago.EstadoPago.PAGADO);
        
        Pago pagoGuardado = pagoRepository.save(pago);
        
        // Actualizar estado del estudiante a AL_DIA
        estudiante.setEstadoPago(Estudiante.EstadoPago.AL_DIA);
        estudianteRepository.save(estudiante);
        
        // Actualizar membresía si existe
        actualizarMembresiaAlPagar(estudiante.getIdEstudiante());
        
        System.out.println(String.format("Pago en efectivo registrado - Estudiante: %s, Mes: %s, Valor: %s",
                estudiante.getNombreCompleto(), pagoDTO.getMesPagado(), pagoDTO.getValor()));
        
        return pagoGuardado;
    }
    
    /**
     * Actualiza el estado de pago de un estudiante cuando se procesa un pago online (Wompi)
     * @param idEstudiante ID del estudiante
     * @param pagado true si el pago fue aprobado, false si fue rechazado
     */
    @Transactional
    public void actualizarEstadoPorPagoOnline(Integer idEstudiante, boolean pagado) {
        Optional<Estudiante> estudianteOpt = estudianteRepository.findById(idEstudiante);
        
        if (estudianteOpt.isPresent()) {
            Estudiante estudiante = estudianteOpt.get();
            
            if (pagado) {
                estudiante.setEstadoPago(Estudiante.EstadoPago.AL_DIA);
                actualizarMembresiaAlPagar(idEstudiante);
                System.out.println("Estado de pago actualizado a AL_DIA - Estudiante: " + estudiante.getNombreCompleto());
            }
            
            estudianteRepository.save(estudiante);
        }
    }
    
    /**
     * Actualiza la membresía del estudiante cuando realiza un pago
     */
    private void actualizarMembresiaAlPagar(Integer idEstudiante) {
        List<Membresia> membresias = membresiaRepository.findByEstudianteIdEstudiante(idEstudiante);
        
        if (!membresias.isEmpty()) {
            Membresia membresia = membresias.get(0); // Tomar la membresía más reciente
            membresia.setEstado(true); // Activar membresía
            membresia.setFechaFin(LocalDate.now().plusMonths(1)); // Extender un mes
            membresiaRepository.save(membresia);
        }
    }
    
    /**
     * Verifica y actualiza los estados de pago de todos los estudiantes
     * Este método puede ser llamado por un scheduler para verificar moras
     */
    @Transactional
    public void verificarEstadosPago() {
        String mesActual = obtenerMesActual();
        List<Estudiante> estudiantes = estudianteRepository.findByEstado(true);
        
        for (Estudiante estudiante : estudiantes) {
            // Si el estudiante no está AL_DIA y ya pasó el día de pago del mes
            if (estudiante.getEstadoPago() != Estudiante.EstadoPago.AL_DIA 
                    && estudiante.getEstadoPago() != Estudiante.EstadoPago.COMPROMISO_PAGO) {
                
                // Verificar si tiene pago del mes actual
                Optional<Pago> pagoMes = pagoRepository.findPagoAprobadoByEstudianteAndMes(
                        estudiante.getIdEstudiante(), mesActual);
                
                if (pagoMes.isEmpty()) {
                    // No tiene pago del mes, verificar si debería estar en mora
                    LocalDate hoy = LocalDate.now();
                    Integer diaPago = estudiante.getDiaPagoMes();
                    
                    if (diaPago != null && hoy.getDayOfMonth() > diaPago) {
                        // Ya pasó la fecha de pago, poner en mora
                        estudiante.setEstadoPago(Estudiante.EstadoPago.EN_MORA);
                        estudianteRepository.save(estudiante);
                        System.out.println("Estudiante en mora: " + estudiante.getNombreCompleto());
                    }
                }
            }
        }
    }
    
    /**
     * Obtiene el mes actual en formato para comparación
     */
    private String obtenerMesActual() {
        LocalDate hoy = LocalDate.now();
        String mes = hoy.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "ES")).toUpperCase();
        return mes + "_" + hoy.getYear();
    }
    
    /**
     * Convierte un Estudiante a EstudianteConEstadoPagoDTO con información del último pago
     */
    private EstudianteConEstadoPagoDTO convertirAEstudianteConEstadoPago(Estudiante estudiante) {
        EstudianteConEstadoPagoDTO dto = EstudianteConEstadoPagoDTO.fromEntity(estudiante);
        
        // Agregar información del último pago
        List<Pago> ultimosPagos = pagoRepository.findUltimoPagoByEstudiante(estudiante.getIdEstudiante());
        if (!ultimosPagos.isEmpty()) {
            Pago ultimoPago = ultimosPagos.get(0);
            dto.setFechaUltimoPago(ultimoPago.getFechaPago());
            dto.setMesUltimoPago(ultimoPago.getMesPagado());
        }
        
        return dto;
    }
    
    /**
     * Obtiene los estudiantes que están en mora
     */
    public List<EstudianteConEstadoPagoDTO> obtenerEstudiantesEnMora() {
        return obtenerPorEstadoPago(Estudiante.EstadoPago.EN_MORA);
    }
    
    /**
     * Obtiene los estudiantes pendientes por pagar
     */
    public List<EstudianteConEstadoPagoDTO> obtenerEstudiantesPendientes() {
        return obtenerPorEstadoPago(Estudiante.EstadoPago.PENDIENTE);
    }
    
    /**
     * Obtiene los estudiantes al día
     */
    public List<EstudianteConEstadoPagoDTO> obtenerEstudiantesAlDia() {
        return obtenerPorEstadoPago(Estudiante.EstadoPago.AL_DIA);
    }
    
    /**
     * ESPECIFICACIÓN: Procesa la importación de estudiantes desde Excel con generación de credenciales
     * 
     * Genera automáticamente:
     * - Username: {nombre.apellido}.{estudianteId}
     * - Password: 12 caracteres (mayúsculas, minúsculas, números, símbolos)
     * 
     * @param inputStream Stream del archivo Excel
     * @param sedeId ID de la sede
     * @param nombreArchivo Nombre del archivo importado
     * @param tamanioArchivo Tamaño del archivo en bytes
     * @return ExcelImportResponseDTO con timestamp ISO 8601 y lista de resultados
     * @throws Exception si hay error al leer el Excel
     */
/**
 * MÉTODO CORREGIDO: Procesa la importación de estudiantes desde Excel
 * 
 * Cambios principales:
 * 1. Agregados logs detallados en cada paso
 * 2. Flush explícito después de cada save para forzar persistencia inmediata
 * 3. Manejo robusto de excepciones
 * 4. Validación previa del rol ESTUDIANTE
 * 5. Verificación de persistencia
 */
@Transactional
public ExcelImportResponseDTO procesarImportacionExcelConUsuarios(
        java.io.InputStream inputStream,
        Integer sedeId,
        String nombreArchivo,
        Long tamanioArchivo) {
    
    List<ExcelImportResultado> resultados = new ArrayList<>();
    int exitosos = 0;
    int errores = 0;
    
    try {
        System.out.println("\n========== INICIANDO IMPORTACIÓN DESDE EXCEL ==========");
        System.out.println("Archivo: " + nombreArchivo);
        System.out.println("Tamaño: " + tamanioArchivo + " bytes");
        System.out.println("Sede ID: " + sedeId);
        
        // 1. Validar que la sede existe
        galacticos_app_back.galacticos.entity.Sede sede = sedeRepository.findById(sedeId)
                .orElseThrow(() -> new RuntimeException("Sede no encontrada con ID: " + sedeId));
        System.out.println("✅ Sede validada: " + sede.getNombre());
        
        // 2. Validar que el rol STUDENT existe
        galacticos_app_back.galacticos.entity.Rol rolEstudiante = rolRepository.findByNombre("STUDENT")
                .orElseThrow(() -> new RuntimeException("ERROR CRÍTICO: Rol STUDENT no existe en la base de datos"));
        System.out.println("✅ Rol STUDENT validado: ID=" + rolEstudiante.getIdRol());
        
        // 3. Leer Excel
        System.out.println("\n📖 Leyendo archivo Excel...");
        List<ExcelEstudianteImportDTO> dtos = excelImportService.leerExcel(inputStream);
        int totalFilas = dtos.size();
        System.out.println("✅ " + totalFilas + " filas encontradas en el Excel");
        
        // 4. Procesar cada fila
        for (int i = 0; i < dtos.size(); i++) {
            ExcelEstudianteImportDTO dto = dtos.get(i);
            int numeroFila = i + 2;  // +1 por encabezado, +1 porque es 1-based
            
            System.out.println("\n--- Procesando Fila " + numeroFila + " ---");
            System.out.println("Nombre: " + dto.getNombreCompleto());
            System.out.println("Documento: " + dto.getNumeroDocumento());
            System.out.println("Email: " + dto.getCorreoEstudiante());
            
            try {
                // Validar DTO
                String erroresValidacion = validarDtoEstudiante(dto);
                if (!erroresValidacion.isEmpty()) {
                    System.out.println("❌ Validación fallida: " + erroresValidacion);
                    errores++;
                    resultados.add(ExcelImportResultado.builder()
                            .fila(numeroFila)
                            .nombreEstudiante(dto.getNombreCompleto())
                            .estado("error")
                            .mensaje("Validación fallida")
                            .detalles(erroresValidacion)
                            .build());
                    continue;
                }
                
                // Verificar si el email ya existe
                if (usuarioRepository.findByEmail(dto.getCorreoEstudiante()).isPresent()) {
                    System.out.println("❌ Email ya registrado: " + dto.getCorreoEstudiante());
                    errores++;
                    resultados.add(ExcelImportResultado.builder()
                            .fila(numeroFila)
                            .nombreEstudiante(dto.getNombreCompleto())
                            .estado("error")
                            .mensaje("El correo ya está registrado en el sistema")
                            .detalles(dto.getCorreoEstudiante())
                            .build());
                    continue;
                }
                
                // Verificar si el documento ya existe
                if (estudianteRepository.findByNumeroDocumento(dto.getNumeroDocumento()).isPresent()) {
                    System.out.println("❌ Documento ya registrado: " + dto.getNumeroDocumento());
                    errores++;
                    resultados.add(ExcelImportResultado.builder()
                            .fila(numeroFila)
                            .nombreEstudiante(dto.getNombreCompleto())
                            .estado("error")
                            .mensaje("El número de documento ya está registrado")
                            .detalles(dto.getNumeroDocumento())
                            .build());
                    continue;
                }
                
                // ============ 1. CREAR ESTUDIANTE ============
                System.out.println("\n1️⃣ Creando Estudiante...");
                Estudiante estudiante = dtoAEstudiante(dto, sede);
                estudiante.setEstadoPago(Estudiante.EstadoPago.PENDIENTE);
                Estudiante estudianteGuardado = estudianteRepository.save(estudiante);
                estudianteRepository.flush();  // ⚠️ FORZAR PERSISTENCIA INMEDIATA
                System.out.println("✅ Estudiante guardado - ID: " + estudianteGuardado.getIdEstudiante());
                
                // Verificar que realmente se guardó
                Optional<Estudiante> verificacion1 = estudianteRepository.findById(estudianteGuardado.getIdEstudiante());
                if (!verificacion1.isPresent()) {
                    throw new RuntimeException("ERROR CRÍTICO: Estudiante no se guardó en BD después del flush");
                }
                System.out.println("✅ Verificación: Estudiante existe en BD");
                
                // ============ 2. GENERAR CREDENCIALES ============
                System.out.println("\n2️⃣ Generando credenciales...");
                // Usuario = Email del estudiante
                String username = dto.getCorreoEstudiante();
                // Contraseña = Número de documento del estudiante
                String passwordGenerada = dto.getNumeroDocumento();
                System.out.println("✅ Usuario (email): " + username);
                System.out.println("✅ Contraseña (documento): " + passwordGenerada);
                
                // ============ 3. CREAR USUARIO ============
                System.out.println("\n3️⃣ Creando Usuario...");
                System.out.println("   - Nombre: " + dto.getNombreCompleto());
                System.out.println("   - Username: " + username);
                System.out.println("   - Email: " + dto.getCorreoEstudiante());
                System.out.println("   - Rol: " + (rolEstudiante != null ? rolEstudiante.getNombre() : "NULL"));
                System.out.println("   - Estudiante ID: " + estudianteGuardado.getIdEstudiante());
                
                Usuario usuario = new Usuario();
                usuario.setNombre(dto.getNombreCompleto());
                usuario.setUsername(username);
                usuario.setEmail(dto.getCorreoEstudiante());
                usuario.setPassword(passwordEncoder.encode(passwordGenerada));
                usuario.setRol(rolEstudiante);
                usuario.setEstudiante(estudianteGuardado);
                usuario.setEstado(true);
                usuario.setRequiereChangioPassword(true);
                usuario.setTipoDocumento(dto.getTipoDocumento());
                usuario.setNumeroDocumento(dto.getNumeroDocumento());
                
                System.out.println("   - Intentando guardar usuario...");
                Usuario usuarioGuardado = usuarioRepository.save(usuario);
                System.out.println("   - Usuario guardado en memoria, ID: " + usuarioGuardado.getIdUsuario());
                usuarioRepository.flush();  // ⚠️ FORZAR PERSISTENCIA INMEDIATA
                System.out.println("✅ Usuario guardado - ID: " + usuarioGuardado.getIdUsuario());
                System.out.println("   Email: " + usuarioGuardado.getEmail());
                System.out.println("   Username: " + usuarioGuardado.getUsername());
                System.out.println("   Password (hasheado): " + usuarioGuardado.getPassword());
                System.out.println("   Requiere cambio de contraseña: " + usuarioGuardado.getRequiereChangioPassword());
                
                // Verificar que realmente se guardó
                Optional<Usuario> verificacion2 = usuarioRepository.findById(usuarioGuardado.getIdUsuario());
                if (!verificacion2.isPresent()) {
                    throw new RuntimeException("ERROR CRÍTICO: Usuario no se guardó en BD después del flush");
                }
                System.out.println("✅ Verificación: Usuario existe en BD");
                Usuario usuarioVerificado = verificacion2.get();
                System.out.println("   Password en BD: " + usuarioVerificado.getPassword());
                
                // ============ 4. CREAR MEMBRESÍA ============
                System.out.println("\n4️⃣ Creando Membresía...");
                Membresia membresia = new Membresia();
                membresia.setEstudiante(estudianteGuardado);
                membresia.setEquipo(null);
                membresia.setFechaInicio(LocalDate.now());
                membresia.setFechaFin(LocalDate.now().plusMonths(1));
                membresia.setValorMensual(new BigDecimal("50000"));
                membresia.setEstado(false);
                
                Membresia membresiaGuardada = membresiaRepository.save(membresia);
                membresiaRepository.flush();  // ⚠️ FORZAR PERSISTENCIA INMEDIATA
                System.out.println("✅ Membresía guardada - ID: " + membresiaGuardada.getIdMembresia());
                
                // Verificar que realmente se guardó
                Optional<Membresia> verificacion3 = membresiaRepository.findById(membresiaGuardada.getIdMembresia());
                if (!verificacion3.isPresent()) {
                    throw new RuntimeException("ERROR CRÍTICO: Membresía no se guardó en BD después del flush");
                }
                System.out.println("✅ Verificación: Membresía existe en BD");
                
                // ============ REGISTRO DE ÉXITO ============
                System.out.println("\n✅✅ FILA PROCESADA CON ÉXITO ✅✅");
                exitosos++;
                resultados.add(ExcelImportResultado.builder()
                        .fila(numeroFila)
                        .estudianteId(estudianteGuardado.getIdEstudiante())
                        .nombreEstudiante(dto.getNombreCompleto())
                        .usuarioCreado(username)
                        .passwordGenerada(passwordGenerada)
                        .estado("exitoso")
                        .mensaje("Estudiante y usuario creados correctamente")
                        .build());
                
            } catch (Exception e) {
                System.out.println("\n❌❌ ERROR EN FILA ❌❌");
                System.out.println("Excepción: " + e.getClass().getSimpleName());
                System.out.println("Mensaje: " + e.getMessage());
                e.printStackTrace();
                
                errores++;
                resultados.add(ExcelImportResultado.builder()
                        .fila(numeroFila)
                        .nombreEstudiante(dto.getNombreCompleto() != null ? dto.getNombreCompleto() : "Desconocido")
                        .estado("error")
                        .mensaje("Error al procesar fila: " + e.getMessage())
                        .detalles(e.getClass().getSimpleName())
                        .build());
            }
        }
        
        // ============ REGISTRAR AUDITORÍA ============
        registrarAuditoriaImportacion(sedeId, exitosos, errores, totalFilas, nombreArchivo);
        
        System.out.println("\n========== RESUMEN DE IMPORTACIÓN ==========");
        System.out.println("Total procesadas: " + totalFilas);
        System.out.println("Exitosas: " + exitosos);
        System.out.println("Con errores: " + errores);
        System.out.println("==========================================\n");
        
    } catch (RuntimeException e) {
        System.out.println("\n❌ ERROR DE NEGOCIO: " + e.getMessage());
        e.printStackTrace();
        throw e;
    } catch (Exception e) {
        System.out.println("\n❌ ERROR INESPERADO: " + e.getMessage());
        e.printStackTrace();
        throw new RuntimeException("Error procesando importación: " + e.getMessage(), e);
    }
    
    // Retornar respuesta con timestamp ISO 8601
    return new ExcelImportResponseDTO(exitosos, errores, resultados.size(), resultados);
}

    /**
     * Registra en auditoría la importación de estudiantes
     */
    private void registrarAuditoriaImportacion(
            Integer sedeId,
            int exitosos,
            int errores,
            int total,
            String nombreArchivo) {
        // TODO: Implementar registro en tabla de auditoría si existe
        // Ejemplo: auditTrailRepository.save(new AuditoriaImportacion(...))
        
        // Por ahora solo logueamos
        org.slf4j.LoggerFactory.getLogger(this.getClass()).info(
                "Importación completada - Sede: {}, Exitosos: {}, Errores: {}, Total: {}, Archivo: {}",
                sedeId, exitosos, errores, total, nombreArchivo
        );
    }
}
