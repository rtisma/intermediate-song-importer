package com.roberttisma.tools.intermediate_song_importer.service;

import static com.roberttisma.tools.intermediate_song_importer.Factory.createRetry;
import static com.roberttisma.tools.intermediate_song_importer.exceptions.ImporterException.buildImporterException;
import static com.roberttisma.tools.intermediate_song_importer.exceptions.ImporterException.checkImporter;
import static com.roberttisma.tools.intermediate_song_importer.util.FileIO.readFileContent;
import static com.roberttisma.tools.intermediate_song_importer.util.JsonUtils.mapper;
import static com.roberttisma.tools.intermediate_song_importer.util.RestClient.get;
import static com.roberttisma.tools.intermediate_song_importer.util.RestClient.post;
import static java.lang.String.format;
import static net.jodah.failsafe.Failsafe.with;

import bio.overture.song.core.model.Analysis;
import bio.overture.song.core.model.FileDTO;
import bio.overture.song.sdk.SongApi;
import com.roberttisma.tools.intermediate_song_importer.model.SongConfig;
import com.roberttisma.tools.intermediate_song_importer.model.Study;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import kong.unirest.HttpResponse;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;

@Slf4j
@Builder
@RequiredArgsConstructor
public class TargetSongService {

  @NonNull private SongApi api;
  @NonNull private SongConfig config;

  public String getTargetAnalysisState(@NonNull String studyId, @NonNull String analysisId) {
    return api.getAnalysis(studyId, analysisId).getAnalysisState().toString();
  }

  public List<FileDTO> getTargetAnalysisFiles(@NonNull String studyId, @NonNull String analysisId) {
    return api.getAnalysisFiles(studyId, analysisId);
  }

  public Analysis submitTargetPayload(@NonNull Path payloadFile) throws IOException {
    val targetPayload = readFileContent(payloadFile);
    val targetStudyId = extractStudyId(targetPayload);
    checkImporter(
        isStudyExist(targetStudyId), "The target studyId '%s' does not exist", targetStudyId);

    val targetAnalysisId =
        with(createRetry(String.class))
            .get(() -> api.submit(targetStudyId, targetPayload).getAnalysisId());
    return api.getAnalysis(targetStudyId, targetAnalysisId);
  }

  // Create the study if it does not exist
  public void saveStudy(@NonNull String targetStudyId) {
    if (!isStudyExist(targetStudyId)) {
      createStudy(targetStudyId);
      log.info("Created target studyId '{}'", targetStudyId);
    }
  }

  public void publishTargetAnalysis(@NonNull Analysis a) {
    with(createRetry(Object.class))
        .run(() -> api.publish(a.getStudyId(), a.getAnalysisId(), false));
  }

  private boolean isStudyExist(@NonNull String studyId) {
    val response = get(getIsStudyExistUrl(studyId));
    return handleNotFound(
            response,
            "Error getting the studyId '%s' for host '%s': %s",
            studyId,
            config.getServerUrl(),
            response)
        .isPresent();
  }

  private void createStudy(@NonNull String studyId) {
    val body = Study.builder().studyId(studyId).build();
    val response = post(config.getAccessToken(), getCreateStudyUrl(studyId), body);
    checkImporter(
        response.isSuccess(),
        "Error creating studyId '%s': %s -> %s",
        studyId,
        response.getStatusText(),
        response.getBody());
  }

  private String getIsStudyExistUrl(String studyId) {
    return format("%s/studies/%s", config.getServerUrl(), studyId);
  }

  private String getCreateStudyUrl(String studyId) {
    return format("%s/studies/%s/", config.getServerUrl(), studyId);
  }

  private static <T> Optional<T> handleNotFound(
      HttpResponse<T> response, String errorFormattedString, Object... args) {
    if (isNotFound(response.getStatus())) {
      return Optional.empty();
    } else if (!isError(response.getStatus())) {
      return Optional.of(response.getBody());
    } else {
      throw buildImporterException(errorFormattedString, args);
    }
  }

  private static boolean isNotFound(int statusCode) {
    return statusCode == 404;
  }

  private static boolean isError(int statusCode) {
    return statusCode >= 400;
  }

  @SneakyThrows
  private static String extractStudyId(String payload) {
    val j = mapper().readTree(payload);
    checkImporter(j.has("studyId"), "json missing the studyId field");
    return j.path("studyId").asText();
  }
}