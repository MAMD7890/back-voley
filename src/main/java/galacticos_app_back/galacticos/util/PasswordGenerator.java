package galacticos_app_back.galacticos.util;

import java.security.SecureRandom;

/**
 * Utilidad para generar contraseñas temporales seguras
 */
public class PasswordGenerator {
    
    private static final String MAYUSCULAS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String MINUSCULAS = "abcdefghijklmnopqrstuvwxyz";
    private static final String NUMEROS = "0123456789";
    private static final String SIMBOLOS = "!@#$%^&*_-+=";
    
    private static final SecureRandom random = new SecureRandom();
    
    /**
     * Genera una contraseña temporal PREDECIBLE para importaciones
     * Formato: TempPass123! (para que el usuario pueda recordarla)
     * @return Contraseña temporal predecible
     */
    public static String generateTemporaryPasswordForImport() {
        // Contraseña temporal predecible para estudiantes importados
        return "TempPass123!";
    }

    /**
     * Genera una contraseña temporal de 12 caracteres
     * Contiene: mayúsculas, minúsculas, números y símbolos
     * 
     * @return Contraseña generada
     */
    public static String generateTemporaryPassword() {
        StringBuilder password = new StringBuilder(12);
        
        // Agregar al menos uno de cada tipo
        password.append(MAYUSCULAS.charAt(random.nextInt(MAYUSCULAS.length())));
        password.append(MINUSCULAS.charAt(random.nextInt(MINUSCULAS.length())));
        password.append(NUMEROS.charAt(random.nextInt(NUMEROS.length())));
        password.append(SIMBOLOS.charAt(random.nextInt(SIMBOLOS.length())));
        
        // Llenar el resto aleatoriamente
        String allChars = MAYUSCULAS + MINUSCULAS + NUMEROS + SIMBOLOS;
        for (int i = 4; i < 12; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }
        
        // Mezclar
        return shuffleString(password.toString());
    }
    
    /**
     * Genera un username en formato: {nombre.apellido}.{id}
     * Ejemplo: juan.perez.450
     * 
     * @param nombreCompleto Nombre completo del estudiante
     * @param estudianteId ID del estudiante
     * @return Username generado
     */
    public static String generateUsername(String nombreCompleto, Integer estudianteId) {
        if (nombreCompleto == null || nombreCompleto.trim().isEmpty()) {
            return "usuario." + estudianteId;
        }
        
        String[] partes = nombreCompleto.trim().toLowerCase().split("\\s+");
        StringBuilder username = new StringBuilder();
        
        if (partes.length >= 2) {
            // Nombre.Apellido.ID
            username.append(partes[0])
                    .append(".")
                    .append(partes[1])
                    .append(".")
                    .append(estudianteId);
        } else {
            // Solo nombre.ID
            username.append(partes[0])
                    .append(".")
                    .append(estudianteId);
        }
        
        // Remover caracteres especiales y limitar a 30 caracteres
        String cleaned = username.toString()
                .replaceAll("[^a-z0-9.]", "");
        
        // Usar la longitud de la cadena limpia, no la original
        if (cleaned.length() > 30) {
            return cleaned.substring(0, 30);
        }
        return cleaned;
    }
    
    /**
     * Mezcla una cadena aleatoriamente
     */
    private static String shuffleString(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }
}
