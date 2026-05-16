package com.aitutor.app.ui.screen.subscription;

import com.aitutor.app.domain.repository.SubscriptionRepository;
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
public final class SubscriptionViewModel_Factory implements Factory<SubscriptionViewModel> {
  private final Provider<SubscriptionRepository> subscriptionRepositoryProvider;

  public SubscriptionViewModel_Factory(
      Provider<SubscriptionRepository> subscriptionRepositoryProvider) {
    this.subscriptionRepositoryProvider = subscriptionRepositoryProvider;
  }

  @Override
  public SubscriptionViewModel get() {
    return newInstance(subscriptionRepositoryProvider.get());
  }

  public static SubscriptionViewModel_Factory create(
      Provider<SubscriptionRepository> subscriptionRepositoryProvider) {
    return new SubscriptionViewModel_Factory(subscriptionRepositoryProvider);
  }

  public static SubscriptionViewModel newInstance(SubscriptionRepository subscriptionRepository) {
    return new SubscriptionViewModel(subscriptionRepository);
  }
}
