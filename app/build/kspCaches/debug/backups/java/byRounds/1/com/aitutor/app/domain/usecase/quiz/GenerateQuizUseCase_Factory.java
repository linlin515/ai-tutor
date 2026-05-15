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
public final class GenerateQuizUseCase_Factory implements Factory<GenerateQuizUseCase> {
  private final Provider<QuizRepository> quizRepositoryProvider;

  public GenerateQuizUseCase_Factory(Provider<QuizRepository> quizRepositoryProvider) {
    this.quizRepositoryProvider = quizRepositoryProvider;
  }

  @Override
  public GenerateQuizUseCase get() {
    return newInstance(quizRepositoryProvider.get());
  }

  public static GenerateQuizUseCase_Factory create(
      Provider<QuizRepository> quizRepositoryProvider) {
    return new GenerateQuizUseCase_Factory(quizRepositoryProvider);
  }

  public static GenerateQuizUseCase newInstance(QuizRepository quizRepository) {
    return new GenerateQuizUseCase(quizRepository);
  }
}
