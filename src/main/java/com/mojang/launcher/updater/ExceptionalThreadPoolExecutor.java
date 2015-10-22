package com.mojang.launcher.updater;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ExceptionalThreadPoolExecutor extends ThreadPoolExecutor
{
  private static final Logger LOGGER;
  
  public ExceptionalThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit)
  {
    super(corePoolSize, maximumPoolSize, keepAliveTime, unit, new LinkedBlockingQueue());
  }
  
  protected void afterExecute(Runnable r, Throwable t)
  {
    super.afterExecute(r, t);
    if ((t == null) && ((r instanceof Future))) {
      try
      {
        Future<?> future = (Future)r;
        if (future.isDone()) {
          future.get();
        }
      }
      catch (CancellationException ce)
      {
        t = ce;
      }
      catch (ExecutionException ee)
      {
        t = ee.getCause();
      }
      catch (InterruptedException ie)
      {
        Thread.currentThread().interrupt();
      }
    }
  }
  
  @Override
  protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value)
  {
    return new ExceptionalFutureTask(runnable, value);
  }
  
  @Override
  protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable)
  {
    return new ExceptionalFutureTask(callable);
  }
  
  public class ExceptionalFutureTask<T> extends FutureTask<T>
  {
	  public ExceptionalFutureTask(final Callable<T> callable) {
          super(callable);
      }
      
      public ExceptionalFutureTask(final Runnable runnable, final T result) {
          super(runnable, result);
      }
    
    protected void done()
    {
      try
      {
        get();
      }
      catch (Throwable t)
      {
        ExceptionalThreadPoolExecutor.LOGGER.error("Unhandled exception in executor " + this, t);
      }
    }
  }
  static {
      LOGGER = LogManager.getLogger();
  }
}