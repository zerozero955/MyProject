package utils;

import common.Result;
import common.util.ThreadPoolUtil;
import entity.ReserveRecord;
import entity.User;
import service.ReserveRecordService;
import service.ResourceService;

import java.time.LocalDateTime;

/**
 * 模拟多用户并发预约（任务书要求：验证加锁后数据不错乱）
 * 用法：先准备一个可预约资源，然后运行本类，观察 5 个线程提交后记录条数是否完整
 * 说明：这是演示/验证类，不属于业务代码，交付时可删
 */
public class ConcurrentReserveDemo {
    private static final int THREADS = 5;

    public static void main(String[] args) throws Exception {
        ResourceService resourceService = ResourceService.getInstance();
        ReserveRecordService reserveService = ReserveRecordService.getInstance();
        User student = new User("student", "123456", "student", LocalDateTime.now());

        Thread[] threads = new Thread[THREADS];
        for (int i = 0; i < THREADS; i++) {
            final int no = i + 1;
            threads[i] = new Thread(() -> {
                LocalDateTime start = LocalDateTime.now().plusDays(no);
                LocalDateTime end = start.plusHours(1);
                Result<ReserveRecord> r = reserveService.submit("R001", start, end,
                        "并发测试-" + no, student);
                System.out.println("线程" + no + " -> " + r.getMsg());
            }, "user-" + no);
        }
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }

        Result<java.util.List<ReserveRecord>> all = reserveService.listMine(student);
        System.out.println("并发提交后，我的预约条数 = " + all.getData().size());
        ThreadPoolUtil.shutdown();
    }
}
