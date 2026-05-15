package com.aitutor.app.ui.screen.review;

import com.aitutor.app.domain.repository.WrongAnswerRepository;
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
public final class ReviewViewModel_Factory implements Factory<ReviewViewModel> {
  private final Provider<WrongAnswerRepository> wrongAnswerRepositoryProvider;

  public ReviewViewModel_Factory(Provider<WrongAnswerRepository> wrongAnswerRepositoryProvider) {
    this.wrongAnswerRepositoryProvider = wrongAnswerRepositoryProvider;
  }

  @Override
  public ReviewViewModel get() {
    return newInstance(wrongAnswerRepositoryProvider.get());
  }

  public static ReviewViewModel_Factory create(
      Provider<WrongAnswerRepository> wrongAnswerRepositoryProvider) {
    return new ReviewViewModel_Factory(wrongAnswerRepositoryProvider);
  }

  public static ReviewViewModel newInstance(WrongAnswerRepository wrongAnswerRepository) {
    return new ReviewViewModel(wrongAnswerRepository);
  }
}
