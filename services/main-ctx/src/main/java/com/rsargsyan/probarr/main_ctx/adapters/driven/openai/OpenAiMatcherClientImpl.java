package com.rsargsyan.probarr.main_ctx.adapters.driven.openai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rsargsyan.probarr.main_ctx.Config;
import com.rsargsyan.probarr.main_ctx.core.ports.client.AnimeMatcherClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
public class OpenAiMatcherClientImpl implements AnimeMatcherClient {

  private static final String MODEL = "gpt-5-nano";

  private final RestClient restClient;
  private final ObjectMapper objectMapper;
  private final boolean enabled;

  @Autowired
  public OpenAiMatcherClientImpl(Config config, ObjectMapper objectMapper) {
    this.enabled = config.openaiApiKey != null && !config.openaiApiKey.isBlank();
    this.objectMapper = objectMapper;
    this.restClient = enabled
        ? RestClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .defaultHeader("Authorization", "Bearer " + config.openaiApiKey)
            .build()
        : null;
  }

  private static final Map<String, Object> SEASON_MATCH_SCHEMA = Map.of(
      "type", "object",
      "properties", Map.of(
          "seasonNumber", Map.of("type", "integer"),
          "fullSeason", Map.of("type", "boolean"),
          "episodeNumbers", Map.of("type", "array", "items", Map.of("type", "integer"))
      ),
      "required", List.of("seasonNumber", "fullSeason", "episodeNumbers"),
      "additionalProperties", false
  );

  private static final Map<String, Object> MATCH_RESULT_SCHEMA = Map.of(
      "type", "object",
      "properties", Map.of("seasons", Map.of("type", "array", "items", SEASON_MATCH_SCHEMA)),
      "required", List.of("seasons"),
      "additionalProperties", false
  );

  private static final Map<String, Object> FILE_MATCH_SCHEMA = Map.of(
      "type", "object",
      "properties", Map.of("fileIndex", Map.of("type", List.of("integer", "null"))),
      "required", List.of("fileIndex"),
      "additionalProperties", false
  );

  record LlmSeasonMatch(int seasonNumber, boolean fullSeason, List<Integer> episodeNumbers) {}
  record LlmMatchResult(List<LlmSeasonMatch> seasons) {}
  record LlmFileMatch(Integer fileIndex) {}

  record ChatMessage(String role, String content) {}

  record JsonSchemaWrapper(String name, boolean strict, Map<String, Object> schema) {}

  record ResponseFormat(String type, @JsonProperty("json_schema") JsonSchemaWrapper jsonSchema) {}

  record ChatCompletionRequest(
      String model,
      List<ChatMessage> messages,
      @JsonProperty("response_format") ResponseFormat responseFormat,
      @JsonProperty("reasoning_effort") String reasoningEffort
  ) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ChatCompletionResponse(List<Choice> choices) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Choice(ChoiceMessage message) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record ChoiceMessage(String content) {}

  private <T> T complete(String prompt, String schemaName, Map<String, Object> schema, Class<T> resultType) {
    ChatCompletionRequest request = new ChatCompletionRequest(
        MODEL,
        List.of(new ChatMessage("user", prompt)),
        new ResponseFormat("json_schema", new JsonSchemaWrapper(schemaName, true, schema)),
        "minimal"
    );
    ChatCompletionResponse response = restClient.post()
        .uri("/chat/completions")
        .body(request)
        .retrieve()
        .body(ChatCompletionResponse.class);
    String content = response != null && !response.choices().isEmpty()
        ? response.choices().get(0).message().content()
        : null;
    if (content == null) return null;
    try {
      return objectMapper.readValue(content, resultType);
    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
      throw new IllegalStateException("Failed to parse OpenAI response: " + content, e);
    }
  }

