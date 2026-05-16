package com.aitutor.app.data.media;

import com.aitutor.app.data.remote.api.AiTutorApi;
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
public final class CloudAsrEngine_Factory implements Factory<CloudAsrEngine> {
  private final Provider<AiTutorApi> aiTutorApiProvider;

  public CloudAsrEngine_Factory(Provider<AiTutorApi> aiTutorApiProvider) {
    this.aiTutorApiProvider = aiTutorApiProvider;
  }

  @Override
  public CloudAsrEngine get() {
    return newInstance(aiTutorApiProvider.get());
  }

  public static CloudAsrEngine_Factory create(Provider<AiTutorApi> aiTutorApiProvider) {
    return new CloudAsrEngine_Factory(aiTutorApiProvider);
  }

  public static CloudAsrEngine newInstance(AiTutorApi aiTutorApi) {
    return new CloudAsrEngine(aiTutorApi);
  }
}
