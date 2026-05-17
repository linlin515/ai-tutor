package com.aitutor.app.domain.usecase.voice;

import com.aitutor.app.domain.repository.VoiceRepository;
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
public final class StartListeningUseCase_Factory implements Factory<StartListeningUseCase> {
  private final Provider<VoiceRepository> voiceRepositoryProvider;

  public StartListeningUseCase_Factory(Provider<VoiceRepository> voiceRepositoryProvider) {
    this.voiceRepositoryProvider = voiceRepositoryProvider;
  }

  @Override
  public StartListeningUseCase get() {
    return newInstance(voiceRepositoryProvider.get());
  }

  public static StartListeningUseCase_Factory create(
      Provider<VoiceRepository> voiceRepositoryProvider) {
    return new StartListeningUseCase_Factory(voiceRepositoryProvider);
  }

  public static StartListeningUseCase newInstance(VoiceRepository voiceRepository) {
    return new StartListeningUseCase(voiceRepository);
  }
}
