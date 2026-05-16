package com.aitutor.app.data.media;

import com.aitutor.app.data.remote.interceptor.TokenManager;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class CloudTtsEngine_Factory implements Factory<CloudTtsEngine> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<TokenManager> tokenManagerProvider;

  public CloudTtsEngine_Factory(Provider<OkHttpClient> okHttpClientProvider,
      Provider<TokenManager> tokenManagerProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
    this.tokenManagerProvider = tokenManagerProvider;
  }

  @Override
  public CloudTtsEngine get() {
    return newInstance(okHttpClientProvider.get(), tokenManagerProvider.get());
  }

  public static CloudTtsEngine_Factory create(Provider<OkHttpClient> okHttpClientProvider,
      Provider<TokenManager> tokenManagerProvider) {
    return new CloudTtsEngine_Factory(okHttpClientProvider, tokenManagerProvider);
  }

  public static CloudTtsEngine newInstance(OkHttpClient okHttpClient, TokenManager tokenManager) {
    return new CloudTtsEngine(okHttpClient, tokenManager);
  }
}
