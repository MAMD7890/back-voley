package galacticos_app_back.galacticos.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@ConfigurationProperties(prefix = "wompi")
@EnableConfigurationProperties
@Data
public class WompiConfig {
    
    private String publicKey;
    private String privateKey;
    private String eventsSecret;
    private String integritySecret;
    private String apiUrl;
    private boolean sandbox;
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
