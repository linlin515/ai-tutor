package com.aitutor.app.domain.usecase;

import com.aitutor.app.domain.repository.AnalyticsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class GetLearningStatsUseCase_Factory implements Factory<GetLearningStatsUseCase> {
  private final Provider<AnalyticsRepository> analyticsRepositoryProvider;

  public GetLearningStatsUseCase_Factory(
      Provider<AnalyticsRepository> analyticsRepositoryProvider) {
    this.analyticsRepositoryProvider = analyticsRepositoryProvider;
  }

  @Override
  public GetLearningStatsUseCase get() {
    return newInstance(analyticsRepositoryProvider.get());
  }

  public static GetLearningStatsUseCase_Factory create(
      Provider<AnalyticsRepository> analyticsRepositoryProvider) {
    return new GetLearningStatsUseCase_Factory(analyticsRepositoryProvider);
  }

  public static GetLearningStatsUseCase newInstance(AnalyticsRepository analyticsRepository) {
    return new GetLearningStatsUseCase(analyticsRepository);
  }
}
