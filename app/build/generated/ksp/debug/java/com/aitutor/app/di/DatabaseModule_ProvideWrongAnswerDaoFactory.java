package com.aitutor.app.di;

import com.aitutor.app.data.local.dao.WrongAnswerDao;
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
public final class DatabaseModule_ProvideWrongAnswerDaoFactory implements Factory<WrongAnswerDao> {
  private final Provider<AiTutorDatabase> databaseProvider;

  public DatabaseModule_ProvideWrongAnswerDaoFactory(Provider<AiTutorDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public WrongAnswerDao get() {
    return provideWrongAnswerDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideWrongAnswerDaoFactory create(
      Provider<AiTutorDatabase> databaseProvider) {
    return new DatabaseModule_ProvideWrongAnswerDaoFactory(databaseProvider);
  }

  public static WrongAnswerDao provideWrongAnswerDao(AiTutorDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideWrongAnswerDao(database));
  }
}
