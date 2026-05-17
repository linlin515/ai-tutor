package com.aitutor.app.data.repository;

import android.content.Context;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class VoiceRepositoryImpl_Factory implements Factory<VoiceRepositoryImpl> {
  private final Provider<Context> contextProvider;

  private final Provider<SpeechRecognizer> speechRecognizerProvider;

  private final Provider<TextToSpeech> textToSpeechProvider;

  public VoiceRepositoryImpl_Factory(Provider<Context> contextProvider,
      Provider<SpeechRecognizer> speechRecognizerProvider,
      Provider<TextToSpeech> textToSpeechProvider) {
    this.contextProvider = contextProvider;
    this.speechRecognizerProvider = speechRecognizerProvider;
    this.textToSpeechProvider = textToSpeechProvider;
  }

  @Override
  public VoiceRepositoryImpl get() {
    return newInstance(contextProvider.get(), speechRecognizerProvider.get(), textToSpeechProvider.get());
  }

  public static VoiceRepositoryImpl_Factory create(Provider<Context> contextProvider,
      Provider<SpeechRecognizer> speechRecognizerProvider,
      Provider<TextToSpeech> textToSpeechProvider) {
    return new VoiceRepositoryImpl_Factory(contextProvider, speechRecognizerProvider, textToSpeechProvider);
  }

  public static VoiceRepositoryImpl newInstance(Context context, SpeechRecognizer speechRecognizer,
      TextToSpeech textToSpeech) {
    return new VoiceRepositoryImpl(context, speechRecognizer, textToSpeech);
  }
}
