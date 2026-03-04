package galacticos_app_back.galacticos.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import galacticos_app_back.galacticos.dto.auth.AuthResponse;
import galacticos_app_back.galacticos.dto.auth.LoginRequest;
import galacticos_app_back.galacticos.dto.auth.RefreshTokenRequest;
import galacticos_app_back.galacticos.dto.auth.RegisterRequest;
import galacticos_app_back.galacticos.entity.Rol;
import galacticos_app_back.galacticos.entity.Usuario;
import galacticos_app_back.galacticos.repository.RolRepository;
import galacticos_app_back.galacticos.repository.UsuarioRepository;
import galacticos_app_back.galacticos.security.JwtTokenProvider;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private FileStorageService fileStorageService;

    @Transactional
    public AuthResponse login(LoginRequest loginRequest) {
        try {
            System.out.println("🔐 ===== INICIANDO LOGIN =====");
            // Normalizar email a minúsculas para búsqueda consistente
            String emailNormalizado = loginRequest.getEmail() != null ? 
                loginRequest.getEmail().toLowerCase().trim() : "";
            System.out.println("📧 Email ingresado: " + loginRequest.getEmail());
            System.out.println("📧 Email normalizado: " + emailNormalizado);
            System.out.println("🔑 Password ingresado (longitud): " + (loginRequest.getPassword() != null ? loginRequest.getPassword().length() : "NULL"));
            String pwd = loginRequest.getPassword();
            System.out.println("🔑 Password ingresado (primeros 5 chars): " + (pwd != null && pwd.length() > 5 ? pwd.substring(0, 5) + "..." : "VERY_SHORT"));
            
            // Buscar usuario en BD con email normalizado
            Usuario usuarioEnBD = usuarioRepository.findByEmail(emailNormalizado).orElse(null);
            if (usuarioEnBD == null) {
                System.out.println("❌ Usuario NO encontrado en BD con email: " + emailNormalizado);
                throw new RuntimeException("Usuario no encontrado");
            }
            
            System.out.println("✅ Usuario encontrado en BD");
            System.out.println("   - ID: " + usuarioEnBD.getIdUsuario());
            System.out.println("   - Nombre: " + usuarioEnBD.getNombre());
            System.out.println("   - Email: " + usuarioEnBD.getEmail());
            System.out.println("   - Estado: " + usuarioEnBD.getEstado());
            System.out.println("   - Password en BD (hasheado, primeros 20 chars): " + (usuarioEnBD.getPassword() != null ? usuarioEnBD.getPassword().substring(0, Math.min(20, usuarioEnBD.getPassword().length())) : "NULL"));
            System.out.println("   - Rol: " + (usuarioEnBD.getRol() != null ? usuarioEnBD.getRol().getNombre() : "NULL"));
            
            // Intentar autenticación con email normalizado
            System.out.println("🔄 Intentando autenticación con AuthenticationManager...");
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            emailNormalizado,
                            loginRequest.getPassword()
                    )
            );

            System.out.println("✅ Autenticación exitosa!");
            SecurityContextHolder.getContext().setAuthentication(authentication);

            String accessToken = jwtTokenProvider.generateToken(emailNormalizado);
            String refreshToken = jwtTokenProvider.generateRefreshToken(emailNormalizado);

            System.out.println("✅ Tokens generados exitosamente");
            return buildAuthResponse(usuarioEnBD, accessToken, refreshToken);
            
        } catch (BadCredentialsException e) {
            System.out.println("❌ BadCredentialsException: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Credenciales inválidas");
        } catch (UsernameNotFoundException e) {
            System.out.println("❌ UsernameNotFoundException: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Usuario no encontrado: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("❌ Exception inesperada: " + e.getClass().getSimpleName() + " - " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error en autenticación: " + e.getMessage());
        }
    }

    @Transactional
    public AuthResponse register(RegisterRequest registerRequest) {
        // Verificar si el email ya existe
        if (usuarioRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new RuntimeException("El email ya está registrado");
        }

        // Crear nuevo usuario
        Usuario usuario = new Usuario();
        usuario.setNombre(registerRequest.getNombre());
        usuario.setEmail(registerRequest.getEmail());
        usuario.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        usuario.setFotoUrl(registerRequest.getFotoUrl());
        usuario.setFotoNombre(registerRequest.getFotoNombre());
        usuario.setEstado(true);

        // Asignar rol
        if (registerRequest.getIdRol() != null) {
            Optional<Rol> rol = rolRepository.findById(registerRequest.getIdRol());
            if (rol.isPresent()) {
                usuario.setRol(rol.get());
            } else {
                // Si el ID no existe, asignar rol USER por defecto
                Optional<Rol> defaultRol = rolRepository.findByNombre("USER");
                usuario.setRol(defaultRol.orElseThrow(() -> new RuntimeException("Rol USER no encontrado")));
            }
        } else {
            // Asignar rol por defecto (USER) si no se proporciona ID
            Optional<Rol> defaultRol = rolRepository.findByNombre("USER");
            usuario.setRol(defaultRol.orElseThrow(() -> new RuntimeException("Rol USER no encontrado")));
        }

        usuario = usuarioRepository.save(usuario);

        String accessToken = jwtTokenProvider.generateToken(usuario.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(usuario.getEmail());

        return buildAuthResponse(usuario, accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse registerStudent(RegisterRequest registerRequest) {
        // Verificar si el email ya existe
        if (usuarioRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new RuntimeException("El email ya está registrado");
        }

        // Crear nuevo usuario
        Usuario usuario = new Usuario();
        usuario.setNombre(registerRequest.getNombre());
        usuario.setEmail(registerRequest.getEmail());
        usuario.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        usuario.setFotoUrl(registerRequest.getFotoUrl());
        usuario.setFotoNombre(registerRequest.getFotoNombre());
        usuario.setEstado(true);

        // Asignar rol STUDENT automáticamente
        Rol studentRol = rolRepository.findByNombre("STUDENT")
                .orElseThrow(() -> new RuntimeException("Rol STUDENT no encontrado. Asegúrate de que la base de datos esté inicializada."));
        usuario.setRol(studentRol);

        usuario = usuarioRepository.save(usuario);

        String accessToken = jwtTokenProvider.generateToken(usuario.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(usuario.getEmail());

        return buildAuthResponse(usuario, accessToken, refreshToken);
    }

    @Transactional
    public AuthResponse registerProfesor(RegisterRequest registerRequest) {
        // Verificar si el email ya existe
        if (usuarioRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new RuntimeException("El email ya está registrado");
        }

        // Crear nuevo usuario
        Usuario usuario = new Usuario();
        usuario.setNombre(registerRequest.getNombre());
        usuario.setEmail(registerRequest.getEmail());
        usuario.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        usuario.setFotoUrl(registerRequest.getFotoUrl());
        usuario.setFotoNombre(registerRequest.getFotoNombre());
        usuario.setEstado(true);

        // Asignar rol PROFESOR automáticamente
        Rol profesorRol = rolRepository.findByNombre("PROFESOR")
                .orElseThrow(() -> new RuntimeException("Rol PROFESOR no encontrado. Asegúrate de que la base de datos esté inicializada."));
        usuario.setRol(profesorRol);

        usuario = usuarioRepository.save(usuario);

        String accessToken = jwtTokenProvider.generateToken(usuario.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(usuario.getEmail());

        return buildAuthResponse(usuario, accessToken, refreshToken);
    }

    public AuthResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new RuntimeException("Refresh token inválido o expirado");
        }

        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new RuntimeException("El token proporcionado no es un refresh token");
        }

        String email = jwtTokenProvider.extractUsername(refreshToken);
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!usuario.getEstado()) {
            throw new RuntimeException("Usuario desactivado");
        }

        String newAccessToken = jwtTokenProvider.generateToken(email);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(email);

        return buildAuthResponse(usuario, newAccessToken, newRefreshToken);
    }

    public Usuario getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No hay usuario autenticado");
        }

        String email = authentication.getName();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    @Transactional
    public AuthResponse.UserInfo updateProfilePhoto(MultipartFile foto) {
        Usuario usuario = getCurrentUser();

        // Eliminar foto anterior si existe
        if (usuario.getFotoNombre() != null && !usuario.getFotoNombre().isEmpty()) {
            fileStorageService.deleteProfilePhoto(usuario.getFotoNombre());
        }

        // Guardar nueva foto
        FileStorageService.FileInfo fileInfo = fileStorageService.storeProfilePhoto(foto);
        usuario.setFotoUrl(fileInfo.getFileUrl());
        usuario.setFotoNombre(fileInfo.getStoredFileName());

        usuarioRepository.save(usuario);

        return AuthResponse.UserInfo.builder()
                .id(usuario.getIdUsuario())
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .fotoUrl(usuario.getFotoUrl())
                .tipoDocumento(usuario.getTipoDocumento())
                .numeroDocumento(usuario.getNumeroDocumento())
                .telefono(usuario.getTelefono())
                .rol(usuario.getRol() != null ? usuario.getRol().getNombre() : null)
                .build();
    }

    @Transactional
    public AuthResponse.UserInfo updateProfile(galacticos_app_back.galacticos.dto.auth.UpdateProfileRequest updateRequest) {
        Usuario usuario = getCurrentUser();
        
        // Verificar si el nuevo email ya está en uso por otro usuario
        if (!usuario.getEmail().equals(updateRequest.getEmail())) {
            if (usuarioRepository.findByEmail(updateRequest.getEmail()).isPresent()) {
                throw new RuntimeException("El email ya está registrado por otro usuario");
            }
        }
        
        // Actualizar campos
        usuario.setNombre(updateRequest.getNombre());
        usuario.setEmail(updateRequest.getEmail());
        usuario.setTipoDocumento(updateRequest.getTipoDocumento());
        usuario.setNumeroDocumento(updateRequest.getNumeroDocumento());
        usuario.setTelefono(updateRequest.getTelefono());
        
        usuarioRepository.save(usuario);
        
        return AuthResponse.UserInfo.builder()
                .id(usuario.getIdUsuario())
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .fotoUrl(usuario.getFotoUrl())
                .tipoDocumento(usuario.getTipoDocumento())
                .numeroDocumento(usuario.getNumeroDocumento())
                .telefono(usuario.getTelefono())
                .rol(usuario.getRol() != null ? usuario.getRol().getNombre() : null)
                .build();
    }

    @Transactional
    public void changePassword(galacticos_app_back.galacticos.dto.auth.ChangePasswordRequest changePasswordRequest) {
        Usuario usuario = getCurrentUser();
        
        // Verificar que la contraseña actual sea correcta
        if (!passwordEncoder.matches(changePasswordRequest.getCurrentPassword(), usuario.getPassword())) {
            throw new RuntimeException("La contraseña actual es incorrecta");
        }
        
        // Actualizar la contraseña
        usuario.setPassword(passwordEncoder.encode(changePasswordRequest.getNewPassword()));
        usuarioRepository.save(usuario);
    }

    private AuthResponse buildAuthResponse(Usuario usuario, String accessToken, String refreshToken) {
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

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtTokenProvider.getExpirationTime())
                .user(userInfo)
                .build();
    }
}
