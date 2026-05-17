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
public final class CreateConversationUseCase_Factory implements Factory<CreateConversationUseCase> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  public CreateConversationUseCase_Factory(Provider<ChatRepository> chatRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
  }

  @Override
  public CreateConversationUseCase get() {
    return newInstance(chatRepositoryProvider.get());
  }

  public static CreateConversationUseCase_Factory create(
      Provider<ChatRepository> chatRepositoryProvider) {
    return new CreateConversationUseCase_Factory(chatRepositoryProvider);
  }

  public static CreateConversationUseCase newInstance(ChatRepository chatRepository) {
    return new CreateConversationUseCase(chatRepository);
  }
}
