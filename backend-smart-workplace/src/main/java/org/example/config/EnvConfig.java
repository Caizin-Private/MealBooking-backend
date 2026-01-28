package org.example.config;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
public class EnvConfig {

    @PostConstruct
    public void init() {
        Dotenv dotenv = Dotenv.configure()
                .directory(System.getProperty("user.dir"))
                .ignoreIfMissing()
                .load();

        setSystemProperty(dotenv, "AWS_ACCESS_KEY");
        setSystemProperty(dotenv, "AWS_SECRET_KEY");
        setSystemProperty(dotenv, "AWS_FROM_EMAIL");
        setSystemProperty(dotenv, "HR_EMAIL");
        setSystemProperty(dotenv, "DATABASE_URL");
        setSystemProperty(dotenv, "DATABASE_USERNAME");
        setSystemProperty(dotenv, "DATABASE_PASSWORD");
    }

    private void setSystemProperty(Dotenv dotenv, String key) {
        String value = dotenv.get(key);
        if (value != null) {
            System.setProperty(key, value);
        }
    }
}
