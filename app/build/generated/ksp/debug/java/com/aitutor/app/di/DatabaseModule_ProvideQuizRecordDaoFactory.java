package com.aitutor.app.di;

import com.aitutor.app.data.local.dao.QuizRecordDao;
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
public final class DatabaseModule_ProvideQuizRecordDaoFactory implements Factory<QuizRecordDao> {
  private final Provider<AiTutorDatabase> databaseProvider;

  public DatabaseModule_ProvideQuizRecordDaoFactory(Provider<AiTutorDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public QuizRecordDao get() {
    return provideQuizRecordDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideQuizRecordDaoFactory create(
      Provider<AiTutorDatabase> databaseProvider) {
    return new DatabaseModule_ProvideQuizRecordDaoFactory(databaseProvider);
  }

  public static QuizRecordDao provideQuizRecordDao(AiTutorDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideQuizRecordDao(database));
  }
}
