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
public final class DeleteConversationUseCase_Factory implements Factory<DeleteConversationUseCase> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  public DeleteConversationUseCase_Factory(Provider<ChatRepository> chatRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
  }

  @Override
  public DeleteConversationUseCase get() {
    return newInstance(chatRepositoryProvider.get());
  }

  public static DeleteConversationUseCase_Factory create(
      Provider<ChatRepository> chatRepositoryProvider) {
    return new DeleteConversationUseCase_Factory(chatRepositoryProvider);
  }

  public static DeleteConversationUseCase newInstance(ChatRepository chatRepository) {
    return new DeleteConversationUseCase(chatRepository);
  }
}
