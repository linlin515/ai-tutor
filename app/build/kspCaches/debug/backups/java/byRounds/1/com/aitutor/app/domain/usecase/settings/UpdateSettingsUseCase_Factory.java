package com.aitutor.app.domain.usecase.settings;

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
public final class UpdateSettingsUseCase_Factory implements Factory<UpdateSettingsUseCase> {
  private final Provider<SettingsRepository> settingsRepositoryProvider;

  public UpdateSettingsUseCase_Factory(Provider<SettingsRepository> settingsRepositoryProvider) {
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public UpdateSettingsUseCase get() {
    return newInstance(settingsRepositoryProvider.get());
  }

  public static UpdateSettingsUseCase_Factory create(
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new UpdateSettingsUseCase_Factory(settingsRepositoryProvider);
  }

  public static UpdateSettingsUseCase newInstance(SettingsRepository settingsRepository) {
    return new UpdateSettingsUseCase(settingsRepository);
  }
}
