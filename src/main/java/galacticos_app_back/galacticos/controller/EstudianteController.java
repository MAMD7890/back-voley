package galacticos_app_back.galacticos.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
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
import galacticos_app_back.galacticos.dto.EstudianteResponseDTO;
import galacticos_app_back.galacticos.dto.ExcelImportResponseDTO;
import galacticos_app_back.galacticos.dto.FotoResponseDTO;
import galacticos_app_back.galacticos.dto.RegistroPagoEfectivoDTO;
import galacticos_app_back.galacticos.dto.auth.AuthResponse;
import galacticos_app_back.galacticos.entity.Estudiante;
import galacticos_app_back.galacticos.entity.Pago;
import galacticos_app_back.galacticos.service.EstudianteService;
import galacticos_app_back.galacticos.service.ExcelImportService;
import galacticos_app_back.galacticos.service.FileStorageService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/estudiantes")
public class EstudianteController {
    
    @Autowired
    private EstudianteService estudianteService;
    
    @Autowired
    private ExcelImportService excelImportService;
    
    @GetMapping
    public ResponseEntity<List<Estudiante>> obtenerTodos() {
        return ResponseEntity.ok(estudianteService.obtenerTodos());
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<EstudianteResponseDTO> obtenerPorId(@PathVariable Integer id) {
        Optional<Estudiante> estudiante = estudianteService.obtenerPorId(id);
        return estudiante.map(est -> ResponseEntity.ok(EstudianteResponseDTO.fromEntity(est)))
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
    public ResponseEntity<EstudianteResponseDTO> actualizar(@PathVariable Integer id, @RequestBody Estudiante estudiante) {
        Estudiante actualizado = estudianteService.actualizar(id, estudiante);
        return actualizado != null ? ResponseEntity.ok(EstudianteResponseDTO.fromEntity(actualizado)) : ResponseEntity.notFound().build();
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
            return ResponseEntity.ok(EstudianteResponseDTO.fromEntity(actualizado));
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
    
    /**
     * Descargar plantilla de Excel para importación de estudiantes
     * 
     * Especificación: GET /api/estudiantes/descargar-plantilla
     * 
     * Funcionalidad:
     * 1. Genera un archivo Excel con todos los campos requeridos
     * 2. Incluye 3 ejemplos de estudiantes con datos completos
     * 3. Headers resaltados en azul para mejor visualización
     * 4. Todas las validaciones necesarias documentadas
     * 
     * Respuesta:
     * - Archivo Excel descargable con nombre: plantilla-estudiantes-FECHA.xlsx
     * - Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
     */
    @GetMapping("/descargar-plantilla")
    public ResponseEntity<?> descargarPlantilla() {
        try {
            byte[] excelBytes = excelImportService.generarPlantillaExcel();
            
            return ResponseEntity.ok()
                    .contentType(MediaType.valueOf("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .header("Content-Disposition", "attachment; filename=plantilla-estudiantes-" + 
                            LocalDate.now() + ".xlsx")
                    .body(excelBytes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                        "error", "Error al generar la plantilla",
                        "detalles", e.getMessage()
                    ));
        }
    }
    
    /**
     * Importar estudiantes masivamente desde un archivo Excel
     * 
     * Especificación: POST /api/estudiantes/importar-excel?sedeId={id}
     * 
     * Parámetros:
     * - file (multipart/form-data): Archivo Excel (.xlsx) - máximo 10MB
     * - sedeId (query param): ID de la sede destino
     * 
     * Autenticación: Bearer Token (JWT) requerido
     * Autorización: Solo administradores
     * 
     * Funcionalidad:
     * 1. Valida que el archivo sea .xlsx
     * 2. Valida tamaño máximo (10MB)
     * 3. Para cada fila válida:
     *    - Crea o actualiza Estudiante
     *    - Crea Usuario con username y password temporal
     *    - Genera credenciales seguras
     * 4. Registra auditoría de importación
     * 5. Retorna reporte detallado
     */
    @Transactional
    @PostMapping("/importar-excel")
    public ResponseEntity<?> importarExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sedeId") Integer sedeId) {
        try {
            // Validación 1: Archivo presente
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                            "error", "Archivo no seleccionado",
                            "detalles", "El campo 'file' es requerido en el form-data"
                        ));
            }
            
            // Validación 2: Tipo de archivo
            if (!file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                            "error", "Formato de archivo inválido",
                            "detalles", "Solo se aceptan archivos .xlsx (Excel 2007+)"
                        ));
            }
            
            // Validación 3: Tamaño máximo (10MB)
            long maxSize = 10 * 1024 * 1024;  // 10MB
            if (file.getSize() > maxSize) {
                return ResponseEntity.status(413)
                        .body(Map.of(
                            "error", "Archivo demasiado grande",
                            "detalles", "El archivo no debe exceder 10MB"
                        ));
            }
            
            // Validación 4: sedeId válido
            if (sedeId == null || sedeId <= 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of(
                            "error", "Sede inválida",
                            "detalles", "El parámetro sedeId es requerido y debe ser mayor a 0"
                        ));
            }
            
            // Procesar la importación
            ExcelImportResponseDTO resultado = estudianteService.procesarImportacionExcelConUsuarios(
                    file.getInputStream(), 
                    sedeId,
                    file.getOriginalFilename(),
                    file.getSize()
            );
            
            return ResponseEntity.ok(resultado);
            
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(404)
                    .body(Map.of(
                        "error", "Sede no encontrada",
                        "detalles", e.getMessage()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(Map.of(
                        "error", "Error interno del servidor",
                        "detalles", "Error al procesar el archivo Excel: " + e.getMessage()
                    ));
        }
    }
}