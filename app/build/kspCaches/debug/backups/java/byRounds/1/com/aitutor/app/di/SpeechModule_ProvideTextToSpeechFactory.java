package com.aitutor.app.di;

import android.content.Context;
import android.speech.tts.TextToSpeech;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class SpeechModule_ProvideTextToSpeechFactory implements Factory<TextToSpeech> {
  private final Provider<Context> contextProvider;

  public SpeechModule_ProvideTextToSpeechFactory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public TextToSpeech get() {
    return provideTextToSpeech(contextProvider.get());
  }

  public static SpeechModule_ProvideTextToSpeechFactory create(Provider<Context> contextProvider) {
    return new SpeechModule_ProvideTextToSpeechFactory(contextProvider);
  }

  public static TextToSpeech provideTextToSpeech(Context context) {
    return Preconditions.checkNotNullFromProvides(SpeechModule.INSTANCE.provideTextToSpeech(context));
  }
}
