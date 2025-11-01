package cola.springboot.cocal

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
class CocalApplication {

    static void main(String[] args) {
        SpringApplication.run(CocalApplication, args)
    }

}
