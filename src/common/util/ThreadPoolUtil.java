package common.util;

import java.util.concurrent.*;

/**
 * 自定义线程池工具类（任务书要求：不用 Executors，手写参数）
 * 用途：① 异步写日志 ② 异步跑统计
 */
public class ThreadPoolUtil {

    private static final ThreadPoolExecutor POOL = new ThreadPoolExecutor(
            2,                                  // 核心线程数
            4,                                  // 最大线程数
            60L, TimeUnit.SECONDS,              // 空闲存活时间
            new ArrayBlockingQueue<>(100),      // 有界队列，防止任务堆积 OOM
            new ThreadPoolExecutor.CallerRunsPolicy());   // 队列满时由调用线程执行，不丢任务

    private ThreadPoolUtil(){}

    public static void submit(Runnable task){
        POOL.execute(task);
    }

    public static <T> Future<T> submit(Callable<T> task) {
        return POOL.submit(task);
    }

    /** 关闭线程池，等待已提交任务执行完 */
    public static void shutdown() {
        POOL.shutdown();
        try {
            if (!POOL.awaitTermination(5, TimeUnit.SECONDS)) {
                POOL.shutdownNow();
            }
        } catch (InterruptedException e) {
            POOL.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
