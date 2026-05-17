package com.aitutor.app.di;

import com.aitutor.app.data.remote.api.ChatStreamApi;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;
import okhttp3.OkHttpClient;

@ScopeMetadata("javax.inject.Singleton")
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
public final class NetworkModule_ProvideChatStreamApiFactory implements Factory<ChatStreamApi> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  public NetworkModule_ProvideChatStreamApiFactory(Provider<OkHttpClient> okHttpClientProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
  }

  @Override
  public ChatStreamApi get() {
    return provideChatStreamApi(okHttpClientProvider.get());
  }

  public static NetworkModule_ProvideChatStreamApiFactory create(
      Provider<OkHttpClient> okHttpClientProvider) {
    return new NetworkModule_ProvideChatStreamApiFactory(okHttpClientProvider);
  }

  public static ChatStreamApi provideChatStreamApi(OkHttpClient okHttpClient) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideChatStreamApi(okHttpClient));
  }
}
