package org.example.config;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ses.SesClient;

@Configuration
@ConfigurationProperties(prefix = "aws.ses")
@Data
@Slf4j
public class AwsSesConfig {

    private String region;
    private String accessKey;
    private String secretKey;

    @Bean
    public SesClient sesClient() {
        log.info("Initializing AWS SES client for region {}", region);

        AwsBasicCredentials credentials =
                AwsBasicCredentials.create(accessKey, secretKey);

        return SesClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(credentials))
                .build();
    }
}
