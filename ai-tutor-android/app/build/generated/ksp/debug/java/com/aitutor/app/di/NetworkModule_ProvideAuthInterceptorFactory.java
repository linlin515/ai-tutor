package com.aitutor.app.di;

import com.aitutor.app.data.remote.interceptor.AuthInterceptor;
import com.aitutor.app.data.remote.interceptor.TokenManager;
import com.aitutor.app.domain.repository.AuthRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

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
public final class NetworkModule_ProvideAuthInterceptorFactory implements Factory<AuthInterceptor> {
  private final Provider<TokenManager> tokenManagerProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  public NetworkModule_ProvideAuthInterceptorFactory(Provider<TokenManager> tokenManagerProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    this.tokenManagerProvider = tokenManagerProvider;
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public AuthInterceptor get() {
    return provideAuthInterceptor(tokenManagerProvider.get(), authRepositoryProvider);
  }

  public static NetworkModule_ProvideAuthInterceptorFactory create(
      Provider<TokenManager> tokenManagerProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    return new NetworkModule_ProvideAuthInterceptorFactory(tokenManagerProvider, authRepositoryProvider);
  }

  public static AuthInterceptor provideAuthInterceptor(TokenManager tokenManager,
      Provider<AuthRepository> authRepositoryProvider) {
    return Preconditions.checkNotNullFromProvides(NetworkModule.INSTANCE.provideAuthInterceptor(tokenManager, authRepositoryProvider));
  }
}
