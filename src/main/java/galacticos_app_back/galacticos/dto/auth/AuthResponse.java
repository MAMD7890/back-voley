package galacticos_app_back.galacticos.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;
    private UserInfo user;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Integer id;
        private String nombre;
        private String email;
        private String fotoUrl;
        private String tipoDocumento;
        private String numeroDocumento;
        private String telefono;
        private String rol;
        private MembresiaInfo membresia;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MembresiaInfo {
        private Integer id;
        private String estado;
        private LocalDate fechaInicio;
        private LocalDate fechaFin;
        private BigDecimal valorMensual;
        private String equipoNombre;
        private Integer equipoId;
    }
}
