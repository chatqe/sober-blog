package io.charqe.qs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
@Slf4j
public class QsBlogApplication {

    public static void main(String[] args) {
        SpringApplication.run(QsBlogApplication.class, args);

        log.info("\n QsBlogApplication start ٩(๑❛ᴗ❛๑)۶ \n");
    }

}


