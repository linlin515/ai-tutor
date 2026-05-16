package com.aitutor.app.di;

import com.aitutor.app.data.local.dao.StudyReportDao;
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
public final class DatabaseModule_ProvideStudyReportDaoFactory implements Factory<StudyReportDao> {
  private final Provider<AiTutorDatabase> databaseProvider;

  public DatabaseModule_ProvideStudyReportDaoFactory(Provider<AiTutorDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public StudyReportDao get() {
    return provideStudyReportDao(databaseProvider.get());
  }

  public static DatabaseModule_ProvideStudyReportDaoFactory create(
      Provider<AiTutorDatabase> databaseProvider) {
    return new DatabaseModule_ProvideStudyReportDaoFactory(databaseProvider);
  }

  public static StudyReportDao provideStudyReportDao(AiTutorDatabase database) {
    return Preconditions.checkNotNullFromProvides(DatabaseModule.INSTANCE.provideStudyReportDao(database));
  }
}
