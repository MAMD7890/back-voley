package galacticos_app_back.galacticos.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import galacticos_app_back.galacticos.dto.ActualizarEstudianteDTO;
import galacticos_app_back.galacticos.dto.CambioEstadoPagoDTO;
import galacticos_app_back.galacticos.dto.CambioPasswordEstudianteDTO;
import galacticos_app_back.galacticos.dto.EstudianteConEstadoPagoDTO;
import galacticos_app_back.galacticos.dto.FotoResponseDTO;
import galacticos_app_back.galacticos.dto.RegistroPagoEfectivoDTO;
import galacticos_app_back.galacticos.dto.auth.AuthResponse;
import galacticos_app_back.galacticos.entity.Estudiante;
import galacticos_app_back.galacticos.entity.Pago;
import galacticos_app_back.galacticos.service.EstudianteService;
import galacticos_app_back.galacticos.service.FileStorageService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/estudiantes")
@CrossOrigin(origins = "*", maxAge = 3600)
public class EstudianteController {
    
    @Autowired
    private EstudianteService estudianteService;
    
    @GetMapping
    public ResponseEntity<List<Estudiante>> obtenerTodos() {
        return ResponseEntity.ok(estudianteService.obtenerTodos());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<Estudiante> obtenerPorId(@PathVariable Integer id) {
        Optional<Estudiante> estudiante = estudianteService.obtenerPorId(id);
        return estudiante.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    
    @GetMapping("/activos/lista")
    public ResponseEntity<List<Estudiante>> obtenerActivos() {
        return ResponseEntity.ok(estudianteService.obtenerActivos());
    }
    
    @GetMapping("/nivel/{nivel}")
    public ResponseEntity<List<Estudiante>> obtenerPorNivel(@PathVariable Estudiante.NivelActual nivel) {
        return ResponseEntity.ok(estudianteService.obtenerPorNivel(nivel));
    }
    
    @GetMapping("/sede/{idSede}")
    public ResponseEntity<List<Estudiante>> obtenerPorSede(@PathVariable Integer idSede) {
        return ResponseEntity.ok(estudianteService.obtenerPorSede(idSede));
    }
    
    @PostMapping
    public ResponseEntity<Estudiante> crear(@RequestBody Estudiante estudiante) {
        return ResponseEntity.ok(estudianteService.crear(estudiante));
    }
    
    @PostMapping("/registro")
    public ResponseEntity<?> registrarEstudiante(@RequestBody Estudiante estudiante) {
        System.out.println("=== INICIANDO REGISTRO DE ESTUDIANTE ===");
        System.out.println("Datos recibidos: " + estudiante);
        System.out.println("Email: " + estudiante.getCorreoEstudiante());
        System.out.println("Nombre: " + estudiante.getNombreCompleto());
        System.out.println("Documento: " + estudiante.getNumeroDocumento());
        System.out.println("Tipo Documento: " + estudiante.getTipoDocumento());
        System.out.println("Sede: " + estudiante.getSede());
        try {
            // Validar campos requeridos para crear el usuario
            if (estudiante.getCorreoEstudiante() == null || estudiante.getCorreoEstudiante().trim().isEmpty()) {
                System.out.println("ERROR: Correo vacío o nulo");
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "El correo del estudiante es obligatorio"));
            }
            if (estudiante.getNombreCompleto() == null || estudiante.getNombreCompleto().trim().isEmpty()) {
                System.out.println("ERROR: Nombre vacío o nulo");
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "El nombre completo del estudiante es obligatorio"));
            }
            if (estudiante.getNumeroDocumento() == null || estudiante.getNumeroDocumento().trim().isEmpty()) {
                System.out.println("ERROR: Documento vacío o nulo");
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "El número de documento del estudiante es obligatorio"));
            }
            if (estudiante.getTipoDocumento() == null) {
                System.out.println("ERROR: Tipo documento nulo");
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "El tipo de documento es obligatorio. Valores válidos: TI, CC, RC, PASAPORTE"));
            }
            if (estudiante.getFechaNacimiento() == null) {
                System.out.println("ERROR: Fecha nacimiento nula");
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "La fecha de nacimiento es obligatoria"));
            }
            if (estudiante.getEdad() == null) {
                System.out.println("ERROR: Edad nula");
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "La edad es obligatoria"));
            }
            if (estudiante.getSede() == null) {
                System.out.println("ERROR: Sede nula");
                return ResponseEntity.badRequest().body(java.util.Map.of("error", "La sede es obligatoria. Enviar como: {\"sede\": {\"idSede\": 1}}"));
            }
            
            AuthResponse response = estudianteService.crearConUsuario(estudiante);
            System.out.println("=== REGISTRO EXITOSO ===");
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            System.out.println("ERROR en registro: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of("error", e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<Estudiante> actualizar(@PathVariable Integer id, @RequestBody Estudiante estudiante) {
        Estudiante actualizado = estudianteService.actualizar(id, estudiante);
        return actualizado != null ? ResponseEntity.ok(actualizado) : ResponseEntity.notFound().build();
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Integer id) {
        estudianteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
    
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<Estudiante> desactivar(@PathVariable Integer id) {
        Estudiante desactivado = estudianteService.desactivar(id);
        return desactivado != null ? ResponseEntity.ok(desactivado) : ResponseEntity.notFound().build();
    }
    
    // ================== NUEVOS ENDPOINTS PARA ACTUALIZACIÓN DE ESTUDIANTES ==================
    
    /**
     * Actualizar contraseña del estudiante
     * PATCH /api/estudiantes/{id}/password
     */
    @PatchMapping("/{id}/password")
    public ResponseEntity<?> cambiarPassword(
            @PathVariable Integer id,
            @Valid @RequestBody CambioPasswordEstudianteDTO cambioPasswordDTO) {
        try {
            estudianteService.cambiarPassword(id, cambioPasswordDTO);
            return ResponseEntity.ok(Map.of("message", "Contraseña actualizada exitosamente"));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("no encontrado")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", errorMessage));
            }
            return ResponseEntity.badRequest()
                    .body(Map.of("error", errorMessage));
        }
    }
    
    /**
     * Subir/actualizar foto del estudiante
     * POST /api/estudiantes/{id}/foto
     */
    @PostMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> actualizarFoto(
            @PathVariable Integer id,
            @RequestParam("foto") MultipartFile foto) {
        try {
            if (foto == null || foto.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No se proporcionó ningún archivo"));
            }
            
            FileStorageService.FileInfo fileInfo = estudianteService.actualizarFoto(id, foto);
            
            return ResponseEntity.ok(FotoResponseDTO.success(
                    "Foto actualizada exitosamente",
                    fileInfo.getFileUrl(),
                    fileInfo.getStoredFileName()
            ));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("no encontrado")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", errorMessage));
            }
            return ResponseEntity.badRequest()
                    .body(Map.of("error", errorMessage));
        }
    }
    
    /**
     * Eliminar foto del estudiante
     * DELETE /api/estudiantes/{id}/foto
     */
    @DeleteMapping("/{id}/foto")
    public ResponseEntity<?> eliminarFoto(@PathVariable Integer id) {
        try {
            estudianteService.eliminarFoto(id);
            return ResponseEntity.ok(Map.of("message", "Foto eliminada exitosamente"));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("no encontrado")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", errorMessage));
            }
            return ResponseEntity.badRequest()
                    .body(Map.of("error", errorMessage));
        }
    }
    
    /**
     * Actualizar datos generales del estudiante (parcial)
     * PATCH /api/estudiantes/{id}
     */
    @PatchMapping("/{id}")
    public ResponseEntity<?> actualizarParcial(
            @PathVariable Integer id,
            @Valid @RequestBody ActualizarEstudianteDTO dto) {
        try {
            Estudiante actualizado = estudianteService.actualizarParcial(id, dto);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("no encontrado")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", errorMessage));
            }
            return ResponseEntity.badRequest()
                    .body(Map.of("error", errorMessage));
        }
    }
    
    // ================== ENDPOINTS PARA GESTIÓN DE ESTADO DE PAGO ==================
    
    /**
     * Obtener todos los estudiantes con su estado de pago
     * GET /api/estudiantes/con-estado-pago
     */
    @GetMapping("/con-estado-pago")
    public ResponseEntity<List<EstudianteConEstadoPagoDTO>> obtenerTodosConEstadoPago() {
        return ResponseEntity.ok(estudianteService.obtenerTodosConEstadoPago());
    }
    
    /**
     * Obtener estudiantes activos con su estado de pago
     * GET /api/estudiantes/activos/con-estado-pago
     */
    @GetMapping("/activos/con-estado-pago")
    public ResponseEntity<List<EstudianteConEstadoPagoDTO>> obtenerActivosConEstadoPago() {
        return ResponseEntity.ok(estudianteService.obtenerActivosConEstadoPago());
    }
    
    /**
     * Obtener estudiantes por estado de pago específico
     * GET /api/estudiantes/estado-pago/{estadoPago}
     */
    @GetMapping("/estado-pago/{estadoPago}")
    public ResponseEntity<List<EstudianteConEstadoPagoDTO>> obtenerPorEstadoPago(
            @PathVariable Estudiante.EstadoPago estadoPago) {
        return ResponseEntity.ok(estudianteService.obtenerPorEstadoPago(estadoPago));
    }
    
    /**
     * Obtener estudiantes en mora
     * GET /api/estudiantes/en-mora
     */
    @GetMapping("/en-mora")
    public ResponseEntity<List<EstudianteConEstadoPagoDTO>> obtenerEstudiantesEnMora() {
        return ResponseEntity.ok(estudianteService.obtenerEstudiantesEnMora());
    }
    
    /**
     * Obtener estudiantes pendientes por pagar
     * GET /api/estudiantes/pendientes-pago
     */
    @GetMapping("/pendientes-pago")
    public ResponseEntity<List<EstudianteConEstadoPagoDTO>> obtenerEstudiantesPendientes() {
        return ResponseEntity.ok(estudianteService.obtenerEstudiantesPendientes());
    }
    
    /**
     * Obtener estudiantes al día
     * GET /api/estudiantes/al-dia
     */
    @GetMapping("/al-dia")
    public ResponseEntity<List<EstudianteConEstadoPagoDTO>> obtenerEstudiantesAlDia() {
        return ResponseEntity.ok(estudianteService.obtenerEstudiantesAlDia());
    }
    
    /**
     * Obtener estudiantes de una sede con estado de pago
     * GET /api/estudiantes/sede/{idSede}/con-estado-pago
     */
    @GetMapping("/sede/{idSede}/con-estado-pago")
    public ResponseEntity<List<EstudianteConEstadoPagoDTO>> obtenerPorSedeConEstadoPago(
            @PathVariable Integer idSede) {
        return ResponseEntity.ok(estudianteService.obtenerPorSedeConEstadoPago(idSede));
    }
    
    /**
     * Cambiar estado de pago de un estudiante manualmente
     * PATCH /api/estudiantes/{id}/estado-pago
     */
    @PatchMapping("/{id}/estado-pago")
    public ResponseEntity<?> cambiarEstadoPago(
            @PathVariable Integer id,
            @RequestBody CambioEstadoPagoDTO cambioDTO) {
        try {
            Estudiante actualizado = estudianteService.cambiarEstadoPago(id, cambioDTO);
            return ResponseEntity.ok(Map.of(
                    "message", "Estado de pago actualizado exitosamente",
                    "estudiante", actualizado.getNombreCompleto(),
                    "nuevoEstado", actualizado.getEstadoPago().name(),
                    "descripcion", EstudianteConEstadoPagoDTO.getDescripcionEstadoPago(actualizado.getEstadoPago())
            ));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("no encontrado")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", errorMessage));
            }
            return ResponseEntity.badRequest()
                    .body(Map.of("error", errorMessage));
        }
    }
    
    /**
     * Registrar pago en efectivo y actualizar estado
     * POST /api/estudiantes/{id}/pago-efectivo
     */
    @PostMapping("/{id}/pago-efectivo")
    public ResponseEntity<?> registrarPagoEfectivo(
            @PathVariable Integer id,
            @RequestBody RegistroPagoEfectivoDTO pagoDTO) {
        try {
            pagoDTO.setIdEstudiante(id);
            Pago pago = estudianteService.registrarPagoEfectivo(pagoDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "message", "Pago en efectivo registrado exitosamente",
                    "idPago", pago.getIdPago(),
                    "mesPagado", pago.getMesPagado(),
                    "valor", pago.getValor(),
                    "referencia", pago.getReferenciaPago(),
                    "nuevoEstadoEstudiante", "AL_DIA"
            ));
        } catch (RuntimeException e) {
            String errorMessage = e.getMessage();
            if (errorMessage.contains("no encontrado")) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", errorMessage));
            }
            return ResponseEntity.badRequest()
                    .body(Map.of("error", errorMessage));
        }
    }
    
    /**
     * Verificar y actualizar estados de pago (ejecutar manualmente o por scheduler)
     * POST /api/estudiantes/verificar-estados-pago
     */
    @PostMapping("/verificar-estados-pago")
    public ResponseEntity<?> verificarEstadosPago() {
        try {
            estudianteService.verificarEstadosPago();
            return ResponseEntity.ok(Map.of(
                    "message", "Verificación de estados de pago completada"
            ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al verificar estados: " + e.getMessage()));
        }
    }
}
