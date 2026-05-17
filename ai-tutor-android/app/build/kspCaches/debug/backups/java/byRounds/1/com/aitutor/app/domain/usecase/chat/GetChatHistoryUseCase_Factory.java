package com.aitutor.app.domain.usecase.chat;

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
public final class GetChatHistoryUseCase_Factory implements Factory<GetChatHistoryUseCase> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  public GetChatHistoryUseCase_Factory(Provider<ChatRepository> chatRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
  }

  @Override
  public GetChatHistoryUseCase get() {
    return newInstance(chatRepositoryProvider.get());
  }

  public static GetChatHistoryUseCase_Factory create(
      Provider<ChatRepository> chatRepositoryProvider) {
    return new GetChatHistoryUseCase_Factory(chatRepositoryProvider);
  }

  public static GetChatHistoryUseCase newInstance(ChatRepository chatRepository) {
    return new GetChatHistoryUseCase(chatRepository);
  }
}
