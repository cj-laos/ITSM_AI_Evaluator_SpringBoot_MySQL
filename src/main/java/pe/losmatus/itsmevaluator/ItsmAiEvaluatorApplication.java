package pe.losmatus.itsmevaluator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
@ConfigurationPropertiesScan
public class ItsmAiEvaluatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(ItsmAiEvaluatorApplication.class, args);
    }
}
