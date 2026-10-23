package io.observatory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ObservatoryApplication {
 public static void main(String[] args) { SpringApplication.run(ObservatoryApplication.class,args); }
}
