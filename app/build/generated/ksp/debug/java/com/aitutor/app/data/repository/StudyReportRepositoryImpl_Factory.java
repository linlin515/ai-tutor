package com.aitutor.app.data.repository;

import com.aitutor.app.data.local.dao.StudyReportDao;
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
public final class StudyReportRepositoryImpl_Factory implements Factory<StudyReportRepositoryImpl> {
  private final Provider<StudyReportDao> studyReportDaoProvider;

  public StudyReportRepositoryImpl_Factory(Provider<StudyReportDao> studyReportDaoProvider) {
    this.studyReportDaoProvider = studyReportDaoProvider;
  }

  @Override
  public StudyReportRepositoryImpl get() {
    return newInstance(studyReportDaoProvider.get());
  }

  public static StudyReportRepositoryImpl_Factory create(
      Provider<StudyReportDao> studyReportDaoProvider) {
    return new StudyReportRepositoryImpl_Factory(studyReportDaoProvider);
  }

  public static StudyReportRepositoryImpl newInstance(StudyReportDao studyReportDao) {
    return new StudyReportRepositoryImpl(studyReportDao);
  }
}
