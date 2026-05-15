package com.aitutor.app.ui.camera;

import android.content.Context;
import com.aitutor.app.domain.repository.ChatRepository;
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
public final class CameraViewModel_Factory implements Factory<CameraViewModel> {
  private final Provider<ChatRepository> chatRepositoryProvider;

  private final Provider<Context> contextProvider;

  public CameraViewModel_Factory(Provider<ChatRepository> chatRepositoryProvider,
      Provider<Context> contextProvider) {
    this.chatRepositoryProvider = chatRepositoryProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public CameraViewModel get() {
    return newInstance(chatRepositoryProvider.get(), contextProvider.get());
  }

  public static CameraViewModel_Factory create(Provider<ChatRepository> chatRepositoryProvider,
      Provider<Context> contextProvider) {
    return new CameraViewModel_Factory(chatRepositoryProvider, contextProvider);
  }

  public static CameraViewModel newInstance(ChatRepository chatRepository, Context context) {
    return new CameraViewModel(chatRepository, context);
  }
}
