package com.gm.test;

import java.nio.CharBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gm.test.dto.ToolUsage;
import com.gm.test.service.TestService;

@SpringBootApplication
public class ThoughtWorkTestApp implements CommandLineRunner {
  private static final Logger LOGGER = LoggerFactory.getLogger(ThoughtWorkTestApp.class);
  @Autowired
  private TestService testService;
  @Autowired
  private ObjectMapper objectMapper;
  private static final List<String> KNOWN_NON_TOOLS = Arrays.asList("rope", "chocolate");

  public static void main(String[] args) {
    // ThoughtWorkTestApp.testApp();
    SpringApplication.run(ThoughtWorkTestApp.class, args);
  }

  @Override
  public void run(String... args) throws Exception {
    Optional<JsonNode> response = testService.getChallenge();
    if (!response.isPresent()) {
      ThoughtWorkTestApp.LOGGER.error("Failed to get valid response");
      return;
    }

    System.out.println("Challenge => " + response.get().toPrettyString());
    String stage = response.get().get("stage").asText();
    if (!StringUtils.hasText(stage) || stage.indexOf("/") == -1) {
      ThoughtWorkTestApp.LOGGER.error("Expected stage pattern is not observed.");
      return;
    }

    String[] stageTokens = stage.split("/");
    if (stageTokens.length != 2) {
      ThoughtWorkTestApp.LOGGER.warn("Invalid Stage response");
      return;
    }

    int currentStage = Integer.parseInt(stageTokens[0]);
    int totalStages = Integer.parseInt(stageTokens[1]);
    ArrayNode challengeArrayNode = objectMapper.createArrayNode();
    int start = currentStage;
    do {
      System.out.println("Executing Stage : " + start);
      Optional<JsonNode> challengeNode = processChallenge(start);
      if (challengeNode.isPresent() && challengeNode.get().has("output")) {
        challengeArrayNode.add(challengeNode.get());
        start++;
      }
    } while (start <= totalStages);
    System.out.println("Challenge Response : " + challengeArrayNode.toPrettyString());
  }

  private Optional<JsonNode> processChallenge(int challengeId) {
    ObjectNode challengeNode = objectMapper.createObjectNode();
    challengeNode.put("stage", challengeId);

    Optional<JsonNode> challengeInput = testService.getChallengeInput();
    if (!challengeInput.isPresent()) {
      ThoughtWorkTestApp.LOGGER.warn("No response received for challenge: {}", challengeId);
      return Optional.of(challengeNode);
    }

    challengeNode.set("input", challengeInput.get());

    ObjectNode outputJsonNode = null;
    switch (challengeId) {
      case 1:
        outputJsonNode = processEncryptionChallenge(challengeInput);
        break;

      case 2:
        outputJsonNode = processFindLaraToolsChallenge(challengeInput);
        break;

      case 3:
        outputJsonNode = processToolUsagesAnalysisChallenge(challengeInput);
        break;

      case 4:
        outputJsonNode = identifyValuableToolsChallenge(challengeInput);
        break;

      default:
        break;
    }

    if (null != outputJsonNode) {
      Optional<JsonNode> outputResponse = testService.postChallengeOutput(outputJsonNode.toString());
      if (!outputResponse.isPresent()) {
        ThoughtWorkTestApp.LOGGER.warn("No response received for post challenge");
        return Optional.of(challengeNode);
      }
      challengeNode.set("output", outputResponse.get());
    }
    return Optional.of(challengeNode);
  }

  public ObjectNode identifyValuableToolsChallenge(Optional<JsonNode> challengeInput) {
    ObjectNode outputJsonNode = objectMapper.createObjectNode();
    JsonNode challengeNode = challengeInput.get();
    JsonNode toolsNode = challengeNode.get("tools");
    int maximumWeight = challengeNode.get("maximumWeight").asInt();

    List<ToolUsage> valueableTools = new ArrayList<>();
    if (toolsNode.isArray()) {
      for (final JsonNode objNode : toolsNode) {
        try {
          valueableTools.add(objectMapper.readValue(objNode.toString(), ToolUsage.class));
        } catch (JsonProcessingException e) {
          ThoughtWorkTestApp.LOGGER.error("Failed to parse JSON", e);
          e.printStackTrace();
        }
      }
    }

    Collections.sort(valueableTools,
        (ToolUsage t1, ToolUsage t2) -> Long.compare(t2.getValue(), t1.getValue()));

    List<ToolUsage> allowedTools = new ArrayList<>(valueableTools.size());
    int totalWeight = 0;
    for (ToolUsage tool : valueableTools) {
      totalWeight = totalWeight + tool.getWeight();
      if (totalWeight <= maximumWeight) {
        allowedTools.add(tool);
      } else {
        totalWeight -= tool.getWeight();
      }
    }

    ArrayNode arrayNode = objectMapper.createArrayNode();
    allowedTools.stream().forEachOrdered(x -> arrayNode.add(x.getName()));
    outputJsonNode.set("toolsToTakeSorted", arrayNode);
    return outputJsonNode;
  }

