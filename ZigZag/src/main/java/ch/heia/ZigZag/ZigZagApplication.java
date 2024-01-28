package ch.heia.ZigZag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.web.multipart.MultipartFile;

@SpringBootApplication
public class ZigZagApplication {
	public static void main(String[] args) {
		SpringApplication.run(ZigZagApplication.class, args);
	}
}

