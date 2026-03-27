package com.rsargsyan.probarr.main_ctx.core.domain.aggregate;

import com.rsargsyan.probarr.main_ctx.core.domain.localentity.AudioTrack;
import com.rsargsyan.probarr.main_ctx.core.domain.localentity.SubtitleTrack;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.Resolution;
import com.rsargsyan.probarr.main_ctx.core.domain.valueobject.RipType;
import jakarta.persistence.*;
import lombok.Getter;

import java.util.List;

@Entity
public class Release extends AggregateRoot {

  @Getter
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "movie_id", nullable = false)
  private Movie movie;

  @Getter
  @Column(nullable = false, unique = true)
  private String infoHash;

  @Getter
  @Column(nullable = false)
  private String filePath;

  @Getter
  @Column(nullable = false)
  private Long fileSizeBytes;

  @Getter
  @Column(nullable = false)
  private String videoCodec;

  @Getter
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Resolution resolution;

  @Getter
  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private RipType ripType;

  @Getter
  @Column(nullable = false)
  private Integer runtimeSeconds;

  @Getter
  @ElementCollection
  @CollectionTable(name = "release_audio_track", joinColumns = @JoinColumn(name = "release_id"))
  private List<AudioTrack> audioTracks;

  @Getter
  @ElementCollection
  @CollectionTable(name = "release_subtitle_track", joinColumns = @JoinColumn(name = "release_id"))
  private List<SubtitleTrack> subtitleTracks;

  @SuppressWarnings("unused")
  Release() {}

  public Release(Movie movie, String infoHash, String filePath, Long fileSizeBytes,
                 String videoCodec, Resolution resolution, RipType ripType,
                 Integer runtimeSeconds, List<AudioTrack> audioTracks,
                 List<SubtitleTrack> subtitleTracks) {
    this.movie = movie;
    this.infoHash = infoHash;
    this.filePath = filePath;
    this.fileSizeBytes = fileSizeBytes;
    this.videoCodec = videoCodec;
    this.resolution = resolution;
    this.ripType = ripType;
    this.runtimeSeconds = runtimeSeconds;
    this.audioTracks = audioTracks;
    this.subtitleTracks = subtitleTracks;
  }
}
