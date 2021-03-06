package com.roberttisma.tools.intermediate_song_importer.util;

import static com.roberttisma.tools.intermediate_song_importer.exceptions.ImporterException.checkImporter;
import static com.roberttisma.tools.intermediate_song_importer.util.FileIO.checkFileExists;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.val;

public class JsonUtils {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public static class PrettyJsonPrinter extends DefaultPrettyPrinter {
    public static final PrettyJsonPrinter INSTANCE = new PrettyJsonPrinter(4);

    public PrettyJsonPrinter(int indentSize) {
      val sb = new StringBuilder();
      for (int i = 0; i < indentSize; i++) {
        sb.append(' ');
      }
      indentArraysWith(new DefaultIndenter(sb.toString(), DefaultIndenter.SYS_LF));
    }
  }

  public static ObjectMapper mapper() {
    return OBJECT_MAPPER;
  }

  @SneakyThrows
  public static JsonNode readTree(@NonNull Path file) {
    checkFileExists(file);
    return OBJECT_MAPPER.readTree(file.toFile());
  }

  @SneakyThrows
  public static String toPrettyJson(@NonNull Object o) {
    return mapper().writerWithDefaultPrettyPrinter().writeValueAsString(o);
  }

  public static void checkRequiredField(JsonNode j, String field) {
    checkImporter(j.has(field), "Could not find field '%s' in %s", field, j.toString());
  }

  @SneakyThrows
  public static <T> T readValue(Path file, Class<T> klass) {
    checkFileExists(file);
    return OBJECT_MAPPER.readValue(file.toFile(), klass);
  }
}
