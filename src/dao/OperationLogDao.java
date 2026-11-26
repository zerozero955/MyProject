package dao;

import common.util.ThreadPoolUtil;
import entity.OperationLogRecord;
import utils.FileUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 日志数据访问层：内存集合 + operation_log.txt 持久化
 * 与其它 DAO 不同点：日志是"只追加"，所以 append 用追加模式写文件
 */

public class OperationLogDao {

    private static final OperationLogDao INSTANCE = new OperationLogDao();
    public static OperationLogDao getInstance(){
        return INSTANCE;
    }
    private static final String FILE = "operation_log.txt";
    private final List<OperationLogRecord> logs = new ArrayList<>();
    private static final Object FILE_LOCK = new Object();

    private OperationLogDao() {
        this.logs.addAll(loadAll());
    }


    /** 追加一条日志：内存立即加锁写入，文件交给线程池异步落盘，主线程不等磁盘 */
    public void append(OperationLogRecord log) {
        synchronized (logs) {
            logs.add(log);
        }
        ThreadPoolUtil.submit(() -> writeToFile(log));

    }

    /** 写文件加锁：多线程同时写会串行，内容不会交错、不会覆盖 */
    private void writeToFile(OperationLogRecord log) {
        synchronized (FILE_LOCK) {
            FileUtil.ensureFile(FILE);
            try (BufferedWriter bw = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(FILE, true), StandardCharsets.UTF_8))) {
                bw.write(toLine(log));
                bw.newLine();
                bw.flush();
            } catch (IOException e) {
                System.out.println("[提示] 操作日志写入失败");
            }
        }
    }


    public List<OperationLogRecord> findAll() {
        synchronized (logs) {
            return new ArrayList<>(logs);
        }
    }

    private List<OperationLogRecord> loadAll() {
        List<OperationLogRecord> list = new ArrayList<>();
        FileUtil.ensureFile(FILE);
        File file = new File(FILE);
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] a = line.split("\\|", -1);
                if (a.length < 8) {
                    continue;               // 脏行跳过，保证启动不失败
                }
                list.add(new OperationLogRecord(a[0], a[1],
                        a[2].isEmpty() ? null : LocalDateTime.parse(a[2]),
                        a[3], a[4], a[5], a[6], a[7]));
            }
        } catch (IOException e) {
            System.out.println("[提示] 历史日志读取失败，按空日志处理");
        }
        return list;
    }

    private String toLine(OperationLogRecord log) {
        return String.join("|", log.getLogId(),
                log.getUsername() == null ? "" : log.getUsername(),
                log.getOperateTime() == null ? "" : log.getOperateTime().toString(),
                log.getMethodFullName() == null ? "" : log.getMethodFullName(),
                log.getOperationDesc() == null ? "" : log.getOperationDesc(),
                log.getParams() == null ? "" : log.getParams(),
                log.getResultInfo() == null ? "" : log.getResultInfo(),
                log.getExceptionMsg() == null ? "" : log.getExceptionMsg());
    }

}
