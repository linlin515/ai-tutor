package com.aitutor.app.data.repository;

import android.content.Context;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import com.aitutor.app.data.media.CloudAsrEngine;
import com.aitutor.app.data.media.CloudTtsEngine;
import com.aitutor.app.data.media.TtsAudioPlayer;
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

  private final Provider<CloudAsrEngine> cloudAsrEngineProvider;

  private final Provider<CloudTtsEngine> cloudTtsEngineProvider;

  private final Provider<TtsAudioPlayer> ttsAudioPlayerProvider;

  public VoiceRepositoryImpl_Factory(Provider<Context> contextProvider,
      Provider<SpeechRecognizer> speechRecognizerProvider,
      Provider<TextToSpeech> textToSpeechProvider, Provider<CloudAsrEngine> cloudAsrEngineProvider,
      Provider<CloudTtsEngine> cloudTtsEngineProvider,
      Provider<TtsAudioPlayer> ttsAudioPlayerProvider) {
    this.contextProvider = contextProvider;
    this.speechRecognizerProvider = speechRecognizerProvider;
    this.textToSpeechProvider = textToSpeechProvider;
    this.cloudAsrEngineProvider = cloudAsrEngineProvider;
    this.cloudTtsEngineProvider = cloudTtsEngineProvider;
    this.ttsAudioPlayerProvider = ttsAudioPlayerProvider;
  }

  @Override
  public VoiceRepositoryImpl get() {
    return newInstance(contextProvider.get(), speechRecognizerProvider.get(), textToSpeechProvider.get(), cloudAsrEngineProvider.get(), cloudTtsEngineProvider.get(), ttsAudioPlayerProvider.get());
  }

  public static VoiceRepositoryImpl_Factory create(Provider<Context> contextProvider,
      Provider<SpeechRecognizer> speechRecognizerProvider,
      Provider<TextToSpeech> textToSpeechProvider, Provider<CloudAsrEngine> cloudAsrEngineProvider,
      Provider<CloudTtsEngine> cloudTtsEngineProvider,
      Provider<TtsAudioPlayer> ttsAudioPlayerProvider) {
    return new VoiceRepositoryImpl_Factory(contextProvider, speechRecognizerProvider, textToSpeechProvider, cloudAsrEngineProvider, cloudTtsEngineProvider, ttsAudioPlayerProvider);
  }

  public static VoiceRepositoryImpl newInstance(Context context, SpeechRecognizer speechRecognizer,
      TextToSpeech textToSpeech, CloudAsrEngine cloudAsrEngine, CloudTtsEngine cloudTtsEngine,
      TtsAudioPlayer ttsAudioPlayer) {
    return new VoiceRepositoryImpl(context, speechRecognizer, textToSpeech, cloudAsrEngine, cloudTtsEngine, ttsAudioPlayer);
  }
}
