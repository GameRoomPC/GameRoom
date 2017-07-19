package system;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author LM. Garret (admin@gameroom.me)
 * @date 18/07/2017.
 */
public abstract class SchedulableTask<T> implements Runnable {
    /**
     * State before the task is being executed,it's idle
     */
    private final static int STATE_PENDING = 0;
    /**
     * When the task is being executed
     */
    private final static int STATE_RUNNING = 1;
    /**
     * When the task has finished its completion
     */
    private final static int STATE_SUCCEEDED = 2;
    /**
     * When the task is cancelled. A cancelled task will not execute again. It should be set to this state
     * only if the global scheduler or service should be cancelled
     */
    private final static int STATE_CANCELLED = 3;

    /**
     * When an uncatched exception occurs during the task execution. The {@link #exception} then holds the thrown exception.
     */
    private final static int STATE_FAILED = 4;

    /**
     * When the task is cancelled. A cancelled task will not execute again. It should be set to this state when it is not
     * needed to execute the task anymore
     */
    private final static int STATE_STOPPED = 5;

    /**
     * Current state of the thread
     */
    private int state = STATE_PENDING;


    //Actions to execute when the state is changed
    private Runnable onPending = null;
    private Runnable onRunning = null;
    private Runnable onSucceeded = null;
    private Runnable onCancelled = null;
    private Runnable onFailed = null;
    private Runnable onStopped = null;

    /**
     * Rate of the task execution
     */
    private long rate;

    /**
     * Delay after which the task is being executed
     */
    private long initialDelay;

    /**
     * Future of the scheduled task
     */
    private ScheduledFuture<?> future;

    /**
     * Holds the exception if the execution of the task fails
     */
    private Exception exception;

    /**
     * Result of the execution of the task
     */
    private T value;

    public SchedulableTask(long initialDelay, long rate) {
        this.initialDelay = initialDelay;
        this.rate = rate;
    }

    @Override
    public void run() {
        if (state != STATE_STOPPED && state != STATE_CANCELLED) {
            setState(STATE_RUNNING);
            exception = null;
            try {
                value = execute();
                setState(STATE_SUCCEEDED);
            } catch (Exception e) {
                exception = e;
                setState(STATE_FAILED);
            }
            setState(STATE_PENDING);
        }
    }

    /**
     * Schedules the current task on the given {@link ScheduledThreadPoolExecutor} with the given {@link #initialDelay}
     * and {@link #rate}.
     *
     * @param executor the executor we want to execute our task on
     */
    public void scheduleAtFixedRateOn(ScheduledThreadPoolExecutor executor) {
        setState(STATE_PENDING);
        future = executor.scheduleAtFixedRate(this, initialDelay, rate, TimeUnit.MILLISECONDS);
    }

    /**
     * Schedules the current task on the given {@link ScheduledThreadPoolExecutor} with the given {@link #initialDelay}
     * and {@link #rate} as fixed delay.
     *
     * @param executor the executor we want to execute our task on
     */
    public void scheduleAtFixedDelayOn(ScheduledThreadPoolExecutor executor) {
        setState(STATE_PENDING);
        future = executor.scheduleWithFixedDelay(this, initialDelay, rate, TimeUnit.MILLISECONDS);
    }

    /**
     * Cancels the task and switch its {@link #state} to {@link #STATE_CANCELLED}
     */
    public void cancel() {
        if (future != null) {
            future.cancel(true);
        }
        setState(STATE_CANCELLED);
    }

    /**
     * Stops the task and switch its {@link #state} to {@link #STATE_STOPPED}
     */
    public void stop() {
        if (future != null) {
            future.cancel(true);
        }
        setState(STATE_STOPPED);
    }

    /**
     * Executes the task
     *
     * @return the value computed at the end of the task or null if we don't care about that
     * @throws Exception any uncatched exception that might occur during the task's execution
     */
    abstract protected T execute() throws Exception;

    /**
     * @return The result value of the task's execution. Might be null !
     */
    public T getValue() {
        return value;
    }

    /**
     * @return the exception that occured if the task has failed. Might be null !
     */
    public Exception getException() {
        return exception;
    }

    /**
     * Changes the state of the task and runs the corresponding action
     *
     * @param state the state we want to switch the task to
     */
    private void setState(int state) {
        this.state = state;
        switch (state) {
            case STATE_PENDING:
                if (onPending != null) {
                    onPending.run();
                }
                break;
            case STATE_RUNNING:
                if (onRunning != null) {
                    onRunning.run();
                }
                break;
            case STATE_SUCCEEDED:
                if (onSucceeded != null) {
                    onSucceeded.run();
                }
                break;
            case STATE_CANCELLED:
                if (onCancelled != null) {
                    onCancelled.run();
                }
                break;
            case STATE_FAILED:
                if (onFailed != null) {
                    onFailed.run();
                }
                break;
            case STATE_STOPPED:
                if (onStopped != null) {
                    onStopped.run();
                }
                break;
            default:
                break;
        }
    }

    public void setOnPending(Runnable onPending) {
        this.onPending = onPending;
    }

    public void setOnRunning(Runnable onRunning) {
        this.onRunning = onRunning;
    }

    public void setOnSucceeded(Runnable onSucceeded) {
        this.onSucceeded = onSucceeded;
    }

    public void setOnCancelled(Runnable onCancelled) {
        this.onCancelled = onCancelled;
    }

    public void setOnFailed(Runnable onFailed) {
        this.onFailed = onFailed;
    }

    public void setOnStopped(Runnable onStopped) {
        this.onStopped = onStopped;
    }

    public long getRate() {
        return rate;
    }

    public void setRate(long rate) {
        this.rate = rate;
    }

    public long getInitialDelay() {
        return initialDelay;
    }

    public void setInitialDelay(long initialDelay) {
        this.initialDelay = initialDelay;
    }
}
