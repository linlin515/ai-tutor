package com.aitutor.app.ui.splash;

import com.aitutor.app.data.remote.interceptor.TokenManager;
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
public final class SplashViewModel_Factory implements Factory<SplashViewModel> {
  private final Provider<TokenManager> tokenManagerProvider;

  public SplashViewModel_Factory(Provider<TokenManager> tokenManagerProvider) {
    this.tokenManagerProvider = tokenManagerProvider;
  }

  @Override
  public SplashViewModel get() {
    return newInstance(tokenManagerProvider.get());
  }

  public static SplashViewModel_Factory create(Provider<TokenManager> tokenManagerProvider) {
    return new SplashViewModel_Factory(tokenManagerProvider);
  }

  public static SplashViewModel newInstance(TokenManager tokenManager) {
    return new SplashViewModel(tokenManager);
  }
}
