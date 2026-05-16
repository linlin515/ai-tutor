package com.aitutor.app;

import android.app.Activity;
import android.app.Service;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.view.View;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.aitutor.app.data.local.CacheManager;
import com.aitutor.app.data.local.dao.AchievementDao;
import com.aitutor.app.data.local.dao.AnalyticsDao;
import com.aitutor.app.data.local.dao.ConversationDao;
import com.aitutor.app.data.local.dao.MessageDao;
import com.aitutor.app.data.local.dao.PendingSubmissionDao;
import com.aitutor.app.data.local.dao.QuizRecordDao;
import com.aitutor.app.data.local.dao.ScoreLogDao;
import com.aitutor.app.data.local.dao.SubscriptionCacheDao;
import com.aitutor.app.data.local.dao.UserScoreDao;
import com.aitutor.app.data.local.dao.WrongAnswerDao;
import com.aitutor.app.data.local.db.AiTutorDatabase;
import com.aitutor.app.data.media.CloudAsrEngine;
import com.aitutor.app.data.media.CloudTtsEngine;
import com.aitutor.app.data.media.TtsAudioPlayer;
import com.aitutor.app.data.remote.api.AiTutorApi;
import com.aitutor.app.data.remote.api.AnalyticsApi;
import com.aitutor.app.data.remote.api.ChatStreamApi;
import com.aitutor.app.data.remote.api.GamificationApi;
import com.aitutor.app.data.remote.api.QuizApi;
import com.aitutor.app.data.remote.api.SolveApi;
import com.aitutor.app.data.remote.api.SubscriptionApi;
import com.aitutor.app.data.remote.datastore.SettingsDataStore;
import com.aitutor.app.data.remote.interceptor.AuthInterceptor;
import com.aitutor.app.data.remote.interceptor.TokenManager;
import com.aitutor.app.data.repository.AnalyticsRepositoryImpl;
import com.aitutor.app.data.repository.AuthRepositoryImpl;
import com.aitutor.app.data.repository.ChatRepositoryImpl;
import com.aitutor.app.data.repository.GamificationRepositoryImpl;
import com.aitutor.app.data.repository.QuizRepositoryImpl;
import com.aitutor.app.data.repository.SettingsRepositoryImpl;
import com.aitutor.app.data.repository.SolveRepositoryImpl;
import com.aitutor.app.data.repository.SubscriptionRepositoryImpl;
import com.aitutor.app.data.repository.UserProfileRepository;
import com.aitutor.app.data.repository.VoiceRepositoryImpl;
import com.aitutor.app.data.repository.WrongAnswerRepositoryImpl;
import com.aitutor.app.di.DatabaseModule_ProvideAchievementDaoFactory;
import com.aitutor.app.di.DatabaseModule_ProvideAnalyticsDaoFactory;
import com.aitutor.app.di.DatabaseModule_ProvideConversationDaoFactory;
import com.aitutor.app.di.DatabaseModule_ProvideDatabaseFactory;
import com.aitutor.app.di.DatabaseModule_ProvideMessageDaoFactory;
import com.aitutor.app.di.DatabaseModule_ProvidePendingSubmissionDaoFactory;
import com.aitutor.app.di.DatabaseModule_ProvideQuizRecordDaoFactory;
import com.aitutor.app.di.DatabaseModule_ProvideScoreLogDaoFactory;
import com.aitutor.app.di.DatabaseModule_ProvideSubscriptionCacheDaoFactory;
import com.aitutor.app.di.DatabaseModule_ProvideUserScoreDaoFactory;
import com.aitutor.app.di.DatabaseModule_ProvideWrongAnswerDaoFactory;
import com.aitutor.app.di.GsonModule_ProvideGsonFactory;
import com.aitutor.app.di.NetworkModule_ProvideAiTutorApiFactory;
import com.aitutor.app.di.NetworkModule_ProvideAnalyticsApiFactory;
import com.aitutor.app.di.NetworkModule_ProvideAuthInterceptorFactory;
import com.aitutor.app.di.NetworkModule_ProvideChatStreamApiFactory;
import com.aitutor.app.di.NetworkModule_ProvideGamificationApiFactory;
import com.aitutor.app.di.NetworkModule_ProvideLoggingInterceptorFactory;
import com.aitutor.app.di.NetworkModule_ProvideOkHttpClientFactory;
import com.aitutor.app.di.NetworkModule_ProvideQuizApiFactory;
import com.aitutor.app.di.NetworkModule_ProvideRetrofitFactory;
import com.aitutor.app.di.NetworkModule_ProvideSolveApiFactory;
import com.aitutor.app.di.NetworkModule_ProvideSubscriptionApiFactory;
import com.aitutor.app.di.SpeechModule_ProvideSpeechRecognizerFactory;
import com.aitutor.app.di.SpeechModule_ProvideTextToSpeechFactory;
import com.aitutor.app.domain.engine.GamificationEngine;
import com.aitutor.app.domain.engine.SpacedRepetitionEngine;
import com.aitutor.app.domain.repository.AuthRepository;
import com.aitutor.app.domain.usecase.chat.ProcessTeachingResponseUseCase;
import com.aitutor.app.domain.usecase.quiz.GenerateQuizUseCase;
import com.aitutor.app.domain.usecase.quiz.SubmitQuizUseCase;
import com.aitutor.app.ui.auth.LoginViewModel;
import com.aitutor.app.ui.auth.LoginViewModel_HiltModules_KeyModule_ProvideFactory;
import com.aitutor.app.ui.camera.CameraViewModel;
import com.aitutor.app.ui.camera.CameraViewModel_HiltModules_KeyModule_ProvideFactory;
import com.aitutor.app.ui.chat.ChatViewModel;
import com.aitutor.app.ui.chat.ChatViewModel_HiltModules_KeyModule_ProvideFactory;
import com.aitutor.app.ui.conversation.ConversationViewModel;
import com.aitutor.app.ui.conversation.ConversationViewModel_HiltModules_KeyModule_ProvideFactory;
import com.aitutor.app.ui.profile.ProfileViewModel;
import com.aitutor.app.ui.profile.ProfileViewModel_HiltModules_KeyModule_ProvideFactory;
import com.aitutor.app.ui.screen.dashboard.DashboardViewModel;
import com.aitutor.app.ui.screen.dashboard.DashboardViewModel_HiltModules_KeyModule_ProvideFactory;
import com.aitutor.app.ui.screen.quiz.QuizViewModel;
import com.aitutor.app.ui.screen.quiz.QuizViewModel_HiltModules_KeyModule_ProvideFactory;
import com.aitutor.app.ui.screen.review.ReviewViewModel;
import com.aitutor.app.ui.screen.review.ReviewViewModel_HiltModules_KeyModule_ProvideFactory;
import com.aitutor.app.ui.screen.subscription.SubscriptionViewModel;
import com.aitutor.app.ui.screen.subscription.SubscriptionViewModel_HiltModules_KeyModule_ProvideFactory;
import com.aitutor.app.ui.settings.SettingsViewModel;
import com.aitutor.app.ui.settings.SettingsViewModel_HiltModules_KeyModule_ProvideFactory;
import com.aitutor.app.ui.splash.SplashViewModel;
import com.aitutor.app.ui.splash.SplashViewModel_HiltModules_KeyModule_ProvideFactory;
import com.google.gson.Gson;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DelegateFactory;
import dagger.internal.DoubleCheck;
import dagger.internal.MapBuilder;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.SetBuilder;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;

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
public final class DaggerAiTutorApp_HiltComponents_SingletonC {
  private DaggerAiTutorApp_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public AiTutorApp_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements AiTutorApp_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public AiTutorApp_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements AiTutorApp_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public AiTutorApp_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements AiTutorApp_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public AiTutorApp_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements AiTutorApp_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public AiTutorApp_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements AiTutorApp_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public AiTutorApp_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements AiTutorApp_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public AiTutorApp_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements AiTutorApp_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public AiTutorApp_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends AiTutorApp_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    private ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends AiTutorApp_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    private FragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends AiTutorApp_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    private ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends AiTutorApp_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    private ActivityCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    @Override
    public void injectMainActivity(MainActivity mainActivity) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Set<String> getViewModelKeys() {
      return SetBuilder.<String>newSetBuilder(11).add(CameraViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(ChatViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(ConversationViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(DashboardViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(LoginViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(ProfileViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(QuizViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(ReviewViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(SettingsViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(SplashViewModel_HiltModules_KeyModule_ProvideFactory.provide()).add(SubscriptionViewModel_HiltModules_KeyModule_ProvideFactory.provide()).build();
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }
  }

  private static final class ViewModelCImpl extends AiTutorApp_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    private Provider<CameraViewModel> cameraViewModelProvider;

    private Provider<ChatViewModel> chatViewModelProvider;

    private Provider<ConversationViewModel> conversationViewModelProvider;

    private Provider<DashboardViewModel> dashboardViewModelProvider;

    private Provider<LoginViewModel> loginViewModelProvider;

    private Provider<ProfileViewModel> profileViewModelProvider;

    private Provider<QuizViewModel> quizViewModelProvider;

    private Provider<ReviewViewModel> reviewViewModelProvider;

    private Provider<SettingsViewModel> settingsViewModelProvider;

    private Provider<SplashViewModel> splashViewModelProvider;

    private Provider<SubscriptionViewModel> subscriptionViewModelProvider;

    private ViewModelCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, SavedStateHandle savedStateHandleParam,
        ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    private ProcessTeachingResponseUseCase processTeachingResponseUseCase() {
      return new ProcessTeachingResponseUseCase(singletonCImpl.chatRepositoryImplProvider.get());
    }

    private GenerateQuizUseCase generateQuizUseCase() {
      return new GenerateQuizUseCase(singletonCImpl.quizRepositoryImplProvider.get());
    }

    private SubmitQuizUseCase submitQuizUseCase() {
      return new SubmitQuizUseCase(singletonCImpl.quizRepositoryImplProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.cameraViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.chatViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.conversationViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.dashboardViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.loginViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.profileViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
      this.quizViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 6);
      this.reviewViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 7);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 8);
      this.splashViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 9);
      this.subscriptionViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 10);
    }

    @Override
    public Map<String, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return MapBuilder.<String, javax.inject.Provider<ViewModel>>newMapBuilder(11).put("com.aitutor.app.ui.camera.CameraViewModel", ((Provider) cameraViewModelProvider)).put("com.aitutor.app.ui.chat.ChatViewModel", ((Provider) chatViewModelProvider)).put("com.aitutor.app.ui.conversation.ConversationViewModel", ((Provider) conversationViewModelProvider)).put("com.aitutor.app.ui.screen.dashboard.DashboardViewModel", ((Provider) dashboardViewModelProvider)).put("com.aitutor.app.ui.auth.LoginViewModel", ((Provider) loginViewModelProvider)).put("com.aitutor.app.ui.profile.ProfileViewModel", ((Provider) profileViewModelProvider)).put("com.aitutor.app.ui.screen.quiz.QuizViewModel", ((Provider) quizViewModelProvider)).put("com.aitutor.app.ui.screen.review.ReviewViewModel", ((Provider) reviewViewModelProvider)).put("com.aitutor.app.ui.settings.SettingsViewModel", ((Provider) settingsViewModelProvider)).put("com.aitutor.app.ui.splash.SplashViewModel", ((Provider) splashViewModelProvider)).put("com.aitutor.app.ui.screen.subscription.SubscriptionViewModel", ((Provider) subscriptionViewModelProvider)).build();
    }

    @Override
    public Map<String, Object> getHiltViewModelAssistedMap() {
      return Collections.<String, Object>emptyMap();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.aitutor.app.ui.camera.CameraViewModel 
          return (T) new CameraViewModel(singletonCImpl.chatRepositoryImplProvider.get(), singletonCImpl.solveRepositoryImplProvider.get(), ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 1: // com.aitutor.app.ui.chat.ChatViewModel 
          return (T) new ChatViewModel(singletonCImpl.chatRepositoryImplProvider.get(), singletonCImpl.voiceRepositoryImplProvider.get(), singletonCImpl.settingsRepositoryImplProvider.get(), (AuthRepository) ((Provider) singletonCImpl.authRepositoryImplProvider).get(), singletonCImpl.userProfileRepositoryProvider.get(), viewModelCImpl.processTeachingResponseUseCase(), singletonCImpl.solveRepositoryImplProvider.get(), ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 2: // com.aitutor.app.ui.conversation.ConversationViewModel 
          return (T) new ConversationViewModel(singletonCImpl.chatRepositoryImplProvider.get());

          case 3: // com.aitutor.app.ui.screen.dashboard.DashboardViewModel 
          return (T) new DashboardViewModel(singletonCImpl.analyticsRepositoryImplProvider.get(), singletonCImpl.gamificationRepositoryImplProvider.get());

          case 4: // com.aitutor.app.ui.auth.LoginViewModel 
          return (T) new LoginViewModel((AuthRepository) ((Provider) singletonCImpl.authRepositoryImplProvider).get());

          case 5: // com.aitutor.app.ui.profile.ProfileViewModel 
          return (T) new ProfileViewModel((AuthRepository) ((Provider) singletonCImpl.authRepositoryImplProvider).get());

          case 6: // com.aitutor.app.ui.screen.quiz.QuizViewModel 
          return (T) new QuizViewModel(viewModelCImpl.generateQuizUseCase(), viewModelCImpl.submitQuizUseCase());

          case 7: // com.aitutor.app.ui.screen.review.ReviewViewModel 
          return (T) new ReviewViewModel(singletonCImpl.wrongAnswerRepositoryImplProvider.get());

          case 8: // com.aitutor.app.ui.settings.SettingsViewModel 
          return (T) new SettingsViewModel(singletonCImpl.settingsRepositoryImplProvider.get(), singletonCImpl.cacheManagerProvider.get());

          case 9: // com.aitutor.app.ui.splash.SplashViewModel 
          return (T) new SplashViewModel(singletonCImpl.tokenManagerProvider.get());

          case 10: // com.aitutor.app.ui.screen.subscription.SubscriptionViewModel 
          return (T) new SubscriptionViewModel(singletonCImpl.subscriptionRepositoryImplProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends AiTutorApp_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    private Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    private ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle 
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends AiTutorApp_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    private ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }
  }

  private static final class SingletonCImpl extends AiTutorApp_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    private Provider<AiTutorDatabase> provideDatabaseProvider;

    private Provider<TokenManager> tokenManagerProvider;

    private Provider<OkHttpClient> provideOkHttpClientProvider;

    private Provider<Retrofit> provideRetrofitProvider;

    private Provider<AiTutorApi> provideAiTutorApiProvider;

    private Provider<AuthRepositoryImpl> authRepositoryImplProvider;

    private Provider<AuthInterceptor> provideAuthInterceptorProvider;

    private Provider<HttpLoggingInterceptor> provideLoggingInterceptorProvider;

    private Provider<ChatStreamApi> provideChatStreamApiProvider;

    private Provider<ChatRepositoryImpl> chatRepositoryImplProvider;

    private Provider<SolveApi> provideSolveApiProvider;

    private Provider<SolveRepositoryImpl> solveRepositoryImplProvider;

    private Provider<SpeechRecognizer> provideSpeechRecognizerProvider;

    private Provider<TextToSpeech> provideTextToSpeechProvider;

    private Provider<CloudAsrEngine> cloudAsrEngineProvider;

    private Provider<CloudTtsEngine> cloudTtsEngineProvider;

    private Provider<TtsAudioPlayer> ttsAudioPlayerProvider;

    private Provider<VoiceRepositoryImpl> voiceRepositoryImplProvider;

    private Provider<SettingsDataStore> settingsDataStoreProvider;

    private Provider<SettingsRepositoryImpl> settingsRepositoryImplProvider;

    private Provider<UserProfileRepository> userProfileRepositoryProvider;

    private Provider<AnalyticsDao> provideAnalyticsDaoProvider;

    private Provider<AnalyticsApi> provideAnalyticsApiProvider;

    private Provider<AnalyticsRepositoryImpl> analyticsRepositoryImplProvider;

    private Provider<AchievementDao> provideAchievementDaoProvider;

    private Provider<UserScoreDao> provideUserScoreDaoProvider;

    private Provider<ScoreLogDao> provideScoreLogDaoProvider;

    private Provider<GamificationEngine> gamificationEngineProvider;

    private Provider<GamificationApi> provideGamificationApiProvider;

    private Provider<GamificationRepositoryImpl> gamificationRepositoryImplProvider;

    private Provider<QuizApi> provideQuizApiProvider;

    private Provider<QuizRecordDao> provideQuizRecordDaoProvider;

    private Provider<PendingSubmissionDao> providePendingSubmissionDaoProvider;

    private Provider<QuizRepositoryImpl> quizRepositoryImplProvider;

    private Provider<WrongAnswerDao> provideWrongAnswerDaoProvider;

    private Provider<WrongAnswerRepositoryImpl> wrongAnswerRepositoryImplProvider;

    private Provider<CacheManager> cacheManagerProvider;

    private Provider<SubscriptionApi> provideSubscriptionApiProvider;

    private Provider<SubscriptionCacheDao> provideSubscriptionCacheDaoProvider;

    private Provider<Gson> provideGsonProvider;

    private Provider<SubscriptionRepositoryImpl> subscriptionRepositoryImplProvider;

    private SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    private ConversationDao conversationDao() {
      return DatabaseModule_ProvideConversationDaoFactory.provideConversationDao(provideDatabaseProvider.get());
    }

    private MessageDao messageDao() {
      return DatabaseModule_ProvideMessageDaoFactory.provideMessageDao(provideDatabaseProvider.get());
    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideDatabaseProvider = DoubleCheck.provider(new SwitchingProvider<AiTutorDatabase>(singletonCImpl, 1));
      this.tokenManagerProvider = DoubleCheck.provider(new SwitchingProvider<TokenManager>(singletonCImpl, 5));
      this.provideOkHttpClientProvider = new DelegateFactory<>();
      this.provideRetrofitProvider = DoubleCheck.provider(new SwitchingProvider<Retrofit>(singletonCImpl, 8));
      this.provideAiTutorApiProvider = DoubleCheck.provider(new SwitchingProvider<AiTutorApi>(singletonCImpl, 7));
      this.authRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<AuthRepositoryImpl>(singletonCImpl, 6));
      this.provideAuthInterceptorProvider = DoubleCheck.provider(new SwitchingProvider<AuthInterceptor>(singletonCImpl, 4));
      this.provideLoggingInterceptorProvider = DoubleCheck.provider(new SwitchingProvider<HttpLoggingInterceptor>(singletonCImpl, 9));
      DelegateFactory.setDelegate(provideOkHttpClientProvider, DoubleCheck.provider(new SwitchingProvider<OkHttpClient>(singletonCImpl, 3)));
      this.provideChatStreamApiProvider = DoubleCheck.provider(new SwitchingProvider<ChatStreamApi>(singletonCImpl, 2));
      this.chatRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<ChatRepositoryImpl>(singletonCImpl, 0));
      this.provideSolveApiProvider = DoubleCheck.provider(new SwitchingProvider<SolveApi>(singletonCImpl, 11));
      this.solveRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<SolveRepositoryImpl>(singletonCImpl, 10));
      this.provideSpeechRecognizerProvider = DoubleCheck.provider(new SwitchingProvider<SpeechRecognizer>(singletonCImpl, 13));
      this.provideTextToSpeechProvider = DoubleCheck.provider(new SwitchingProvider<TextToSpeech>(singletonCImpl, 14));
      this.cloudAsrEngineProvider = DoubleCheck.provider(new SwitchingProvider<CloudAsrEngine>(singletonCImpl, 15));
      this.cloudTtsEngineProvider = DoubleCheck.provider(new SwitchingProvider<CloudTtsEngine>(singletonCImpl, 16));
      this.ttsAudioPlayerProvider = DoubleCheck.provider(new SwitchingProvider<TtsAudioPlayer>(singletonCImpl, 17));
      this.voiceRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<VoiceRepositoryImpl>(singletonCImpl, 12));
      this.settingsDataStoreProvider = DoubleCheck.provider(new SwitchingProvider<SettingsDataStore>(singletonCImpl, 19));
      this.settingsRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<SettingsRepositoryImpl>(singletonCImpl, 18));
      this.userProfileRepositoryProvider = DoubleCheck.provider(new SwitchingProvider<UserProfileRepository>(singletonCImpl, 20));
      this.provideAnalyticsDaoProvider = DoubleCheck.provider(new SwitchingProvider<AnalyticsDao>(singletonCImpl, 22));
      this.provideAnalyticsApiProvider = DoubleCheck.provider(new SwitchingProvider<AnalyticsApi>(singletonCImpl, 23));
      this.analyticsRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<AnalyticsRepositoryImpl>(singletonCImpl, 21));
      this.provideAchievementDaoProvider = DoubleCheck.provider(new SwitchingProvider<AchievementDao>(singletonCImpl, 26));
      this.provideUserScoreDaoProvider = DoubleCheck.provider(new SwitchingProvider<UserScoreDao>(singletonCImpl, 27));
      this.provideScoreLogDaoProvider = DoubleCheck.provider(new SwitchingProvider<ScoreLogDao>(singletonCImpl, 28));
      this.gamificationEngineProvider = DoubleCheck.provider(new SwitchingProvider<GamificationEngine>(singletonCImpl, 25));
      this.provideGamificationApiProvider = DoubleCheck.provider(new SwitchingProvider<GamificationApi>(singletonCImpl, 29));
      this.gamificationRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<GamificationRepositoryImpl>(singletonCImpl, 24));
      this.provideQuizApiProvider = DoubleCheck.provider(new SwitchingProvider<QuizApi>(singletonCImpl, 31));
      this.provideQuizRecordDaoProvider = DoubleCheck.provider(new SwitchingProvider<QuizRecordDao>(singletonCImpl, 32));
      this.providePendingSubmissionDaoProvider = DoubleCheck.provider(new SwitchingProvider<PendingSubmissionDao>(singletonCImpl, 33));
      this.quizRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<QuizRepositoryImpl>(singletonCImpl, 30));
      this.provideWrongAnswerDaoProvider = DoubleCheck.provider(new SwitchingProvider<WrongAnswerDao>(singletonCImpl, 35));
      this.wrongAnswerRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<WrongAnswerRepositoryImpl>(singletonCImpl, 34));
      this.cacheManagerProvider = DoubleCheck.provider(new SwitchingProvider<CacheManager>(singletonCImpl, 36));
      this.provideSubscriptionApiProvider = DoubleCheck.provider(new SwitchingProvider<SubscriptionApi>(singletonCImpl, 38));
      this.provideSubscriptionCacheDaoProvider = DoubleCheck.provider(new SwitchingProvider<SubscriptionCacheDao>(singletonCImpl, 39));
      this.provideGsonProvider = DoubleCheck.provider(new SwitchingProvider<Gson>(singletonCImpl, 40));
      this.subscriptionRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<SubscriptionRepositoryImpl>(singletonCImpl, 37));
    }

    @Override
    public void injectAiTutorApp(AiTutorApp aiTutorApp) {
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return Collections.<Boolean>emptySet();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @SuppressWarnings("unchecked")
      @Override
      public T get() {
        switch (id) {
          case 0: // com.aitutor.app.data.repository.ChatRepositoryImpl 
          return (T) new ChatRepositoryImpl(singletonCImpl.conversationDao(), singletonCImpl.messageDao(), singletonCImpl.provideChatStreamApiProvider.get(), singletonCImpl.tokenManagerProvider.get(), singletonCImpl.provideAiTutorApiProvider.get(), ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 1: // com.aitutor.app.data.local.db.AiTutorDatabase 
          return (T) DatabaseModule_ProvideDatabaseFactory.provideDatabase(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 2: // com.aitutor.app.data.remote.api.ChatStreamApi 
          return (T) NetworkModule_ProvideChatStreamApiFactory.provideChatStreamApi(singletonCImpl.provideOkHttpClientProvider.get());

          case 3: // okhttp3.OkHttpClient 
          return (T) NetworkModule_ProvideOkHttpClientFactory.provideOkHttpClient(singletonCImpl.provideAuthInterceptorProvider.get(), singletonCImpl.provideLoggingInterceptorProvider.get());

          case 4: // com.aitutor.app.data.remote.interceptor.AuthInterceptor 
          return (T) NetworkModule_ProvideAuthInterceptorFactory.provideAuthInterceptor(singletonCImpl.tokenManagerProvider.get(), ((Provider) singletonCImpl.authRepositoryImplProvider));

          case 5: // com.aitutor.app.data.remote.interceptor.TokenManager 
          return (T) new TokenManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 6: // com.aitutor.app.data.repository.AuthRepositoryImpl 
          return (T) new AuthRepositoryImpl(singletonCImpl.provideAiTutorApiProvider.get(), singletonCImpl.tokenManagerProvider.get());

          case 7: // com.aitutor.app.data.remote.api.AiTutorApi 
          return (T) NetworkModule_ProvideAiTutorApiFactory.provideAiTutorApi(singletonCImpl.provideRetrofitProvider.get());

          case 8: // retrofit2.Retrofit 
          return (T) NetworkModule_ProvideRetrofitFactory.provideRetrofit(singletonCImpl.provideOkHttpClientProvider.get());

          case 9: // okhttp3.logging.HttpLoggingInterceptor 
          return (T) NetworkModule_ProvideLoggingInterceptorFactory.provideLoggingInterceptor();

          case 10: // com.aitutor.app.data.repository.SolveRepositoryImpl 
          return (T) new SolveRepositoryImpl(singletonCImpl.provideSolveApiProvider.get(), singletonCImpl.provideAiTutorApiProvider.get());

          case 11: // com.aitutor.app.data.remote.api.SolveApi 
          return (T) NetworkModule_ProvideSolveApiFactory.provideSolveApi(singletonCImpl.provideOkHttpClientProvider.get());

          case 12: // com.aitutor.app.data.repository.VoiceRepositoryImpl 
          return (T) new VoiceRepositoryImpl(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule), singletonCImpl.provideSpeechRecognizerProvider.get(), singletonCImpl.provideTextToSpeechProvider.get(), singletonCImpl.cloudAsrEngineProvider.get(), singletonCImpl.cloudTtsEngineProvider.get(), singletonCImpl.ttsAudioPlayerProvider.get());

          case 13: // android.speech.SpeechRecognizer 
          return (T) SpeechModule_ProvideSpeechRecognizerFactory.provideSpeechRecognizer(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 14: // android.speech.tts.TextToSpeech 
          return (T) SpeechModule_ProvideTextToSpeechFactory.provideTextToSpeech(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 15: // com.aitutor.app.data.media.CloudAsrEngine 
          return (T) new CloudAsrEngine(singletonCImpl.provideAiTutorApiProvider.get());

          case 16: // com.aitutor.app.data.media.CloudTtsEngine 
          return (T) new CloudTtsEngine(singletonCImpl.provideOkHttpClientProvider.get(), singletonCImpl.tokenManagerProvider.get());

          case 17: // com.aitutor.app.data.media.TtsAudioPlayer 
          return (T) new TtsAudioPlayer();

          case 18: // com.aitutor.app.data.repository.SettingsRepositoryImpl 
          return (T) new SettingsRepositoryImpl(singletonCImpl.settingsDataStoreProvider.get());

          case 19: // com.aitutor.app.data.remote.datastore.SettingsDataStore 
          return (T) new SettingsDataStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 20: // com.aitutor.app.data.repository.UserProfileRepository 
          return (T) new UserProfileRepository((AuthRepository) ((Provider) singletonCImpl.authRepositoryImplProvider).get());

          case 21: // com.aitutor.app.data.repository.AnalyticsRepositoryImpl 
          return (T) new AnalyticsRepositoryImpl(singletonCImpl.provideAnalyticsDaoProvider.get(), singletonCImpl.provideAnalyticsApiProvider.get());

          case 22: // com.aitutor.app.data.local.dao.AnalyticsDao 
          return (T) DatabaseModule_ProvideAnalyticsDaoFactory.provideAnalyticsDao(singletonCImpl.provideDatabaseProvider.get());

          case 23: // com.aitutor.app.data.remote.api.AnalyticsApi 
          return (T) NetworkModule_ProvideAnalyticsApiFactory.provideAnalyticsApi(singletonCImpl.provideRetrofitProvider.get());

          case 24: // com.aitutor.app.data.repository.GamificationRepositoryImpl 
          return (T) new GamificationRepositoryImpl(singletonCImpl.gamificationEngineProvider.get(), singletonCImpl.provideGamificationApiProvider.get());

          case 25: // com.aitutor.app.domain.engine.GamificationEngine 
          return (T) new GamificationEngine(singletonCImpl.provideAchievementDaoProvider.get(), singletonCImpl.provideUserScoreDaoProvider.get(), singletonCImpl.provideScoreLogDaoProvider.get());

          case 26: // com.aitutor.app.data.local.dao.AchievementDao 
          return (T) DatabaseModule_ProvideAchievementDaoFactory.provideAchievementDao(singletonCImpl.provideDatabaseProvider.get());

          case 27: // com.aitutor.app.data.local.dao.UserScoreDao 
          return (T) DatabaseModule_ProvideUserScoreDaoFactory.provideUserScoreDao(singletonCImpl.provideDatabaseProvider.get());

          case 28: // com.aitutor.app.data.local.dao.ScoreLogDao 
          return (T) DatabaseModule_ProvideScoreLogDaoFactory.provideScoreLogDao(singletonCImpl.provideDatabaseProvider.get());

          case 29: // com.aitutor.app.data.remote.api.GamificationApi 
          return (T) NetworkModule_ProvideGamificationApiFactory.provideGamificationApi(singletonCImpl.provideRetrofitProvider.get());

          case 30: // com.aitutor.app.data.repository.QuizRepositoryImpl 
          return (T) new QuizRepositoryImpl(singletonCImpl.provideQuizApiProvider.get(), singletonCImpl.provideQuizRecordDaoProvider.get(), singletonCImpl.providePendingSubmissionDaoProvider.get());

          case 31: // com.aitutor.app.data.remote.api.QuizApi 
          return (T) NetworkModule_ProvideQuizApiFactory.provideQuizApi(singletonCImpl.provideRetrofitProvider.get());

          case 32: // com.aitutor.app.data.local.dao.QuizRecordDao 
          return (T) DatabaseModule_ProvideQuizRecordDaoFactory.provideQuizRecordDao(singletonCImpl.provideDatabaseProvider.get());

          case 33: // com.aitutor.app.data.local.dao.PendingSubmissionDao 
          return (T) DatabaseModule_ProvidePendingSubmissionDaoFactory.providePendingSubmissionDao(singletonCImpl.provideDatabaseProvider.get());

          case 34: // com.aitutor.app.data.repository.WrongAnswerRepositoryImpl 
          return (T) new WrongAnswerRepositoryImpl(singletonCImpl.provideWrongAnswerDaoProvider.get(), new SpacedRepetitionEngine());

          case 35: // com.aitutor.app.data.local.dao.WrongAnswerDao 
          return (T) DatabaseModule_ProvideWrongAnswerDaoFactory.provideWrongAnswerDao(singletonCImpl.provideDatabaseProvider.get());

          case 36: // com.aitutor.app.data.local.CacheManager 
          return (T) new CacheManager(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 37: // com.aitutor.app.data.repository.SubscriptionRepositoryImpl 
          return (T) new SubscriptionRepositoryImpl(singletonCImpl.provideSubscriptionApiProvider.get(), singletonCImpl.provideSubscriptionCacheDaoProvider.get(), singletonCImpl.provideGsonProvider.get());

          case 38: // com.aitutor.app.data.remote.api.SubscriptionApi 
          return (T) NetworkModule_ProvideSubscriptionApiFactory.provideSubscriptionApi(singletonCImpl.provideRetrofitProvider.get());

          case 39: // com.aitutor.app.data.local.dao.SubscriptionCacheDao 
          return (T) DatabaseModule_ProvideSubscriptionCacheDaoFactory.provideSubscriptionCacheDao(singletonCImpl.provideDatabaseProvider.get());

          case 40: // com.google.gson.Gson 
          return (T) GsonModule_ProvideGsonFactory.provideGson();

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
