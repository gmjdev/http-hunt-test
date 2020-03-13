package com.gm.test.service;

import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestService {
  private static final Logger LOGGER = LoggerFactory.getLogger(TestService.class);
  @Autowired
  private WebClient webClient;
  @Autowired
  private ObjectMapper objectMapper;
  @Value("${app.baseUrl:#{null}}")
  private String baseUrl;
  @Value("${app.userId:#{null}}")
  private String userId;

  public Optional<JsonNode> getChallenge() {
    try {
      String response = webClient.get().uri("/challenge").retrieve().bodyToMono(String.class).block();
      return Optional.of(objectMapper.readTree(response));
    } catch (JsonProcessingException e) {
      TestService.LOGGER.error("Failed to parse JSON", e);
    } catch (WebClientException e) {
      TestService.LOGGER.error("Failed to get valid response", e);
    }
    return Optional.empty();
  }

  public Optional<JsonNode> getChallengeInput() {
    try {
      String response =
          webClient.get().uri("/challenge/input").retrieve().bodyToMono(String.class).block();
      return Optional.of(objectMapper.readTree(response));
    } catch (JsonProcessingException e) {
      TestService.LOGGER.error("Failed to parse JSON", e);
    } catch (WebClientException e) {
      TestService.LOGGER.error("Failed to get valid response", e);
    }
    return Optional.empty();
  }

  public Optional<JsonNode> postChallengeOutput(String jsonObj) {
    try {
      JsonNode jsonNode = objectMapper.readTree(jsonObj);
      String responseBody =
          webClient.post().uri("/challenge/output")
              .body(BodyInserters.fromValue(jsonNode))
              .exchange()
              .block()
              .bodyToMono(String.class).block();
      String responseTxt = objectMapper.readTree(responseBody).get("message").asText();
      if (!"Wrong answer!! Try again!".equalsIgnoreCase(responseTxt) &&
          !"Timeout, the response need to come back in less than 2 seconds".equalsIgnoreCase(responseTxt)) {
        return Optional.of(objectMapper.readTree(responseBody));
      }
      TestService.LOGGER.warn("Received invalid response: {}", responseTxt);
    } catch (JsonProcessingException e) {
      TestService.LOGGER.error("Failed to parse JSON", e);
    } catch (WebClientException e) {
      TestService.LOGGER.error("Failed to get valid response", e);
    }
    return Optional.empty();
  }
}
