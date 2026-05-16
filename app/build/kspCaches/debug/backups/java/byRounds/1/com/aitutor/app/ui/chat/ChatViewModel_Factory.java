package com.aitutor.app.ui.chat;

import android.content.Context;
import com.aitutor.app.data.repository.UserProfileRepository;
import com.aitutor.app.domain.repository.AuthRepository;
import com.aitutor.app.domain.repository.ChatRepository;
import com.aitutor.app.domain.repository.SettingsRepository;
import com.aitutor.app.domain.repository.SolveRepository;
import com.aitutor.app.domain.repository.VoiceRepository;
import com.aitutor.app.domain.usecase.chat.ProcessTeachingResponseUseCase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class ChatViewModel_Factory implements Factory<ChatViewModel> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<VoiceRepository> voiceRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<UserProfileRepository> userProfileRepositoryProvider;

  private final Provider<ProcessTeachingResponseUseCase> processTeachingResponseUseCaseProvider;

  private final Provider<SolveRepository> solveRepositoryProvider;

  private final Provider<Context> appContextProvider;

  public ChatViewModel_Factory(Provider<ChatRepository> chatRepositoryProvider,
      Provider<VoiceRepository> voiceRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<UserProfileRepository> userProfileRepositoryProvider,
      Provider<ProcessTeachingResponseUseCase> processTeachingResponseUseCaseProvider,
      Provider<SolveRepository> solveRepositoryProvider, Provider<Context> appContextProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.voiceRepositoryProvider = voiceRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.userProfileRepositoryProvider = userProfileRepositoryProvider;
    this.processTeachingResponseUseCaseProvider = processTeachingResponseUseCaseProvider;
    this.solveRepositoryProvider = solveRepositoryProvider;
    this.appContextProvider = appContextProvider;
  }

  @Override
  public ChatViewModel get() {
    return newInstance(chatRepositoryProvider.get(), voiceRepositoryProvider.get(), settingsRepositoryProvider.get(), authRepositoryProvider.get(), userProfileRepositoryProvider.get(), processTeachingResponseUseCaseProvider.get(), solveRepositoryProvider.get(), appContextProvider.get());
  }

  public static ChatViewModel_Factory create(Provider<ChatRepository> chatRepositoryProvider,
      Provider<VoiceRepository> voiceRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<UserProfileRepository> userProfileRepositoryProvider,
      Provider<ProcessTeachingResponseUseCase> processTeachingResponseUseCaseProvider,
      Provider<SolveRepository> solveRepositoryProvider, Provider<Context> appContextProvider) {
    return new ChatViewModel_Factory(chatRepositoryProvider, voiceRepositoryProvider, settingsRepositoryProvider, authRepositoryProvider, userProfileRepositoryProvider, processTeachingResponseUseCaseProvider, solveRepositoryProvider, appContextProvider);
  }

  public static ChatViewModel newInstance(ChatRepository chatRepository,
      VoiceRepository voiceRepository, SettingsRepository settingsRepository,
      AuthRepository authRepository, UserProfileRepository userProfileRepository,
      ProcessTeachingResponseUseCase processTeachingResponseUseCase,
      SolveRepository solveRepository, Context appContext) {
    return new ChatViewModel(chatRepository, voiceRepository, settingsRepository, authRepository, userProfileRepository, processTeachingResponseUseCase, solveRepository, appContext);
  }
}
