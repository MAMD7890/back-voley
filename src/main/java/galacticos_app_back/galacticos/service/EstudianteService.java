package galacticos_app_back.galacticos.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
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
        
        // Registrar usuario con rol STUDENT automático
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
    
    // Actualizar estudiante
    public Estudiante actualizar(Integer id, Estudiante estudiante) {
        Optional<Estudiante> existente = estudianteRepository.findById(id);
        if (existente.isPresent()) {
            estudiante.setIdEstudiante(id);
            return estudianteRepository.save(estudiante);
        }
        return null;
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
}
