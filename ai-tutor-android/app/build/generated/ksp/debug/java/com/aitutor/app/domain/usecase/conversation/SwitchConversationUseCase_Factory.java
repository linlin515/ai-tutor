package com.aitutor.app.domain.usecase.conversation;

import com.aitutor.app.domain.repository.ChatRepository;
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
public final class SwitchConversationUseCase_Factory implements Factory<SwitchConversationUseCase> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  public SwitchConversationUseCase_Factory(Provider<ChatRepository> chatRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
  }

  @Override
  public SwitchConversationUseCase get() {
    return newInstance(chatRepositoryProvider.get());
  }

  public static SwitchConversationUseCase_Factory create(
      Provider<ChatRepository> chatRepositoryProvider) {
    return new SwitchConversationUseCase_Factory(chatRepositoryProvider);
  }

  public static SwitchConversationUseCase newInstance(ChatRepository chatRepository) {
    return new SwitchConversationUseCase(chatRepository);
  }
}
