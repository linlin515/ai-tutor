package com.aitutor.app.di;

import com.aitutor.app.data.remote.api.SolveApi;
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
public final class NetworkModule_ProvideSolveApiFactory implements Factory<SolveApi> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  public NetworkModule_ProvideSolveApiFactory(Provider<OkHttpClient> okHttpClientProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
  }

  @Override
  public SolveApi get() {
    return provideSolveApi(okHttpClientProvider.get());
  }

  public static NetworkModule_ProvideSolveApiFactory create(
      Provider<OkHttpClient> okHttpClientProvider) {
    return new NetworkModule_ProvideSolveApiFactory(okHttpClientProvider);
  }

  public static SolveApi provideSolveApi(OkHttpClient okHttpClient) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideSolveApi(okHttpClient));
  }
}
