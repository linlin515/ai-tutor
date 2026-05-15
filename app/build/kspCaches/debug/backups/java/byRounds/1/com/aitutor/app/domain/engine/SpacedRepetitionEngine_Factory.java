package com.aitutor.app.domain.engine;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class SpacedRepetitionEngine_Factory implements Factory<SpacedRepetitionEngine> {
  @Override
  public SpacedRepetitionEngine get() {
    return newInstance();
  }

  public static SpacedRepetitionEngine_Factory create() {
    return InstanceHolder.INSTANCE;
  }

  public static SpacedRepetitionEngine newInstance() {
    return new SpacedRepetitionEngine();
  }

  private static final class InstanceHolder {
    private static final SpacedRepetitionEngine_Factory INSTANCE = new SpacedRepetitionEngine_Factory();
  }
}
