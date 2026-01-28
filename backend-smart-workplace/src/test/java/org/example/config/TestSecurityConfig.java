package org.example.config;

import org.example.service.AzureOAuth2UserService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

import static org.mockito.Mockito.mock;

@TestConfiguration
@Profile("test")
public class TestSecurityConfig {

    @Bean
    @Primary
    SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .oauth2ResourceServer(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .securityContext(AbstractHttpConfigurer::disable)
                .sessionManagement(AbstractHttpConfigurer::disable);
        return http.build();
    }

    @Bean
    @Primary
    public MealBookingConfig testMealBookingConfig() {
        MealBookingConfig config = new MealBookingConfig();
        config.setCutoffTime(java.time.LocalTime.of(22, 0));
        return config;
    }

    @Bean
    @Primary
    public AzureOAuth2UserService mockAzureOAuth2UserService() {
        return mock(AzureOAuth2UserService.class);
    }

    @Bean
    @Primary
    public AzureOAuth2UserFilter mockAzureOAuth2UserFilter() {
        return mock(AzureOAuth2UserFilter.class);
    }
}