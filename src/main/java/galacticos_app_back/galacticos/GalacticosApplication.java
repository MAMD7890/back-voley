package galacticos_app_back.galacticos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GalacticosApplication {

	public static void main(String[] args) {
		SpringApplication.run(GalacticosApplication.class, args);
	}

}
