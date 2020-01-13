package com.roberttisma.tools.intermediate_song_importer.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseCache {
  private List<GetLyricsResponse> responses;
}
