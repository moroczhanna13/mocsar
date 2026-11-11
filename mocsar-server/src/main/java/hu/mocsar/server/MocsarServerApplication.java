package hu.mocsar.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MocsarServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MocsarServerApplication.class, args);
    }
}
