package com.aitutor.app.data.model;

import com.aitutor.app.domain.repository.SubscriptionRepository;
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
public final class QuotaGuard_Factory implements Factory<QuotaGuard> {
  private final Provider<SubscriptionRepository> subscriptionRepositoryProvider;

  public QuotaGuard_Factory(Provider<SubscriptionRepository> subscriptionRepositoryProvider) {
    this.subscriptionRepositoryProvider = subscriptionRepositoryProvider;
  }

  @Override
  public QuotaGuard get() {
    return newInstance(subscriptionRepositoryProvider.get());
  }

  public static QuotaGuard_Factory create(
      Provider<SubscriptionRepository> subscriptionRepositoryProvider) {
    return new QuotaGuard_Factory(subscriptionRepositoryProvider);
  }

  public static QuotaGuard newInstance(SubscriptionRepository subscriptionRepository) {
    return new QuotaGuard(subscriptionRepository);
  }
}
