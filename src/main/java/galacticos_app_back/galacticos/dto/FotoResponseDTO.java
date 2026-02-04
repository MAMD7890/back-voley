package galacticos_app_back.galacticos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FotoResponseDTO {
    private String message;
    private String fotoUrl;
    private String fotoNombre;
    
    public static FotoResponseDTO success(String message, String fotoUrl, String fotoNombre) {
        return FotoResponseDTO.builder()
                .message(message)
                .fotoUrl(fotoUrl)
                .fotoNombre(fotoNombre)
                .build();
    }
    
    public static FotoResponseDTO deleted(String message) {
        return FotoResponseDTO.builder()
                .message(message)
                .fotoUrl(null)
                .fotoNombre(null)
                .build();
    }
}
