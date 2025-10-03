package cola.springboot.cocal

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@SpringBootApplication
@EnableJpaAuditing
class CocalApplication {

    static void main(String[] args) {
        SpringApplication.run(CocalApplication, args)
    }

}
