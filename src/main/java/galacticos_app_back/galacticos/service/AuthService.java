package galacticos_app_back.galacticos.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);

            Usuario usuario = usuarioRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            String accessToken = jwtTokenProvider.generateToken(loginRequest.getEmail());
            String refreshToken = jwtTokenProvider.generateRefreshToken(loginRequest.getEmail());

            return buildAuthResponse(usuario, accessToken, refreshToken);
            
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Credenciales inválidas");
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