  @Override
  public List<SeasonMatch> matchRelease(String releaseTitle, List<String> showNames, List<SeasonSummary> seasons) {
    if (!enabled) return List.of();

    String seasonStructure = seasons.stream()
        .map(s -> String.format("Season %d: %d episodes", s.seasonNumber(), s.episodeCount()))
        .collect(Collectors.joining("\n"));

    String prompt = """
        Release title: %s

        Show names/aliases: %s

        Known season structure:
        %s

        This is an anime release. Anime fansub groups commonly prefix the release with a group \
        tag in brackets (e.g. "[SubsPlease]"), and number episodes by an absolute count across \
        the whole series rather than per-season — e.g. for a show with a 24-episode season 1, \
        absolute episode 25 is season 2 episode 1. Batches often cover an absolute range \
        (e.g. "25-47") or a whole season.

        Determine which season(s) and episode(s) of THIS show this release covers, translating \
        any absolute numbering into season-relative episode numbers using the season structure \
        above. If the release is for a different show, or is a non-episode extra (NCOP/NCED, \
        specials, interviews, etc.) rather than a numbered episode, return no season matches.
        """.formatted(releaseTitle, String.join(", ", showNames), seasonStructure);

    log.info("OpenAiMatcherClient.matchRelease: calling LLM for '{}'", releaseTitle);
    long startedAt = System.nanoTime();
    try {
      LlmMatchResult result = complete(prompt, "match_result", MATCH_RESULT_SCHEMA, LlmMatchResult.class);
      long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
      log.info("OpenAiMatcherClient.matchRelease: got response for '{}' in {}ms: {}", releaseTitle, elapsedMs, result);

      if (result == null || result.seasons() == null) return List.of();
      return result.seasons().stream()
          .map(s -> new SeasonMatch(s.seasonNumber(), s.fullSeason(),
              s.episodeNumbers() != null ? s.episodeNumbers() : List.of()))
          .toList();
    } catch (Exception e) {
      long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
      log.warn("OpenAiMatcherClient.matchRelease failed for '{}' after {}ms: {}", releaseTitle, elapsedMs, e.getMessage());
      return List.of();
    }
  }

  @Override
  public Integer locateEpisodeFile(List<FileCandidate> files, List<String> showNames,
                                   Integer seasonNumber, Integer episodeNumber, Integer absoluteNumber) {
    if (!enabled) return null;

    String fileList = files.stream()
        .map(f -> String.format("index=%d name=%s size=%d bytes", f.index(), f.name(), f.sizeBytes()))
        .collect(Collectors.joining("\n"));

    String prompt = """
        Show names/aliases: %s

        Target episode: season=%s episode=%s absolute=%s

        Files in this torrent:
        %s

        This is an anime release. Pick the file index that is exactly the target episode — not an \
        NCOP/NCED, special, OVA, or any other extra. Episode numbers in filenames may be absolute \
        (across the whole series) or season-relative - the target episode's season-relative number \
        and absolute number can both appear as plausible matches for different files, so look at \
        the full set of filenames (how many there are, their numbering range) to work out which \
        convention this release actually uses before deciding. If no file confidently matches, \
        return null.
        """.formatted(String.join(", ", showNames), seasonNumber, episodeNumber, absoluteNumber, fileList);

    log.info("OpenAiMatcherClient.locateEpisodeFile: calling LLM for season={} episode={} absolute={} ({} file(s))",
        seasonNumber, episodeNumber, absoluteNumber, files.size());
    long startedAt = System.nanoTime();
    try {
      LlmFileMatch result = complete(prompt, "file_match", FILE_MATCH_SCHEMA, LlmFileMatch.class);
      long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
      log.info("OpenAiMatcherClient.locateEpisodeFile: got response in {}ms: {}", elapsedMs, result);

      Integer fileIndex = result != null ? result.fileIndex() : null;
      if (fileIndex == null) return null;
      boolean exists = files.stream().anyMatch(f -> f.index() == fileIndex);
      return exists ? fileIndex : null;
    } catch (Exception e) {
      long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
      log.warn("OpenAiMatcherClient.locateEpisodeFile failed after {}ms: {}", elapsedMs, e.getMessage());
      return null;
    }
  }
}
