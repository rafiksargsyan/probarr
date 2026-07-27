package com.rsargsyan.probarr.main_ctx.core.domain.valueobject;

import java.util.List;

public record AddReleaseResult(boolean accepted, List<Release> replacedReleases) {

  public static final AddReleaseResult REJECTED = new AddReleaseResult(false, List.of());
}
