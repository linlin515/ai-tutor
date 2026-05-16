package com.aitutor.app.data.repository;

import com.aitutor.app.data.local.dao.SubscriptionCacheDao;
import com.aitutor.app.data.remote.api.SubscriptionApi;
import com.google.gson.Gson;
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
public final class SubscriptionRepositoryImpl_Factory implements Factory<SubscriptionRepositoryImpl> {
  private final Provider<SubscriptionApi> subscriptionApiProvider;

  private final Provider<SubscriptionCacheDao> subscriptionCacheDaoProvider;

  private final Provider<Gson> gsonProvider;

  public SubscriptionRepositoryImpl_Factory(Provider<SubscriptionApi> subscriptionApiProvider,
      Provider<SubscriptionCacheDao> subscriptionCacheDaoProvider, Provider<Gson> gsonProvider) {
    this.subscriptionApiProvider = subscriptionApiProvider;
    this.subscriptionCacheDaoProvider = subscriptionCacheDaoProvider;
    this.gsonProvider = gsonProvider;
  }

  @Override
  public SubscriptionRepositoryImpl get() {
    return newInstance(subscriptionApiProvider.get(), subscriptionCacheDaoProvider.get(), gsonProvider.get());
  }

  public static SubscriptionRepositoryImpl_Factory create(
      Provider<SubscriptionApi> subscriptionApiProvider,
      Provider<SubscriptionCacheDao> subscriptionCacheDaoProvider, Provider<Gson> gsonProvider) {
    return new SubscriptionRepositoryImpl_Factory(subscriptionApiProvider, subscriptionCacheDaoProvider, gsonProvider);
  }

  public static SubscriptionRepositoryImpl newInstance(SubscriptionApi subscriptionApi,
      SubscriptionCacheDao subscriptionCacheDao, Gson gson) {
    return new SubscriptionRepositoryImpl(subscriptionApi, subscriptionCacheDao, gson);
  }
}
