package com.aitutor.app.data.repository;

import com.aitutor.app.data.remote.api.AiTutorApi;
import com.aitutor.app.data.remote.api.SolveApi;
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
public final class SolveRepositoryImpl_Factory implements Factory<SolveRepositoryImpl> {
  private final Provider<SolveApi> solveApiProvider;

  private final Provider<AiTutorApi> aiTutorApiProvider;

  public SolveRepositoryImpl_Factory(Provider<SolveApi> solveApiProvider,
      Provider<AiTutorApi> aiTutorApiProvider) {
    this.solveApiProvider = solveApiProvider;
    this.aiTutorApiProvider = aiTutorApiProvider;
  }

  @Override
  public SolveRepositoryImpl get() {
    return newInstance(solveApiProvider.get(), aiTutorApiProvider.get());
  }

  public static SolveRepositoryImpl_Factory create(Provider<SolveApi> solveApiProvider,
      Provider<AiTutorApi> aiTutorApiProvider) {
    return new SolveRepositoryImpl_Factory(solveApiProvider, aiTutorApiProvider);
  }

  public static SolveRepositoryImpl newInstance(SolveApi solveApi, AiTutorApi aiTutorApi) {
    return new SolveRepositoryImpl(solveApi, aiTutorApi);
  }
}
