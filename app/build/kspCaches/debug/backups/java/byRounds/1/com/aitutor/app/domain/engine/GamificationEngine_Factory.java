package com.aitutor.app.domain.engine;

import com.aitutor.app.data.local.dao.AchievementDao;
import com.aitutor.app.data.local.dao.ScoreLogDao;
import com.aitutor.app.data.local.dao.UserScoreDao;
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
public final class GamificationEngine_Factory implements Factory<GamificationEngine> {
  private final Provider<AchievementDao> achievementDaoProvider;

  private final Provider<UserScoreDao> userScoreDaoProvider;

  private final Provider<ScoreLogDao> scoreLogDaoProvider;

  public GamificationEngine_Factory(Provider<AchievementDao> achievementDaoProvider,
      Provider<UserScoreDao> userScoreDaoProvider, Provider<ScoreLogDao> scoreLogDaoProvider) {
    this.achievementDaoProvider = achievementDaoProvider;
    this.userScoreDaoProvider = userScoreDaoProvider;
    this.scoreLogDaoProvider = scoreLogDaoProvider;
  }

  @Override
  public GamificationEngine get() {
    return newInstance(achievementDaoProvider.get(), userScoreDaoProvider.get(), scoreLogDaoProvider.get());
  }

  public static GamificationEngine_Factory create(Provider<AchievementDao> achievementDaoProvider,
      Provider<UserScoreDao> userScoreDaoProvider, Provider<ScoreLogDao> scoreLogDaoProvider) {
    return new GamificationEngine_Factory(achievementDaoProvider, userScoreDaoProvider, scoreLogDaoProvider);
  }

  public static GamificationEngine newInstance(AchievementDao achievementDao,
      UserScoreDao userScoreDao, ScoreLogDao scoreLogDao) {
    return new GamificationEngine(achievementDao, userScoreDao, scoreLogDao);
  }
}
