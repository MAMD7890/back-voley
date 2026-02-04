package galacticos_app_back.galacticos.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;

@Service
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class FileStorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    private Path fileStorageLocation;

    @PostConstruct
    public void init() {
        this.fileStorageLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.fileStorageLocation);
            // Crear subdirectorio para fotos de perfil
            Files.createDirectories(this.fileStorageLocation.resolve("profiles"));
            // Crear subdirectorio para fotos de estudiantes
            Files.createDirectories(this.fileStorageLocation.resolve("estudiantes"));
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo crear el directorio de uploads", ex);
        }
    }

    public FileInfo storeFile(MultipartFile file, String subDirectory) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // Validar tipo de archivo
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new RuntimeException("Solo se permiten archivos de imagen");
        }

        // Limpiar nombre del archivo
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        
        // Validar nombre
        if (originalFileName.contains("..")) {
            throw new RuntimeException("Nombre de archivo inválido: " + originalFileName);
        }

        // Generar nombre único
        String fileExtension = getFileExtension(originalFileName);
        String newFileName = UUID.randomUUID().toString() + fileExtension;

        try {
            Path targetLocation = this.fileStorageLocation.resolve(subDirectory).resolve(newFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Retornar información del archivo
            String fileUrl = "/uploads/" + subDirectory + "/" + newFileName;
            return new FileInfo(newFileName, originalFileName, fileUrl);

        } catch (IOException ex) {
            throw new RuntimeException("No se pudo almacenar el archivo " + originalFileName, ex);
        }
    }

    public FileInfo storeProfilePhoto(MultipartFile file) {
        return storeFile(file, "profiles");
    }

    public FileInfo storeEstudiantePhoto(MultipartFile file) {
        return storeFile(file, "estudiantes");
    }

    public void deleteFile(String fileName, String subDirectory) {
        if (fileName == null || fileName.isEmpty()) {
            return;
        }
        try {
            Path filePath = this.fileStorageLocation.resolve(subDirectory).resolve(fileName);
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new RuntimeException("No se pudo eliminar el archivo " + fileName, ex);
        }
    }

    public void deleteProfilePhoto(String fileName) {
        deleteFile(fileName, "profiles");
    }

    public void deleteEstudiantePhoto(String fileName) {
        deleteFile(fileName, "estudiantes");
    }

    /**
     * Valida si el archivo es una imagen válida (JPG, PNG, WEBP)
     */
    public boolean isValidImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }
        return contentType.equals("image/jpeg") || 
               contentType.equals("image/png") || 
               contentType.equals("image/webp");
    }

    /**
     * Obtiene la extensión válida de la imagen basada en el content type
     */
    public String getValidImageExtension(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) return null;
        
        return switch (contentType) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> null;
        };
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf(".") == -1) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf("."));
    }

    // Clase interna para retornar información del archivo
    public static class FileInfo {
        private final String storedFileName;
        private final String originalFileName;
        private final String fileUrl;

        public FileInfo(String storedFileName, String originalFileName, String fileUrl) {
            this.storedFileName = storedFileName;
            this.originalFileName = originalFileName;
            this.fileUrl = fileUrl;
        }

        public String getStoredFileName() {
            return storedFileName;
        }

        public String getOriginalFileName() {
            return originalFileName;
        }

        public String getFileUrl() {
            return fileUrl;
        }
    }
}
