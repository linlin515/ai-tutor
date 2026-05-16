package com.aitutor.app.data.repository;

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
public final class UserProfileRepository_Factory implements Factory<UserProfileRepository> {
  private final Provider<AuthRepository> authRepositoryProvider;

  public UserProfileRepository_Factory(Provider<AuthRepository> authRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
  }

  @Override
  public UserProfileRepository get() {
    return newInstance(authRepositoryProvider.get());
  }

  public static UserProfileRepository_Factory create(
      Provider<AuthRepository> authRepositoryProvider) {
    return new UserProfileRepository_Factory(authRepositoryProvider);
  }

  public static UserProfileRepository newInstance(AuthRepository authRepository) {
    return new UserProfileRepository(authRepository);
  }
}
