package com.aitutor.app.data.media;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class TtsAudioPlayer_Factory implements Factory<TtsAudioPlayer> {
  @Override
  public TtsAudioPlayer get() {
    return newInstance();
  }

  public static TtsAudioPlayer_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static TtsAudioPlayer newInstance() {
    return new TtsAudioPlayer();
  }

  private static final class InstanceHolder {
    private static final TtsAudioPlayer_Factory INSTANCE = new TtsAudioPlayer_Factory();
  }
}
