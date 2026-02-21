package galacticos_app_back.galacticos.controller;

import galacticos_app_back.galacticos.dto.auth.*;
import galacticos_app_back.galacticos.entity.Usuario;
import galacticos_app_back.galacticos.service.AuthService;
import galacticos_app_back.galacticos.service.FileStorageService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private FileStorageService fileStorageService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            AuthResponse response = authService.login(loginRequest);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MessageResponse.error(e.getMessage()));
        }
    }

    /**
     * Registro de usuario con foto de perfil obligatoria
     * Usa multipart/form-data
     */
    @PostMapping(value = "/register", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> register(
            @RequestParam("nombre") String nombre,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam(value = "idRol", required = false) Integer idRol,
            @RequestParam("foto") MultipartFile foto) {
        try {
            // Validar que la foto no esté vacía
            if (foto == null || foto.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(MessageResponse.error("La foto de perfil es obligatoria"));
            }

            // Validar campos obligatorios
            if (nombre == null || nombre.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(MessageResponse.error("El nombre es obligatorio"));
            }
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(MessageResponse.error("El email es obligatorio"));
            }
            if (password == null || password.length() < 6) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(MessageResponse.error("La contraseña debe tener al menos 6 caracteres"));
            }

            // Crear el request
            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setNombre(nombre.trim());
            registerRequest.setEmail(email.trim());
            registerRequest.setPassword(password);
            registerRequest.setIdRol(idRol);

            // Procesar y guardar la foto
            FileStorageService.FileInfo fileInfo = fileStorageService.storeProfilePhoto(foto);
            registerRequest.setFotoUrl(fileInfo.getFileUrl());
            registerRequest.setFotoNombre(fileInfo.getStoredFileName());

            AuthResponse response = authService.register(registerRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(MessageResponse.error(e.getMessage()));
        }
    }

    /**
     * Registro de estudiante (asigna automáticamente el rol ESTUDIANTE)
     * Usa multipart/form-data
     */
    @PostMapping(value = "/register-student", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> registerStudent(
            @RequestParam("nombre") String nombre,
            @RequestParam("email") String email,
            @RequestParam("password") String password,
            @RequestParam("foto") MultipartFile foto) {
        try {
            // Validar que la foto no esté vacía
            if (foto == null || foto.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(MessageResponse.error("La foto de perfil es obligatoria"));
            }

            // Validar campos obligatorios
            if (nombre == null || nombre.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(MessageResponse.error("El nombre es obligatorio"));
            }
            if (email == null || email.trim().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(MessageResponse.error("El email es obligatorio"));
            }
            if (password == null || password.length() < 6) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(MessageResponse.error("La contraseña debe tener al menos 6 caracteres"));
            }

            // Crear el request
            RegisterRequest registerRequest = new RegisterRequest();
            registerRequest.setNombre(nombre.trim());
            registerRequest.setEmail(email.trim());
            registerRequest.setPassword(password);

            // Procesar y guardar la foto
            FileStorageService.FileInfo fileInfo = fileStorageService.storeProfilePhoto(foto);
            registerRequest.setFotoUrl(fileInfo.getFileUrl());
            registerRequest.setFotoNombre(fileInfo.getStoredFileName());

            // Registrar con rol ESTUDIANTE automático
            AuthResponse response = authService.registerStudent(registerRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(MessageResponse.error(e.getMessage()));
        }
    }

    /**
     * Actualizar foto de perfil del usuario autenticado
     */
    @PostMapping(value = "/update-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateProfilePhoto(@RequestParam("foto") MultipartFile foto) {
        try {
            if (foto == null || foto.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(MessageResponse.error("Debe proporcionar una foto"));
            }

            AuthResponse.UserInfo updatedUser = authService.updateProfilePhoto(foto);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(MessageResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest refreshTokenRequest) {
        try {
            AuthResponse response = authService.refreshToken(refreshTokenRequest);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MessageResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Usuario usuario = authService.getCurrentUser();
            AuthResponse.UserInfo userInfo = AuthResponse.UserInfo.builder()
                    .id(usuario.getIdUsuario())
                    .nombre(usuario.getNombre())
                    .email(usuario.getEmail())
                    .fotoUrl(usuario.getFotoUrl())
                    .tipoDocumento(usuario.getTipoDocumento())
                    .numeroDocumento(usuario.getNumeroDocumento())
                    .telefono(usuario.getTelefono())
                    .rol(usuario.getRol() != null ? usuario.getRol().getNombre() : null)
                    .build();
            return ResponseEntity.ok(userInfo);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(MessageResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // En JWT, el logout se maneja del lado del cliente eliminando el token
        // Aquí podríamos implementar una blacklist de tokens si fuera necesario
        return ResponseEntity.ok(MessageResponse.success("Sesión cerrada exitosamente"));
    }

    @PostMapping("/validate")
    public ResponseEntity<?> validateToken() {
        // Si llegamos aquí, el token es válido (ya pasó por el filtro JWT)
        return ResponseEntity.ok(MessageResponse.success("Token válido"));
    }

    /**
     * Actualizar perfil del usuario autenticado
     */
    @PutMapping("/update-profile")
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequest updateRequest) {
        try {
            AuthResponse.UserInfo updatedUser = authService.updateProfile(updateRequest);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(MessageResponse.error(e.getMessage()));
        }
    }

    /**
     * Cambiar contraseña del usuario autenticado
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@Valid @RequestBody ChangePasswordRequest changePasswordRequest) {
        try {
            authService.changePassword(changePasswordRequest);
            return ResponseEntity.ok(MessageResponse.success("Contraseña actualizada exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(MessageResponse.error(e.getMessage()));
        }
    }
}
