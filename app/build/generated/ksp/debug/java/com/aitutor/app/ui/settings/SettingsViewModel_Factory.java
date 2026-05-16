package com.aitutor.app.ui.settings;

import com.aitutor.app.data.local.CacheManager;
import com.aitutor.app.domain.repository.SettingsRepository;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<CacheManager> cacheManagerProvider;

  public SettingsViewModel_Factory(Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<CacheManager> cacheManagerProvider) {
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.cacheManagerProvider = cacheManagerProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(settingsRepositoryProvider.get(), cacheManagerProvider.get());
  }

  public static SettingsViewModel_Factory create(
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<CacheManager> cacheManagerProvider) {
    return new SettingsViewModel_Factory(settingsRepositoryProvider, cacheManagerProvider);
  }

  public static SettingsViewModel newInstance(SettingsRepository settingsRepository,
      CacheManager cacheManager) {
    return new SettingsViewModel(settingsRepository, cacheManager);
  }
}
