package com.aitutor.app.domain.usecase;

import android.content.Context;
import com.aitutor.app.domain.repository.StudyReportRepository;
import com.aitutor.app.ui.report.PdfReportRenderer;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class ShareStudyReportUseCase_Factory implements Factory<ShareStudyReportUseCase> {
  private final Provider<StudyReportRepository> studyReportRepositoryProvider;

  private final Provider<PdfReportRenderer> pdfReportRendererProvider;

  private final Provider<Context> contextProvider;

  public ShareStudyReportUseCase_Factory(
      Provider<StudyReportRepository> studyReportRepositoryProvider,
      Provider<PdfReportRenderer> pdfReportRendererProvider, Provider<Context> contextProvider) {
    this.studyReportRepositoryProvider = studyReportRepositoryProvider;
    this.pdfReportRendererProvider = pdfReportRendererProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public ShareStudyReportUseCase get() {
    return newInstance(studyReportRepositoryProvider.get(), pdfReportRendererProvider.get(), contextProvider.get());
  }

  public static ShareStudyReportUseCase_Factory create(
      Provider<StudyReportRepository> studyReportRepositoryProvider,
      Provider<PdfReportRenderer> pdfReportRendererProvider, Provider<Context> contextProvider) {
    return new ShareStudyReportUseCase_Factory(studyReportRepositoryProvider, pdfReportRendererProvider, contextProvider);
  }

  public static ShareStudyReportUseCase newInstance(StudyReportRepository studyReportRepository,
      PdfReportRenderer pdfReportRenderer, Context context) {
    return new ShareStudyReportUseCase(studyReportRepository, pdfReportRenderer, context);
  }
}