  private ObjectNode processToolUsagesAnalysisChallenge(Optional<JsonNode> challengeInput) {
    ObjectNode outputJsonNode = objectMapper.createObjectNode();
    JsonNode challengeNode = challengeInput.get();
    JsonNode toolUsagesNode = challengeNode.get("toolUsage");
    List<ToolUsage> toolsUsagesArr = new ArrayList<>();
    if (toolUsagesNode.isArray()) {
      for (final JsonNode objNode : toolUsagesNode) {
        try {
          toolsUsagesArr.add(objectMapper.readValue(objNode.toString(), ToolUsage.class));
        } catch (JsonProcessingException e) {
          ThoughtWorkTestApp.LOGGER.error("Failed to parse JSON", e);
          e.printStackTrace();
        }
      }
    }

    Map<String, List<ToolUsage>> toolUsages =
        toolsUsagesArr.stream().collect(Collectors.groupingBy(ToolUsage::getName));
    Set<Entry<String, List<ToolUsage>>> entrySet = toolUsages.entrySet();
    List<ToolUsage> usagesData = new ArrayList<>(entrySet.size());
    for (Entry<String, List<ToolUsage>> entry : entrySet) {
      long totalUsages =
          entry.getValue().stream().map(x -> Duration.between(x.getUseStartTime(), x.getUseEndTime()).toMinutes())
              .collect(Collectors.summingLong(Long::longValue));
      ThoughtWorkTestApp.LOGGER.info("Usages of tool: {} - {} Minutes", entry.getKey(), totalUsages);
      usagesData.add(ToolUsage.valueOf(entry.getKey(), totalUsages));
    }

    Collections.sort(usagesData,
        (ToolUsage t1, ToolUsage t2) -> Long.compare(t2.getUsagesDuration(), t1.getUsagesDuration()));

    ArrayNode arrayNode = objectMapper.createArrayNode();
    usagesData.stream().map(x -> {
      ObjectNode individualToolUsage = objectMapper.createObjectNode();
      individualToolUsage.put("name", x.getName());
      individualToolUsage.put("timeUsedInMinutes", x.getUsagesDuration());
      return individualToolUsage;
    }).forEachOrdered(x -> arrayNode.add(x));
    outputJsonNode.set("toolsSortedOnUsage", arrayNode);
    return outputJsonNode;
  }

  private ObjectNode processFindLaraToolsChallenge(Optional<JsonNode> challengeInput) {
    ObjectNode outputJsonNode = objectMapper.createObjectNode();
    JsonNode challengeNode = challengeInput.get();
    String hiddenTools = challengeNode.get("hiddenTools").asText();
    JsonNode toolsJsonNode = challengeNode.get("tools");
    List<String> tools = new ArrayList<>();
    if (toolsJsonNode.isArray()) {
      for (final JsonNode objNode : toolsJsonNode) {
        tools.add(objNode.asText());
      }
    }
    List<Character> hiddenToolsArr = hiddenTools.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
    Map<Character, Long> characterFrequence =
        hiddenToolsArr.parallelStream().collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

    List<String> toolsIdentified =
        tools.stream().filter(this::removeKnownNonTools).filter(x -> findTool(x, characterFrequence))
            .collect(Collectors.toList());
    ArrayNode arrayNode = objectMapper.createArrayNode();
    toolsIdentified.stream().forEach(x -> arrayNode.add(x));
    outputJsonNode.set("toolsFound", arrayNode);
    ThoughtWorkTestApp.LOGGER.info("Tools identified so far: {}", toolsIdentified);
    return outputJsonNode;
  }

  private ObjectNode processEncryptionChallenge(Optional<JsonNode> challengeInput) {
    JsonNode challengeJson = challengeInput.get();
    String cipherTxt = challengeJson.get("encryptedMessage").asText();
    int decipherKey = challengeJson.get("key").asInt();

    String dicherText = CharBuffer.wrap(cipherTxt.toUpperCase().toCharArray())
        .chars().map(decipherText(decipherKey))
        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
        .toString();
    ObjectNode outputJsonNode = objectMapper.createObjectNode();
    outputJsonNode.put("message", dicherText);
    return outputJsonNode;
  }

  private IntUnaryOperator decipherText(int decipherKey) {
    return x -> {
      if (x >= 'A' && x <= 'Z') {
        int sub = x - decipherKey;
        if (sub < 'A') {
          sub = 'Z' - ('A' - sub - 1);
        }
        return sub;
      }
      return x;
    };
  }

  private boolean findTool(String tool, Map<Character, Long> characterFrequence) {
    char[] chars = tool.toCharArray();
    ThoughtWorkTestApp.LOGGER.info("Checking existence of tool: {}", tool);
    boolean itemFound = false;
    for (char c : chars) {
      Long currentVal = characterFrequence.get(c);
      if (null != currentVal && currentVal > 0) {
        characterFrequence.put(c, currentVal - 1);
        itemFound = true;
      } else {
        itemFound = false;
        ThoughtWorkTestApp.LOGGER.warn("Tool not found: {}", tool);
        break;
      }
    }
    return itemFound;
  }

  private boolean removeKnownNonTools(String tool) {
    boolean match =
        ThoughtWorkTestApp.KNOWN_NON_TOOLS.parallelStream().anyMatch(x -> !tool.equalsIgnoreCase(x.toLowerCase()));
    return match;
  }
}
