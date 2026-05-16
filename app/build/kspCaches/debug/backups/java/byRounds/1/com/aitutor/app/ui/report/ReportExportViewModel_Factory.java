package com.aitutor.app.ui.report;

import com.aitutor.app.domain.usecase.ShareStudyReportUseCase;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class ReportExportViewModel_Factory implements Factory<ReportExportViewModel> {
  private final Provider<ShareStudyReportUseCase> shareStudyReportUseCaseProvider;

  public ReportExportViewModel_Factory(
      Provider<ShareStudyReportUseCase> shareStudyReportUseCaseProvider) {
    this.shareStudyReportUseCaseProvider = shareStudyReportUseCaseProvider;
  }

  @Override
  public ReportExportViewModel get() {
    return newInstance(shareStudyReportUseCaseProvider.get());
  }

  public static ReportExportViewModel_Factory create(
      Provider<ShareStudyReportUseCase> shareStudyReportUseCaseProvider) {
    return new ReportExportViewModel_Factory(shareStudyReportUseCaseProvider);
  }

  public static ReportExportViewModel newInstance(ShareStudyReportUseCase shareStudyReportUseCase) {
    return new ReportExportViewModel(shareStudyReportUseCase);
  }
}
