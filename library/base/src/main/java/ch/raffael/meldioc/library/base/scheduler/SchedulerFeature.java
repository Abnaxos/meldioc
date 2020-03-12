package ch.raffael.meldioc.library.base.scheduler;

import ch.raffael.meldioc.Feature;
import ch.raffael.meldioc.Parameter;
import ch.raffael.meldioc.Provision;
import ch.raffael.meldioc.library.base.lifecycle.ShutdownFeature;
import ch.raffael.meldioc.library.base.threading.ThreadingFeature;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.Executor;

@Feature
public interface SchedulerFeature {

  @Provision
  Scheduler scheduler();

  @Feature
  @Parameter.Prefix("scheduler")
  abstract class Default implements SchedulerFeature {

    @Provision(shared = true)
    @Override
    public Scheduler scheduler() {
      return DefaultScheduler.withExecutor(executor())
          .clock(clock())
          .readjustDuration(readjustDuration())
          .earlyRunTolerance(earlyRunTolerance())
          .build();
    }

    @Parameter
    protected Duration earlyRunTolerance() {
      return DefaultScheduler.DEFAULT_EARLY_RUN_TOLERANCE;
    }

    @Parameter
    protected Duration lateRunTolerance() {
      return DefaultScheduler.DEFAULT_LATE_RUN_TOLERANCE;
    }

    @Parameter
    protected Duration readjustDuration() {
      return DefaultScheduler.DEFAULT_READJUST_DURATION;
    }

    protected abstract Executor executor();

    protected Clock clock() {
      return Clock.systemDefaultZone();
    }
  }

  @Feature
  abstract class WithShutdown extends Default implements ShutdownFeature {
    @Provision(shared = true)
    @Override
    public Scheduler scheduler() {
      var scheduler = (DefaultScheduler) super.scheduler();
      shutdownController().onPrepare(scheduler::shutdown);
      return scheduler;
    }
  }

  @Feature
  abstract class WithThreading extends Default implements ThreadingFeature {
    @Override
    protected Executor executor() {
      return workExecutor();
    }
  }

  @Feature
  abstract class WithThreadingAndShutdown extends WithShutdown implements ThreadingFeature {
    @Override
    protected Executor executor() {
      return workExecutor();
    }
  }
}
