package com.aitutor.app.ui.conversation;

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
public final class ConversationViewModel_Factory implements Factory<ConversationViewModel> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  public ConversationViewModel_Factory(Provider<ChatRepository> chatRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
  }

  @Override
  public ConversationViewModel get() {
    return newInstance(chatRepositoryProvider.get());
  }

  public static ConversationViewModel_Factory create(
      Provider<ChatRepository> chatRepositoryProvider) {
    return new ConversationViewModel_Factory(chatRepositoryProvider);
  }

  public static ConversationViewModel newInstance(ChatRepository chatRepository) {
    return new ConversationViewModel(chatRepository);
  }
}
