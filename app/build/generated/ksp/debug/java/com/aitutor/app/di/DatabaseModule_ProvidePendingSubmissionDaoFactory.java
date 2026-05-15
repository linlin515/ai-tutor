package com.aitutor.app.di;

import com.aitutor.app.data.local.dao.PendingSubmissionDao;
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
public final class DatabaseModule_ProvidePendingSubmissionDaoFactory implements Factory<PendingSubmissionDao> {
  private final Provider<AiTutorDatabase> databaseProvider;

  public DatabaseModule_ProvidePendingSubmissionDaoFactory(
      Provider<AiTutorDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public PendingSubmissionDao get() {
    return providePendingSubmissionDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvidePendingSubmissionDaoFactory create(
      Provider<AiTutorDatabase> databaseProvider) {
    return new DatabaseModule_ProvidePendingSubmissionDaoFactory(databaseProvider);
  }

  public static PendingSubmissionDao providePendingSubmissionDao(AiTutorDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.providePendingSubmissionDao(database));
  }
}
