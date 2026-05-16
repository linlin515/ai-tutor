package com.aitutor.app.ui.report;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class PdfReportRenderer_Factory implements Factory<PdfReportRenderer> {
  @Override
  public PdfReportRenderer get() {
    return newInstance();
  }

  public static PdfReportRenderer_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static PdfReportRenderer newInstance() {
    return new PdfReportRenderer();
  }

  private static final class InstanceHolder {
    private static final PdfReportRenderer_Factory INSTANCE = new PdfReportRenderer_Factory();
  }
}
