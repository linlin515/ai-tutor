package com.aitutor.app.di;

import com.aitutor.app.data.local.dao.SubscriptionCacheDao;
import com.aitutor.app.data.local.db.AiTutorDatabase;
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
public final class DatabaseModule_ProvideSubscriptionCacheDaoFactory implements Factory<SubscriptionCacheDao> {
  private final Provider<AiTutorDatabase> databaseProvider;

  public DatabaseModule_ProvideSubscriptionCacheDaoFactory(
      Provider<AiTutorDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public SubscriptionCacheDao get() {
    return provideSubscriptionCacheDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideSubscriptionCacheDaoFactory create(
      Provider<AiTutorDatabase> databaseProvider) {
    return new DatabaseModule_ProvideSubscriptionCacheDaoFactory(databaseProvider);
  }

  public static SubscriptionCacheDao provideSubscriptionCacheDao(AiTutorDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideSubscriptionCacheDao(database));
  }
}
