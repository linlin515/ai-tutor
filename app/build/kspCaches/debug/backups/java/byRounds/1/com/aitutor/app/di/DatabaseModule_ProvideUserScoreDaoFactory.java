package com.aitutor.app.di;

import com.aitutor.app.data.local.dao.UserScoreDao;
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
public final class DatabaseModule_ProvideUserScoreDaoFactory implements Factory<UserScoreDao> {
  private final Provider<AiTutorDatabase> databaseProvider;

  public DatabaseModule_ProvideUserScoreDaoFactory(Provider<AiTutorDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public UserScoreDao get() {
    return provideUserScoreDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideUserScoreDaoFactory create(
      Provider<AiTutorDatabase> databaseProvider) {
    return new DatabaseModule_ProvideUserScoreDaoFactory(databaseProvider);
  }

  public static UserScoreDao provideUserScoreDao(AiTutorDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideUserScoreDao(database));
  }
}
