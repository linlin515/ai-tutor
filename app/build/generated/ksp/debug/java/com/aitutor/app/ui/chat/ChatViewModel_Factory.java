package com.aitutor.app.ui.chat;

import com.aitutor.app.domain.repository.AuthRepository;
import com.aitutor.app.domain.repository.ChatRepository;
import com.aitutor.app.domain.repository.SettingsRepository;
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
public final class ChatViewModel_Factory implements Factory<ChatViewModel> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<VoiceRepository> voiceRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  public ChatViewModel_Factory(Provider<ChatRepository> chatRepositoryProvider,
      Provider<VoiceRepository> voiceRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.voiceRepositoryProvider = voiceRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public ChatViewModel get() {
    return newInstance(chatRepositoryProvider.get(), voiceRepositoryProvider.get(), settingsRepositoryProvider.get(), authRepositoryProvider.get());
  }

  public static ChatViewModel_Factory create(Provider<ChatRepository> chatRepositoryProvider,
      Provider<VoiceRepository> voiceRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    return new ChatViewModel_Factory(chatRepositoryProvider, voiceRepositoryProvider, settingsRepositoryProvider, authRepositoryProvider);
  }

  public static ChatViewModel newInstance(ChatRepository chatRepository,
      VoiceRepository voiceRepository, SettingsRepository settingsRepository,
      AuthRepository authRepository) {
    return new ChatViewModel(chatRepository, voiceRepository, settingsRepository, authRepository);
  }
}
