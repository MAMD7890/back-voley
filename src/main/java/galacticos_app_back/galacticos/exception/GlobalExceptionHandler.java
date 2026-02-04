package galacticos_app_back.galacticos.exception;

import galacticos_app_back.galacticos.dto.auth.MessageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        
        String mensaje = ex.getMessage();
        System.out.println("Error de deserialización JSON: " + mensaje);
        
        // Extraer información útil del error
        if (mensaje != null && mensaje.contains("Cannot deserialize value of type")) {
            if (mensaje.contains("TipoDocumento")) {
                response.put("error", "Valor inválido para tipoDocumento. Valores válidos: TI, CC, RC, PASAPORTE");
            } else if (mensaje.contains("NivelActual")) {
                response.put("error", "Valor inválido para nivelActual. Valores válidos: INICIANTE, INTERMEDIO, AVANZADO");
            } else if (mensaje.contains("Sexo")) {
                response.put("error", "Valor inválido para sexo. Valores válidos: MASCULINO, FEMENINO, OTRO");
            } else if (mensaje.contains("Jornada")) {
                response.put("error", "Valor inválido para jornada. Valores válidos: MAÑANA, TARDE, NOCHE, UNICA");
            } else if (mensaje.contains("Dominancia")) {
                response.put("error", "Valor inválido para dominancia. Valores válidos: DERECHA, IZQUIERDA, AMBIDIESTRO");
            } else if (mensaje.contains("LocalDate")) {
                response.put("error", "Formato de fecha inválido. Use el formato: YYYY-MM-DD (ej: 2010-03-15)");
            } else {
                response.put("error", "Error al procesar los datos JSON: " + mensaje.substring(0, Math.min(mensaje.length(), 200)));
            }
        } else {
            response.put("error", "Error al leer el cuerpo de la solicitud. Verifique que el JSON sea válido.");
        }
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", "Error de validación");
        response.put("errors", errors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<MessageResponse> handleBadCredentialsException(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(MessageResponse.error("Credenciales inválidas"));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<MessageResponse> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(MessageResponse.error("Error de autenticación: " + ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<MessageResponse> handleAccessDeniedException(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(MessageResponse.error("Acceso denegado: No tienes permisos para realizar esta acción"));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<MessageResponse> handleRuntimeException(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(MessageResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<MessageResponse> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(MessageResponse.error("Error interno del servidor"));
    }
}
