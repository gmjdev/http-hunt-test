package com.gm.test.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import com.gm.test.service.TestService;

@Configuration
public class AppConfig {
  @Value("${app.userId:#{null}}")
  private String defaultUser;
  @Value("${app.baseUrl:#{null}}")
  private String baseUrl;

  @Bean
  public WebClient webClient() {
    return WebClient.builder().baseUrl(baseUrl)
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE)
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.ALL_VALUE)
        .defaultHeader("userId", defaultUser)
        .build();
  }

  @Bean
  public RestTemplate restTemplate() {
    return new RestTemplate();
    // builder.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
    // builder.defaultHeader("userId", defaultUser);
    // return builder.build();
  }

  @Bean
  public TestService testService() {
    return new TestService();
  }
}
