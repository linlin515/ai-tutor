package com.aitutor.app.di;

import com.aitutor.app.data.local.dao.AnalyticsDao;
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
public final class DatabaseModule_ProvideAnalyticsDaoFactory implements Factory<AnalyticsDao> {
  private final Provider<AiTutorDatabase> databaseProvider;

  public DatabaseModule_ProvideAnalyticsDaoFactory(Provider<AiTutorDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public AnalyticsDao get() {
    return provideAnalyticsDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideAnalyticsDaoFactory create(
      Provider<AiTutorDatabase> databaseProvider) {
    return new DatabaseModule_ProvideAnalyticsDaoFactory(databaseProvider);
  }

  public static AnalyticsDao provideAnalyticsDao(AiTutorDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideAnalyticsDao(database));
  }
}
