package com.aitutor.app.di;

import com.aitutor.app.data.local.dao.ScoreLogDao;
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
public final class DatabaseModule_ProvideScoreLogDaoFactory implements Factory<ScoreLogDao> {
  private final Provider<AiTutorDatabase> databaseProvider;

  public DatabaseModule_ProvideScoreLogDaoFactory(Provider<AiTutorDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ScoreLogDao get() {
    return provideScoreLogDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideScoreLogDaoFactory create(
      Provider<AiTutorDatabase> databaseProvider) {
    return new DatabaseModule_ProvideScoreLogDaoFactory(databaseProvider);
  }

  public static ScoreLogDao provideScoreLogDao(AiTutorDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideScoreLogDao(database));
  }
}
