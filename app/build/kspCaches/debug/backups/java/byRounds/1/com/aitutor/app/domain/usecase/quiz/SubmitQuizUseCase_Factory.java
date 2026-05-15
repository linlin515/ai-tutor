package com.aitutor.app.domain.usecase.quiz;

import com.aitutor.app.domain.repository.QuizRepository;
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
public final class SubmitQuizUseCase_Factory implements Factory<SubmitQuizUseCase> {
  private final Provider<QuizRepository> quizRepositoryProvider;

  public SubmitQuizUseCase_Factory(Provider<QuizRepository> quizRepositoryProvider) {
    this.quizRepositoryProvider = quizRepositoryProvider;
  }

  @Override
  public SubmitQuizUseCase get() {
    return newInstance(quizRepositoryProvider.get());
  }

  public static SubmitQuizUseCase_Factory create(Provider<QuizRepository> quizRepositoryProvider) {
    return new SubmitQuizUseCase_Factory(quizRepositoryProvider);
  }

  public static SubmitQuizUseCase newInstance(QuizRepository quizRepository) {
    return new SubmitQuizUseCase(quizRepository);
  }
}
