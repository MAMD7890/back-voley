package galacticos_app_back.galacticos.service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import galacticos_app_back.galacticos.dto.ExcelEstudianteImportDTO;
import galacticos_app_back.galacticos.entity.Estudiante;
import galacticos_app_back.galacticos.entity.Sede;
import galacticos_app_back.galacticos.repository.EstudianteRepository;
import galacticos_app_back.galacticos.repository.SedeRepository;

/**
 * Servicio para importar estudiantes desde archivos Excel
 */
@Service
public class ExcelImportService {
    
    private static final Logger logger = LoggerFactory.getLogger(ExcelImportService.class);
    
    @Autowired
    private SedeRepository sedeRepository;
    
    @Autowired
    private EstudianteRepository estudianteRepository;
    
    // Mapeo de nombres de columnas del Excel a índices
    private static final Map<String, Integer> COLUMN_INDEX_MAP = new HashMap<>();
    
    static {
        COLUMN_INDEX_MAP.put("Nombres y Apellidos", 1);
        COLUMN_INDEX_MAP.put("Tipo de Documento", 2);
        COLUMN_INDEX_MAP.put("Número de Documento", 3);
        COLUMN_INDEX_MAP.put("Fecha de Nacimiento", 4);
        COLUMN_INDEX_MAP.put("Edad", 5);
        COLUMN_INDEX_MAP.put("Sexo", 6);
        COLUMN_INDEX_MAP.put("Dirección de Residencia", 7);
        COLUMN_INDEX_MAP.put("Barrio", 8);
        COLUMN_INDEX_MAP.put("Celular de Contacto Estudiante", 9);
        COLUMN_INDEX_MAP.put("Número de WhatsApp Estudiante", 10);
        COLUMN_INDEX_MAP.put("Correo Electrónico", 11);
        COLUMN_INDEX_MAP.put("Sede", 12);
        COLUMN_INDEX_MAP.put("Nombre del Padre/Madre/Tutor", 13);
        COLUMN_INDEX_MAP.put("Parentesco", 14);
        COLUMN_INDEX_MAP.put("Número de Documento del Tutor", 15);
        COLUMN_INDEX_MAP.put("Teléfono del Tutor", 16);
        COLUMN_INDEX_MAP.put("Correo del Tutor", 17);
        COLUMN_INDEX_MAP.put("Ocupación del Tutor", 18);
        COLUMN_INDEX_MAP.put("Institución Educativa", 19);
        COLUMN_INDEX_MAP.put("Jornada", 20);
        COLUMN_INDEX_MAP.put("Curso / Grado Actual", 21);
        COLUMN_INDEX_MAP.put("EPS / Entidad de Salud", 22);
        COLUMN_INDEX_MAP.put("Tipo de Sangre", 23);
        COLUMN_INDEX_MAP.put("Alergias", 24);
        COLUMN_INDEX_MAP.put("Enfermedades o Condiciones Médicas", 25);
        COLUMN_INDEX_MAP.put("Medicamentos que consume", 26);
        COLUMN_INDEX_MAP.put("Certificado Médico Deportivo", 27);
        COLUMN_INDEX_MAP.put("Fechas de Pago en el Mes", 28);
        COLUMN_INDEX_MAP.put("Nombre Contacto de Emergencia", 29);
        COLUMN_INDEX_MAP.put("Teléfono del contacto de emergencia", 30);
        COLUMN_INDEX_MAP.put("Parentesco del contacto", 31);
        COLUMN_INDEX_MAP.put("Ocupación del contacto", 32);
        COLUMN_INDEX_MAP.put("Correo electrónico del contacto", 33);
        COLUMN_INDEX_MAP.put("Personas LGBTIQ+", 34);
        COLUMN_INDEX_MAP.put("Personas con Discapacidad", 35);
        COLUMN_INDEX_MAP.put("Condición / Patología", 36);
        COLUMN_INDEX_MAP.put("Migrantes, Refugiados", 37);
        COLUMN_INDEX_MAP.put("Poblaciones Étnicas", 38);
        COLUMN_INDEX_MAP.put("Religión", 39);
        COLUMN_INDEX_MAP.put("Experiencia en Voleibol", 40);
        COLUMN_INDEX_MAP.put("Otras Disciplinas", 41);
        COLUMN_INDEX_MAP.put("Posición Preferida", 42);
        COLUMN_INDEX_MAP.put("Dominancia", 43);
        COLUMN_INDEX_MAP.put("Nivel Actual", 44);
        COLUMN_INDEX_MAP.put("Clubes o Escuelas Anteriores", 45);
        COLUMN_INDEX_MAP.put("Consentimiento Informado", 46);
        COLUMN_INDEX_MAP.put("Firma digital", 47);
        COLUMN_INDEX_MAP.put("Fecha de diligenciamiento", 48);
    }
    
