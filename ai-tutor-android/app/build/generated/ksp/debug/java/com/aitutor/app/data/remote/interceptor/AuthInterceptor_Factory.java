package com.aitutor.app.data.remote.interceptor;

import com.aitutor.app.domain.repository.AuthRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class AuthInterceptor_Factory implements Factory<AuthInterceptor> {
  private final Provider<TokenManager> tokenManagerProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  public AuthInterceptor_Factory(Provider<TokenManager> tokenManagerProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    this.tokenManagerProvider = tokenManagerProvider;
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public AuthInterceptor get() {
    return newInstance(tokenManagerProvider.get(), authRepositoryProvider);
  }

  public static AuthInterceptor_Factory create(Provider<TokenManager> tokenManagerProvider,
      Provider<AuthRepository> authRepositoryProvider) {
    return new AuthInterceptor_Factory(tokenManagerProvider, authRepositoryProvider);
  }

  public static AuthInterceptor newInstance(TokenManager tokenManager,
      Provider<AuthRepository> authRepositoryProvider) {
    return new AuthInterceptor(tokenManager, authRepositoryProvider);
  }
}
