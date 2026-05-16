package com.aitutor.app.data.repository;

import com.aitutor.app.data.remote.datastore.SettingsDataStore;
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
public final class SettingsRepositoryImpl_Factory implements Factory<SettingsRepositoryImpl> {
  private final Provider<SettingsDataStore> settingsDataStoreProvider;

  public SettingsRepositoryImpl_Factory(Provider<SettingsDataStore> settingsDataStoreProvider) {
    this.settingsDataStoreProvider = settingsDataStoreProvider;
  }

  @Override
  public SettingsRepositoryImpl get() {
    return newInstance(settingsDataStoreProvider.get());
  }

  public static SettingsRepositoryImpl_Factory create(
      Provider<SettingsDataStore> settingsDataStoreProvider) {
    return new SettingsRepositoryImpl_Factory(settingsDataStoreProvider);
  }

  public static SettingsRepositoryImpl newInstance(SettingsDataStore settingsDataStore) {
    return new SettingsRepositoryImpl(settingsDataStore);
  }
}
