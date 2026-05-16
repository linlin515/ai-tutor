package com.aitutor.app.ui.screen.dashboard;

import com.aitutor.app.domain.repository.AnalyticsRepository;
import com.aitutor.app.domain.repository.GamificationRepository;
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
public final class DashboardViewModel_Factory implements Factory<DashboardViewModel> {
  private final Provider<AnalyticsRepository> analyticsRepositoryProvider;

  private final Provider<GamificationRepository> gamificationRepositoryProvider;

  public DashboardViewModel_Factory(Provider<AnalyticsRepository> analyticsRepositoryProvider,
      Provider<GamificationRepository> gamificationRepositoryProvider) {
    this.analyticsRepositoryProvider = analyticsRepositoryProvider;
    this.gamificationRepositoryProvider = gamificationRepositoryProvider;
  }

  @Override
  public DashboardViewModel get() {
    return newInstance(analyticsRepositoryProvider.get(), gamificationRepositoryProvider.get());
  }

  public static DashboardViewModel_Factory create(
      Provider<AnalyticsRepository> analyticsRepositoryProvider,
      Provider<GamificationRepository> gamificationRepositoryProvider) {
    return new DashboardViewModel_Factory(analyticsRepositoryProvider, gamificationRepositoryProvider);
  }

  public static DashboardViewModel newInstance(AnalyticsRepository analyticsRepository,
      GamificationRepository gamificationRepository) {
    return new DashboardViewModel(analyticsRepository, gamificationRepository);
  }
}
