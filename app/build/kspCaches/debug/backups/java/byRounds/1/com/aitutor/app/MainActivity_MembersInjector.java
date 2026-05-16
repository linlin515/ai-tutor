package com.aitutor.app;

import com.aitutor.app.data.remote.datastore.LanguagePreferences;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class MainActivity_MembersInjector implements MembersInjector<MainActivity> {
  private final Provider<LanguagePreferences> languagePreferencesProvider;

  public MainActivity_MembersInjector(Provider<LanguagePreferences> languagePreferencesProvider) {
    this.languagePreferencesProvider = languagePreferencesProvider;
  }

  public static MembersInjector<MainActivity> create(
      Provider<LanguagePreferences> languagePreferencesProvider) {
    return new MainActivity_MembersInjector(languagePreferencesProvider);
  }

  @Override
  public void injectMembers(MainActivity instance) {
    injectLanguagePreferences(instance, languagePreferencesProvider.get());
  }

  @InjectedFieldSignature("com.aitutor.app.MainActivity.languagePreferences")
  public static void injectLanguagePreferences(MainActivity instance,
      LanguagePreferences languagePreferences) {
    instance.languagePreferences = languagePreferences;
  }
}
