package uy.edu.ucu.scouting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ScoutingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScoutingApplication.class, args);
    }
}
