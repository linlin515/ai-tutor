package com.aitutor.app.data.repository;

import com.aitutor.app.data.local.dao.AnalyticsDao;
import com.aitutor.app.data.remote.api.AnalyticsApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AnalyticsRepositoryImpl_Factory implements Factory<AnalyticsRepositoryImpl> {
  private final Provider<AnalyticsDao> analyticsDaoProvider;

  private final Provider<AnalyticsApi> analyticsApiProvider;

  public AnalyticsRepositoryImpl_Factory(Provider<AnalyticsDao> analyticsDaoProvider,
      Provider<AnalyticsApi> analyticsApiProvider) {
    this.analyticsDaoProvider = analyticsDaoProvider;
    this.analyticsApiProvider = analyticsApiProvider;
  }

  @Override
  public AnalyticsRepositoryImpl get() {
    return newInstance(analyticsDaoProvider.get(), analyticsApiProvider.get());
  }

  public static AnalyticsRepositoryImpl_Factory create(Provider<AnalyticsDao> analyticsDaoProvider,
      Provider<AnalyticsApi> analyticsApiProvider) {
    return new AnalyticsRepositoryImpl_Factory(analyticsDaoProvider, analyticsApiProvider);
  }

  public static AnalyticsRepositoryImpl newInstance(AnalyticsDao analyticsDao,
      AnalyticsApi analyticsApi) {
    return new AnalyticsRepositoryImpl(analyticsDao, analyticsApi);
  }
}
