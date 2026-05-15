package com.aitutor.app.ui.screen.quiz;

import com.aitutor.app.domain.usecase.quiz.GenerateQuizUseCase;
import com.aitutor.app.domain.usecase.quiz.SubmitQuizUseCase;
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
public final class QuizViewModel_Factory implements Factory<QuizViewModel> {
  private final Provider<GenerateQuizUseCase> generateQuizUseCaseProvider;

  private final Provider<SubmitQuizUseCase> submitQuizUseCaseProvider;

  public QuizViewModel_Factory(Provider<GenerateQuizUseCase> generateQuizUseCaseProvider,
      Provider<SubmitQuizUseCase> submitQuizUseCaseProvider) {
    this.generateQuizUseCaseProvider = generateQuizUseCaseProvider;
    this.submitQuizUseCaseProvider = submitQuizUseCaseProvider;
  }

  @Override
  public QuizViewModel get() {
    return newInstance(generateQuizUseCaseProvider.get(), submitQuizUseCaseProvider.get());
  }

  public static QuizViewModel_Factory create(
      Provider<GenerateQuizUseCase> generateQuizUseCaseProvider,
      Provider<SubmitQuizUseCase> submitQuizUseCaseProvider) {
    return new QuizViewModel_Factory(generateQuizUseCaseProvider, submitQuizUseCaseProvider);
  }

  public static QuizViewModel newInstance(GenerateQuizUseCase generateQuizUseCase,
      SubmitQuizUseCase submitQuizUseCase) {
    return new QuizViewModel(generateQuizUseCase, submitQuizUseCase);
  }
}
