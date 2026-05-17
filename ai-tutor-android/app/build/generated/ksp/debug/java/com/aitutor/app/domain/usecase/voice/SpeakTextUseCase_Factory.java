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
public final class SpeakTextUseCase_Factory implements Factory<SpeakTextUseCase> {
  private final Provider<VoiceRepository> voiceRepositoryProvider;

  public SpeakTextUseCase_Factory(Provider<VoiceRepository> voiceRepositoryProvider) {
    this.voiceRepositoryProvider = voiceRepositoryProvider;
  }

  @Override
  public SpeakTextUseCase get() {
    return newInstance(voiceRepositoryProvider.get());
  }

  public static SpeakTextUseCase_Factory create(Provider<VoiceRepository> voiceRepositoryProvider) {
    return new SpeakTextUseCase_Factory(voiceRepositoryProvider);
  }

  public static SpeakTextUseCase newInstance(VoiceRepository voiceRepository) {
    return new SpeakTextUseCase(voiceRepository);
  }
}
