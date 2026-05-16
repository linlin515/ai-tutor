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
public final class ProcessTeachingResponseUseCase_Factory implements Factory<ProcessTeachingResponseUseCase> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  public ProcessTeachingResponseUseCase_Factory(Provider<ChatRepository> chatRepositoryProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
  }

  @Override
  public ProcessTeachingResponseUseCase get() {
    return newInstance(chatRepositoryProvider.get());
  }

  public static ProcessTeachingResponseUseCase_Factory create(
      Provider<ChatRepository> chatRepositoryProvider) {
    return new ProcessTeachingResponseUseCase_Factory(chatRepositoryProvider);
  }

  public static ProcessTeachingResponseUseCase newInstance(ChatRepository chatRepository) {
    return new ProcessTeachingResponseUseCase(chatRepository);
  }
}
