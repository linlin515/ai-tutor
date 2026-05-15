package com.aitutor.app.data.repository;

import com.aitutor.app.data.local.dao.WrongAnswerDao;
import com.aitutor.app.domain.engine.SpacedRepetitionEngine;
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
public final class WrongAnswerRepositoryImpl_Factory implements Factory<WrongAnswerRepositoryImpl> {
  private final Provider<WrongAnswerDao> wrongAnswerDaoProvider;

  private final Provider<SpacedRepetitionEngine> spacedRepetitionEngineProvider;

  public WrongAnswerRepositoryImpl_Factory(Provider<WrongAnswerDao> wrongAnswerDaoProvider,
      Provider<SpacedRepetitionEngine> spacedRepetitionEngineProvider) {
    this.wrongAnswerDaoProvider = wrongAnswerDaoProvider;
    this.spacedRepetitionEngineProvider = spacedRepetitionEngineProvider;
  }

  @Override
  public WrongAnswerRepositoryImpl get() {
    return newInstance(wrongAnswerDaoProvider.get(), spacedRepetitionEngineProvider.get());
  }

  public static WrongAnswerRepositoryImpl_Factory create(
      Provider<WrongAnswerDao> wrongAnswerDaoProvider,
      Provider<SpacedRepetitionEngine> spacedRepetitionEngineProvider) {
    return new WrongAnswerRepositoryImpl_Factory(wrongAnswerDaoProvider, spacedRepetitionEngineProvider);
  }

  public static WrongAnswerRepositoryImpl newInstance(WrongAnswerDao wrongAnswerDao,
      SpacedRepetitionEngine spacedRepetitionEngine) {
    return new WrongAnswerRepositoryImpl(wrongAnswerDao, spacedRepetitionEngine);
  }
}
