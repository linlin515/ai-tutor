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
public final class StreamChatUseCase_Factory implements Factory<StreamChatUseCase> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  public StreamChatUseCase_Factory(Provider<ChatRepository> chatRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
  }

  @Override
  public StreamChatUseCase get() {
    return newInstance(chatRepositoryProvider.get());
  }

  public static StreamChatUseCase_Factory create(Provider<ChatRepository> chatRepositoryProvider) {
    return new StreamChatUseCase_Factory(chatRepositoryProvider);
  }

  public static StreamChatUseCase newInstance(ChatRepository chatRepository) {
    return new StreamChatUseCase(chatRepository);
  }
}
