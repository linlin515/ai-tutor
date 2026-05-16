package com.aitutor.app.data.repository;

import com.aitutor.app.data.remote.api.GamificationApi;
import com.aitutor.app.domain.engine.GamificationEngine;
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
public final class GamificationRepositoryImpl_Factory implements Factory<GamificationRepositoryImpl> {
  private final Provider<GamificationEngine> gamificationEngineProvider;

  private final Provider<GamificationApi> gamificationApiProvider;

  public GamificationRepositoryImpl_Factory(Provider<GamificationEngine> gamificationEngineProvider,
      Provider<GamificationApi> gamificationApiProvider) {
    this.gamificationEngineProvider = gamificationEngineProvider;
    this.gamificationApiProvider = gamificationApiProvider;
  }

  @Override
  public GamificationRepositoryImpl get() {
    return newInstance(gamificationEngineProvider.get(), gamificationApiProvider.get());
  }

  public static GamificationRepositoryImpl_Factory create(
      Provider<GamificationEngine> gamificationEngineProvider,
      Provider<GamificationApi> gamificationApiProvider) {
    return new GamificationRepositoryImpl_Factory(gamificationEngineProvider, gamificationApiProvider);
  }

  public static GamificationRepositoryImpl newInstance(GamificationEngine gamificationEngine,
      GamificationApi gamificationApi) {
    return new GamificationRepositoryImpl(gamificationEngine, gamificationApi);
  }
}