    /**
     * Lee un archivo Excel y retorna una lista de DTOs
     * Lee dinámicamente basado en los encabezados de la fila 1
     */
    public List<ExcelEstudianteImportDTO> leerExcel(InputStream inputStream) throws Exception {
        List<ExcelEstudianteImportDTO> estudiantes = new ArrayList<>();
        
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            
            // Leer la fila de encabezados (fila 0)
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new Exception("El archivo Excel no tiene encabezados en la primera fila");
            }
            
            // Mapear índices de columnas basado en los headers
            Map<String, Integer> columnIndex = mapearColumnasHeader(headerRow);
            
            // Procesar filas de datos (comenzar desde fila 1)
            for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                
                if (row == null || esFilaVacia(row)) {
                    continue;
                }
                
                ExcelEstudianteImportDTO dto = mapearFilaDinamica(row, rowIndex + 1, columnIndex);
                if (dto != null) {
                    estudiantes.add(dto);
                }
            }
        }
        
        return estudiantes;
    }
    
    /**
     * Mapea los índices de columnas basado en los headers
     */
    private Map<String, Integer> mapearColumnasHeader(Row headerRow) {
        Map<String, Integer> columnIndex = new HashMap<>();
        
        for (int colIndex = 0; colIndex < headerRow.getLastCellNum(); colIndex++) {
            Cell cell = headerRow.getCell(colIndex);
            if (cell != null) {
                String header = cell.getStringCellValue()
                    .trim()
                    .toLowerCase()
                    .replaceAll("\\*", "")           // Remover asteriscos
                    .replaceAll("\\s+", " ")         // Normalizar espacios múltiples
                    .replaceAll("\\(.*?\\)", "")     // Remover contenido entre paréntesis
                    .trim();                          // Trim final
                
                logger.debug("Header normalizado [" + colIndex + "]: '" + header + "'");
                
                // Si el header no está vacío después de normalizar, agregarlo
                if (!header.isEmpty()) {
                    columnIndex.put(header, colIndex);
                }
            }
        }
        
        logger.info("Headers encontrados: " + columnIndex.keySet());
        return columnIndex;
    }
    
    /**
     * Mapea una fila del Excel a un DTO dinámicamente basado en los headers
     */
    private ExcelEstudianteImportDTO mapearFilaDinamica(Row row, int numeroFila, Map<String, Integer> columnIndex) {
        try {
            ExcelEstudianteImportDTO dto = new ExcelEstudianteImportDTO();
            dto.setNumeroFila(numeroFila);
            
            // Función auxiliar para obtener valor por header
            java.util.function.Function<String, String> obtenerValor = (String header) -> {
                Integer colIdx = columnIndex.get(header.toLowerCase());
                if (colIdx != null) {
                    return getCellValueString(row, colIdx);
                }
                return null;
            };
            
            java.util.function.Function<String, Integer> obtenerInt = (String header) -> {
                Integer colIdx = columnIndex.get(header.toLowerCase());
                if (colIdx != null) {
                    return getCellValueInt(row, colIdx);
                }
                return null;
            };
            
            java.util.function.Function<String, LocalDate> obtenerFecha = (String header) -> {
                Integer colIdx = columnIndex.get(header.toLowerCase());
                if (colIdx != null) {
                    return getCellValueDate(row, colIdx);
                }
                return null;
            };
            
            java.util.function.Function<String, Boolean> obtenerBoolean = (String header) -> {
                Integer colIdx = columnIndex.get(header.toLowerCase());
                if (colIdx != null) {
                    return getCellValueBoolean(row, colIdx);
                }
                return null;
            };
            
            // Información personal - buscar en múltiples variaciones de headers
            dto.setNombreCompleto(
                obtenerValor.apply("nombre completo") != null ? obtenerValor.apply("nombre completo") :
                obtenerValor.apply("nombrecompleto") != null ? obtenerValor.apply("nombrecompleto") :
                obtenerValor.apply("nombre_completo") != null ? obtenerValor.apply("nombre_completo") : null
            );
            
            dto.setTipoDocumento(
                obtenerValor.apply("tipo documento") != null ? obtenerValor.apply("tipo documento") :
                obtenerValor.apply("tipodocumento") != null ? obtenerValor.apply("tipodocumento") :
                obtenerValor.apply("tipo_documento") != null ? obtenerValor.apply("tipo_documento") : null
            );
            
            dto.setNumeroDocumento(
                obtenerValor.apply("numero documento") != null ? obtenerValor.apply("numero documento") :
                obtenerValor.apply("numerodocumento") != null ? obtenerValor.apply("numerodocumento") :
                obtenerValor.apply("numero_documento") != null ? obtenerValor.apply("numero_documento") : null
            );
            
            dto.setFechaNacimiento(
                obtenerFecha.apply("fecha nacimiento (dd/mm/yyyy)") != null ? obtenerFecha.apply("fecha nacimiento (dd/mm/yyyy)") :
                obtenerFecha.apply("fecha nacimiento") != null ? obtenerFecha.apply("fecha nacimiento") :
                obtenerFecha.apply("fechanacimiento") != null ? obtenerFecha.apply("fechanacimiento") :
                obtenerFecha.apply("fecha_nacimiento") != null ? obtenerFecha.apply("fecha_nacimiento") : null
            );
            
            dto.setEdad(
                obtenerInt.apply("edad") != null ? obtenerInt.apply("edad") : null
            );
            
            dto.setSexo(
                obtenerValor.apply("sexo") != null ? obtenerValor.apply("sexo") : null
            );
            
            // Información de contacto
            dto.setDireccionResidencia(
                obtenerValor.apply("direccion residencia") != null ? obtenerValor.apply("direccion residencia") :
                obtenerValor.apply("direccionresidencia") != null ? obtenerValor.apply("direccionresidencia") :
                obtenerValor.apply("direccion_residencia") != null ? obtenerValor.apply("direccion_residencia") :
                obtenerValor.apply("dirección residencia") != null ? obtenerValor.apply("dirección residencia") : null
            );
            
            dto.setBarrio(
                obtenerValor.apply("barrio") != null ? obtenerValor.apply("barrio") : null
            );
            
            dto.setCelularEstudiante(
                obtenerValor.apply("celular estudiante") != null ? obtenerValor.apply("celular estudiante") :
                obtenerValor.apply("celularestudiante") != null ? obtenerValor.apply("celularestudiante") :
                obtenerValor.apply("celular_estudiante") != null ? obtenerValor.apply("celular_estudiante") :
                obtenerValor.apply("celular") != null ? obtenerValor.apply("celular") : null
            );
            
            dto.setWhatsappEstudiante(
                obtenerValor.apply("whatsapp estudiante") != null ? obtenerValor.apply("whatsapp estudiante") :
                obtenerValor.apply("whatsappestudiante") != null ? obtenerValor.apply("whatsappestudiante") :
                obtenerValor.apply("whatsapp_estudiante") != null ? obtenerValor.apply("whatsapp_estudiante") :
                obtenerValor.apply("whatsapp") != null ? obtenerValor.apply("whatsapp") : null
            );
            
            dto.setCorreoEstudiante(
                obtenerValor.apply("correo estudiante") != null ? obtenerValor.apply("correo estudiante") :
                obtenerValor.apply("correoestudiante") != null ? obtenerValor.apply("correoestudiante") :
                obtenerValor.apply("correo_estudiante") != null ? obtenerValor.apply("correo_estudiante") :
                obtenerValor.apply("correo") != null ? obtenerValor.apply("correo") :
                obtenerValor.apply("email") != null ? obtenerValor.apply("email") : null
            );
            
            // Información del tutor
            dto.setNombreTutor(
                obtenerValor.apply("nombre tutor") != null ? obtenerValor.apply("nombre tutor") :
                obtenerValor.apply("nombretutor") != null ? obtenerValor.apply("nombretutor") :
                obtenerValor.apply("nombre_tutor") != null ? obtenerValor.apply("nombre_tutor") : null
            );
            
            dto.setParentescoTutor(
                obtenerValor.apply("parentesco tutor") != null ? obtenerValor.apply("parentesco tutor") :
                obtenerValor.apply("parentescotutor") != null ? obtenerValor.apply("parentescotutor") :
                obtenerValor.apply("parentesco_tutor") != null ? obtenerValor.apply("parentesco_tutor") : null
            );
            
            dto.setDocumentoTutor(
                obtenerValor.apply("documento tutor") != null ? obtenerValor.apply("documento tutor") :
                obtenerValor.apply("documentotutor") != null ? obtenerValor.apply("documentotutor") :
                obtenerValor.apply("documento_tutor") != null ? obtenerValor.apply("documento_tutor") : null
            );
            
            dto.setTelefonoTutor(
                obtenerValor.apply("telefono tutor") != null ? obtenerValor.apply("telefono tutor") :
                obtenerValor.apply("telefonotutor") != null ? obtenerValor.apply("telefonotutor") :
                obtenerValor.apply("telefono_tutor") != null ? obtenerValor.apply("telefono_tutor") :
                obtenerValor.apply("teléfono tutor") != null ? obtenerValor.apply("teléfono tutor") : null
            );
            
            dto.setCorreoTutor(
                obtenerValor.apply("correo tutor") != null ? obtenerValor.apply("correo tutor") :
                obtenerValor.apply("correotutor") != null ? obtenerValor.apply("correotutor") :
                obtenerValor.apply("correo_tutor") != null ? obtenerValor.apply("correo_tutor") : null
            );
            
            dto.setOcupacionTutor(
                obtenerValor.apply("ocupacion tutor") != null ? obtenerValor.apply("ocupacion tutor") :
                obtenerValor.apply("ocupaciontutor") != null ? obtenerValor.apply("ocupaciontutor") :
                obtenerValor.apply("ocupacion_tutor") != null ? obtenerValor.apply("ocupacion_tutor") :
                obtenerValor.apply("ocupación tutor") != null ? obtenerValor.apply("ocupación tutor") : null
            );
            
            // Información académica
            dto.setInstitucionEducativa(
                obtenerValor.apply("institucion educativa") != null ? obtenerValor.apply("institucion educativa") :
                obtenerValor.apply("institucioneducativa") != null ? obtenerValor.apply("institucioneducativa") :
                obtenerValor.apply("institucion_educativa") != null ? obtenerValor.apply("institucion_educativa") :
                obtenerValor.apply("institución educativa") != null ? obtenerValor.apply("institución educativa") : null
            );
            
            dto.setJornada(
                obtenerValor.apply("jornada") != null ? obtenerValor.apply("jornada") : null
            );
            
            dto.setGradoActual(
                obtenerInt.apply("grado actual") != null ? obtenerInt.apply("grado actual") :
                obtenerInt.apply("gradoactual") != null ? obtenerInt.apply("gradoactual") :
                obtenerInt.apply("grado_actual") != null ? obtenerInt.apply("grado_actual") : null
            );
            
            // Información médica
            dto.setEps(
                obtenerValor.apply("eps") != null ? obtenerValor.apply("eps") : null
            );
            
            dto.setTipoSangre(
                obtenerValor.apply("tipo sangre") != null ? obtenerValor.apply("tipo sangre") :
                obtenerValor.apply("tiposangre") != null ? obtenerValor.apply("tiposangre") :
                obtenerValor.apply("tipo_sangre") != null ? obtenerValor.apply("tipo_sangre") : null
            );
            
            dto.setAlergias(
                obtenerValor.apply("alergias") != null ? obtenerValor.apply("alergias") : null
            );
            
            dto.setEnfermedadesCondiciones(
                obtenerValor.apply("enfermedades/condiciones") != null ? obtenerValor.apply("enfermedades/condiciones") :
                obtenerValor.apply("enfermedades condiciones") != null ? obtenerValor.apply("enfermedades condiciones") :
                obtenerValor.apply("enfermedadescondiciones") != null ? obtenerValor.apply("enfermedadescondiciones") :
                obtenerValor.apply("enfermedades_condiciones") != null ? obtenerValor.apply("enfermedades_condiciones") : null
            );
            
            dto.setMedicamentos(
                obtenerValor.apply("medicamentos") != null ? obtenerValor.apply("medicamentos") : null
            );
            
            dto.setCertificadoMedicoDeportivo(
                obtenerBoolean.apply("certificado medico deportivo (si/no)") != null ? obtenerBoolean.apply("certificado medico deportivo (si/no)") :
                obtenerBoolean.apply("certificado medico deportivo") != null ? obtenerBoolean.apply("certificado medico deportivo") : false
            );
            
            // Información de pago
            dto.setDiaPagoMes(
                obtenerInt.apply("dia pago mes") != null ? obtenerInt.apply("dia pago mes") :
                obtenerInt.apply("diapagomes") != null ? obtenerInt.apply("diapagomes") :
                obtenerInt.apply("dia_pago_mes") != null ? obtenerInt.apply("dia_pago_mes") :
                obtenerInt.apply("día pago mes") != null ? obtenerInt.apply("día pago mes") : null
            );
            
            // Información de emergencia
            dto.setNombreEmergencia(
                obtenerValor.apply("nombre emergencia") != null ? obtenerValor.apply("nombre emergencia") :
                obtenerValor.apply("nombreemergencia") != null ? obtenerValor.apply("nombreemergencia") :
                obtenerValor.apply("nombre_emergencia") != null ? obtenerValor.apply("nombre_emergencia") : null
            );
            
            dto.setTelefonoEmergencia(
                obtenerValor.apply("telefono emergencia") != null ? obtenerValor.apply("telefono emergencia") :
                obtenerValor.apply("telefonoemergencia") != null ? obtenerValor.apply("telefonoemergencia") :
                obtenerValor.apply("telefono_emergencia") != null ? obtenerValor.apply("telefono_emergencia") :
                obtenerValor.apply("teléfono emergencia") != null ? obtenerValor.apply("teléfono emergencia") : null
            );
            
            dto.setParentescoEmergencia(
                obtenerValor.apply("parentesco emergencia") != null ? obtenerValor.apply("parentesco emergencia") :
                obtenerValor.apply("parentescoemergencia") != null ? obtenerValor.apply("parentescoemergencia") :
                obtenerValor.apply("parentesco_emergencia") != null ? obtenerValor.apply("parentesco_emergencia") : null
            );
            
            dto.setOcupacionEmergencia(
                obtenerValor.apply("ocupacion emergencia") != null ? obtenerValor.apply("ocupacion emergencia") :
                obtenerValor.apply("ocupacionemergencia") != null ? obtenerValor.apply("ocupacionemergencia") :
                obtenerValor.apply("ocupacion_emergencia") != null ? obtenerValor.apply("ocupacion_emergencia") :
                obtenerValor.apply("ocupación emergencia") != null ? obtenerValor.apply("ocupación emergencia") : null
            );
            
            dto.setCorreoEmergencia(
                obtenerValor.apply("correo emergencia") != null ? obtenerValor.apply("correo emergencia") :
                obtenerValor.apply("correoemergencia") != null ? obtenerValor.apply("correoemergencia") :
                obtenerValor.apply("correo_emergencia") != null ? obtenerValor.apply("correo_emergencia") : null
            );
            
            // Poblaciones vulnerables
            dto.setPerteneceIgbtiq(
                obtenerBoolean.apply("pertenece lgbtiq (si/no)") != null ? obtenerBoolean.apply("pertenece lgbtiq (si/no)") :
                obtenerBoolean.apply("pertenece lgbtiq") != null ? obtenerBoolean.apply("pertenece lgbtiq") : false
            );
            
            dto.setPersonaDiscapacidad(
                obtenerBoolean.apply("persona discapacidad (si/no)") != null ? obtenerBoolean.apply("persona discapacidad (si/no)") :
                obtenerBoolean.apply("persona discapacidad") != null ? obtenerBoolean.apply("persona discapacidad") : false
            );
            
            dto.setCondicionDiscapacidad(
                obtenerValor.apply("condicion discapacidad") != null ? obtenerValor.apply("condicion discapacidad") :
                obtenerValor.apply("condiciondiscapacidad") != null ? obtenerValor.apply("condiciondiscapacidad") :
                obtenerValor.apply("condicion_discapacidad") != null ? obtenerValor.apply("condicion_discapacidad") :
                obtenerValor.apply("condición discapacidad") != null ? obtenerValor.apply("condición discapacidad") : null
            );
            
            dto.setMigranteRefugiado(
                obtenerBoolean.apply("migrante/refugiado (si/no)") != null ? obtenerBoolean.apply("migrante/refugiado (si/no)") :
                obtenerBoolean.apply("migrante refugiado") != null ? obtenerBoolean.apply("migrante refugiado") : false
            );
            
            dto.setPoblacionEtnica(
                obtenerValor.apply("poblacion etnica") != null ? obtenerValor.apply("poblacion etnica") :
                obtenerValor.apply("poblacionetnica") != null ? obtenerValor.apply("poblacionetnica") :
                obtenerValor.apply("poblacion_etnica") != null ? obtenerValor.apply("poblacion_etnica") :
                obtenerValor.apply("población étnica") != null ? obtenerValor.apply("población étnica") : null
            );
            
            dto.setReligion(
                obtenerValor.apply("religion") != null ? obtenerValor.apply("religion") :
                obtenerValor.apply("religión") != null ? obtenerValor.apply("religión") : null
            );
            
            // Información deportiva
            dto.setExperienciaVoleibol(
                obtenerValor.apply("experiencia voleibol") != null ? obtenerValor.apply("experiencia voleibol") :
                obtenerValor.apply("experienciavoleibol") != null ? obtenerValor.apply("experienciavoleibol") :
                obtenerValor.apply("experiencia_voleibol") != null ? obtenerValor.apply("experiencia_voleibol") : null
            );
            
            dto.setOtrasDisciplinas(
                obtenerValor.apply("otras disciplinas") != null ? obtenerValor.apply("otras disciplinas") :
                obtenerValor.apply("otrasdisciplinas") != null ? obtenerValor.apply("otrasdisciplinas") :
                obtenerValor.apply("otras_disciplinas") != null ? obtenerValor.apply("otras_disciplinas") : null
            );
            
            dto.setPosicionPreferida(
                obtenerValor.apply("posicion preferida") != null ? obtenerValor.apply("posicion preferida") :
                obtenerValor.apply("posicionpreferida") != null ? obtenerValor.apply("posicionpreferida") :
                obtenerValor.apply("posicion_preferida") != null ? obtenerValor.apply("posicion_preferida") :
                obtenerValor.apply("posición preferida") != null ? obtenerValor.apply("posición preferida") : null
            );
            
            dto.setDominancia(
                obtenerValor.apply("dominancia") != null ? obtenerValor.apply("dominancia") : null
            );
            
            dto.setNivelActual(
                obtenerValor.apply("nivel actual") != null ? obtenerValor.apply("nivel actual") :
                obtenerValor.apply("nivelactual") != null ? obtenerValor.apply("nivelactual") :
                obtenerValor.apply("nivel_actual") != null ? obtenerValor.apply("nivel_actual") : null
            );
            
            dto.setClubesAnteriores(
                obtenerValor.apply("clubes anteriores") != null ? obtenerValor.apply("clubes anteriores") :
                obtenerValor.apply("clubesanteriores") != null ? obtenerValor.apply("clubesanteriores") :
                obtenerValor.apply("clubes_anteriores") != null ? obtenerValor.apply("clubes_anteriores") : null
            );
            
            return dto;
        } catch (Exception e) {
            logger.error("Error al mapear fila " + numeroFila + ": " + e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Mapea una fila del Excel a un DTO (método antiguo - por si acaso)
     */
    private ExcelEstudianteImportDTO mapearFila(Row row, int numeroFila) {
        try {
            ExcelEstudianteImportDTO dto = new ExcelEstudianteImportDTO();
            dto.setNumeroFila(numeroFila);
            
            // Información personal
            dto.setNombreCompleto(getCellValueString(row, 1));
            dto.setTipoDocumento(getCellValueString(row, 2));
            dto.setNumeroDocumento(getCellValueString(row, 3));
            dto.setFechaNacimiento(getCellValueDate(row, 4));
            dto.setEdad(getCellValueInt(row, 5));
            dto.setSexo(getCellValueString(row, 6));
            
            // Información de contacto
            dto.setDireccionResidencia(getCellValueString(row, 7));
            dto.setBarrio(getCellValueString(row, 8));
            dto.setCelularEstudiante(getCellValueString(row, 9));
            dto.setWhatsappEstudiante(getCellValueString(row, 10));
            dto.setCorreoEstudiante(getCellValueString(row, 11));
            
            // Sede
            dto.setNombreSede(getCellValueString(row, 12));
            
            // Información del tutor
            dto.setNombreTutor(getCellValueString(row, 13));
            dto.setParentescoTutor(getCellValueString(row, 14));
            dto.setDocumentoTutor(getCellValueString(row, 15));
            dto.setTelefonoTutor(getCellValueString(row, 16));
            dto.setCorreoTutor(getCellValueString(row, 17));
            dto.setOcupacionTutor(getCellValueString(row, 18));
            
            // Información académica
            dto.setInstitucionEducativa(getCellValueString(row, 19));
            dto.setJornada(getCellValueString(row, 20));
            dto.setGradoActual(getCellValueInt(row, 21));
            
            // Información médica
            dto.setEps(getCellValueString(row, 22));
            dto.setTipoSangre(getCellValueString(row, 23));
            dto.setAlergias(getCellValueString(row, 24));
            dto.setEnfermedadesCondiciones(getCellValueString(row, 25));
            dto.setMedicamentos(getCellValueString(row, 26));
            dto.setCertificadoMedicoDeportivo(getCellValueBoolean(row, 27));
            
            // Información de pago
            dto.setDiaPagoMes(getCellValueInt(row, 28));
            
            // Información de emergencia
            dto.setNombreEmergencia(getCellValueString(row, 29));
            dto.setTelefonoEmergencia(getCellValueString(row, 30));
            dto.setParentescoEmergencia(getCellValueString(row, 31));
            dto.setOcupacionEmergencia(getCellValueString(row, 32));
            dto.setCorreoEmergencia(getCellValueString(row, 33));
            
            // Poblaciones vulnerables
            dto.setPerteneceIgbtiq(getCellValueBoolean(row, 34));
            dto.setPersonaDiscapacidad(getCellValueBoolean(row, 35));
            dto.setCondicionDiscapacidad(getCellValueString(row, 36));
            dto.setMigranteRefugiado(getCellValueBoolean(row, 37));
            dto.setPoblacionEtnica(getCellValueString(row, 38));
            dto.setReligion(getCellValueString(row, 39));
            
            // Información deportiva
            dto.setExperienciaVoleibol(getCellValueString(row, 40));
            dto.setOtrasDisciplinas(getCellValueString(row, 41));
            dto.setPosicionPreferida(getCellValueString(row, 42));
            dto.setDominancia(getCellValueString(row, 43));
            dto.setNivelActual(getCellValueString(row, 44));
            dto.setClubesAnteriores(getCellValueString(row, 45));
            
            // Consentimiento
            dto.setConsentimientoInformado(getCellValueBoolean(row, 46));
            dto.setFirmaDigital(getCellValueString(row, 47));
            dto.setFechaDiligenciamiento(getCellValueDate(row, 48));
            
            dto.setProcesadoExitosamente(false);
            
            return dto;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Métodos auxiliares para obtener valores de celda
     */
    private String getCellValueString(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }
        
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((long) cell.getNumericCellValue());
        }
        return null;
    }
    
    private Integer getCellValueInt(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }
        
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return Integer.parseInt(cell.getStringCellValue().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    private LocalDate getCellValueDate(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return null;
        }
        
        if (cell.getCellType() == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                Date date = cell.getDateCellValue();
                return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
        } else if (cell.getCellType() == CellType.STRING) {
            String fechaStr = cell.getStringCellValue().trim();
            
            // Intentar múltiples formatos de fecha
            java.time.format.DateTimeFormatter[] formatos = {
                java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"),  // DD/MM/YYYY
                java.time.format.DateTimeFormatter.ofPattern("d/M/yyyy"),    // D/M/YYYY (sin ceros)
                java.time.format.DateTimeFormatter.ISO_LOCAL_DATE            // YYYY-MM-DD
            };
            
            for (java.time.format.DateTimeFormatter formato : formatos) {
                try {
                    return LocalDate.parse(fechaStr, formato);
                } catch (Exception e) {
                    // Intentar siguiente formato
                }
            }
            
            return null;
        }
        return null;
    }
    
    private Boolean getCellValueBoolean(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            return false;
        }
        
        if (cell.getCellType() == CellType.BOOLEAN) {
            return cell.getBooleanCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            String value = cell.getStringCellValue().trim().toLowerCase();
            return value.equals("sí") || value.equals("si") || value.equals("true") || value.equals("1");
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue() != 0;
        }
        return false;
    }
    
    /**
     * Verifica si una fila está vacía
     */
    private boolean esFilaVacia(Row row) {
        for (int cellIndex = row.getFirstCellNum(); cellIndex < row.getLastCellNum(); cellIndex++) {
            Cell cell = row.getCell(cellIndex);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Genera una plantilla de Excel con todos los campos requeridos y ejemplos
     */
    public byte[] generarPlantillaExcel() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Estudiantes");
            
            // Headers
            String[] headers = {
                "Nombre Completo *",
                "Tipo Documento *",
                "Numero Documento *",
                "Fecha Nacimiento (DD/MM/YYYY) *",
                "Edad",
                "Sexo",
                "Direccion Residencia",
                "Barrio",
                "Celular Estudiante",
                "WhatsApp Estudiante",
                "Correo Estudiante *",
                "Nombre Tutor",
                "Parentesco Tutor",
                "Documento Tutor",
                "Telefono Tutor",
                "Correo Tutor",
                "Ocupacion Tutor",
                "Institucion Educativa",
                "Jornada",
                "Grado Actual",
                "EPS",
                "Tipo Sangre",
                "Alergias",
                "Enfermedades/Condiciones",
                "Medicamentos",
                "Certificado Medico Deportivo (Si/No)",
                "Dia Pago Mes",
                "Nombre Emergencia",
                "Telefono Emergencia",
                "Parentesco Emergencia",
                "Ocupacion Emergencia",
                "Correo Emergencia",
                "Pertenece LGBTIQ (Si/No)",
                "Persona Discapacidad (Si/No)",
                "Condicion Discapacidad",
                "Migrante/Refugiado (Si/No)",
                "Poblacion Etnica",
                "Religion",
                "Experiencia Voleibol",
                "Otras Disciplinas",
                "Posicion Preferida",
                "Dominancia",
                "Nivel Actual",
                "Clubes Anteriores"
            };
            
            // Crear fila de headers
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                // Estilo para headers
                org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
                headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.LIGHT_BLUE.getIndex());
                headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
                headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
                headerStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
                org.apache.poi.ss.usermodel.Font font = workbook.createFont();
                font.setBold(true);
                headerStyle.setFont(font);
                cell.setCellStyle(headerStyle);
            }
            
            // Datos de ejemplo
            String[][] ejemploData = {
                {
                    "Juan Pérez García", "Cédula", "1234567890", "21/11/2001",
                    "25", "Masculino", "Calle 10 #20-30", "Centro",
                    "3001234567", "3001234567", "juan.perez@example.com",
                    "Maria García", "Madre", "1098765432", "3109876543", "maria@example.com", "Docente",
                    "Colegio XYZ", "Matutina", "10", "EPS Salud", "O+",
                    "Ninguna", "Ninguna", "Ninguno", "No", "15",
                    "Carlos García", "3101234567", "Abuelo", "Jubilado", "carlos@example.com",
                    "No", "No", "Ninguna", "No", "Ninguna", "Católica",
                    "Intermedio", "Futbol", "Diestro", "Intermedio", "Club Deportivo"
                },
                {
                    "María López Rodríguez", "Cédula", "9876543210", "15/03/2002",
                    "22", "Femenino", "Carrera 5 #15-40", "Nororiental",
                    "3009876543", "3009876543", "maria.lopez@example.com",
                    "Juan López", "Padre", "1087654321", "3107654321", "juan@example.com", "Ingeniero",
                    "Instituto ABC", "Vespertina", "11", "EPS Plus", "AB-",
                    "Polen", "Ninguna", "Ninguno", "No", "10",
                    "Rosa López", "3108765432", "Abuela", "Ama de casa", "rosa@example.com",
                    "No", "No", "Ninguna", "No", "Ninguna", "Protestante",
                    "Avanzado", "Natación", "Zurda", "Avanzado", "Club Acuático"
                },
                {
                    "Carlos Gómez Martínez", "Cédula", "5555555555", "10/07/2001",
                    "24", "Masculino", "Avenida Boyacá #50-60", "Centro Sur",
                    "3005555555", "3005555555", "carlos.gomez@example.com",
                    "Patricia Martínez", "Madre", "1055555555", "3105555555", "patricia@example.com", "Comerciante",
                    "Liceo DEF", "Única", "9", "Saludcoop", "A+",
                    "Maní", "Asma", "Inhalador", "No", "20",
                    "Jorge Gómez", "3106666666", "Tío", "Abogado", "jorge@example.com",
                    "No", "No", "Ninguna", "No", "Afrodescendiente", "Evangélica",
                    "Principiante", "Tenis", "Diestro", "Principiante", "Club de Tenis"
                }
            };
            
            // Agregar datos de ejemplo
            for (int rowIndex = 0; rowIndex < ejemploData.length; rowIndex++) {
                Row row = sheet.createRow(rowIndex + 1);
                for (int colIndex = 0; colIndex < ejemploData[rowIndex].length; colIndex++) {
                    Cell cell = row.createCell(colIndex);
                    cell.setCellValue(ejemploData[rowIndex][colIndex]);
                }
            }
            
            // Ajustar ancho de columnas
            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, 20 * 256);
            }
            
            // Convertir a bytes
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        }
    }
}
