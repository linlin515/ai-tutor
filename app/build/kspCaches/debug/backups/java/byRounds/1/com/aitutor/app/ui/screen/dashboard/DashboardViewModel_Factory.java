package com.aitutor.app.ui.screen.dashboard;

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
public final class DashboardViewModel_Factory implements Factory<DashboardViewModel> {
  private final Provider<AnalyticsRepository> analyticsRepositoryProvider;

  public DashboardViewModel_Factory(Provider<AnalyticsRepository> analyticsRepositoryProvider) {
    this.analyticsRepositoryProvider = analyticsRepositoryProvider;
  }

  @Override
  public DashboardViewModel get() {
    return newInstance(analyticsRepositoryProvider.get());
  }

  public static DashboardViewModel_Factory create(
      Provider<AnalyticsRepository> analyticsRepositoryProvider) {
    return new DashboardViewModel_Factory(analyticsRepositoryProvider);
  }

  public static DashboardViewModel newInstance(AnalyticsRepository analyticsRepository) {
    return new DashboardViewModel(analyticsRepository);
  }
}
