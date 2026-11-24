package dao;


import entity.ReserveRecord;
import entity.ReserveStatus;
import exception.BusinessException;
import utils.FileUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 预约数据访问层：reserve.txt 读写（任务11 起为 10 列）
 * recordId,resourceId,applicant,applyTime,start,end,status,remark,rejectReason,auditTime
 */


public class ReserveRecordDao {

    /** 饿汉单例：类加载即创建，JVM 保证线程安全 */
    private static final ReserveRecordDao INSTANCE = new ReserveRecordDao();

    public static ReserveRecordDao getInstance() {
        return INSTANCE;
    }

    /** 私有构造：禁止外部 new，创建动作只能走 getInstance() */
    private ReserveRecordDao() {
    }

    private static final String FILE = "reserve.txt";

    public List<ReserveRecord> loadAll() {
        List<ReserveRecord> list = new ArrayList<>();
        FileUtil.ensureFile(FILE);
        File file = new File(FILE);
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                String[] a = line.split(",", -1);
                if (a.length < 8) {
                    continue;
                }
                ReserveRecord r = new ReserveRecord(a[0], a[1], a[2],
                        LocalDateTime.parse(a[3]), LocalDateTime.parse(a[4]),
                        LocalDateTime.parse(a[5]), ReserveStatus.valueOf(a[6]), a[7]);
                if (a.length > 8 && !a[8].isEmpty()) {
                    r.setRejectReason(a[8]);
                }
                if (a.length > 9 && !a[9].isEmpty()) {
                    r.setAuditTime(LocalDateTime.parse(a[9]));
                }
                list.add(r);

            }
        } catch (IOException e) {
            throw new BusinessException("加载预约数据失败", e);
        }
        return list;
    }

    public void saveAll(List<ReserveRecord> list) {
        FileUtil.ensureFile(FILE);
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(FILE), StandardCharsets.UTF_8))) {
            for (ReserveRecord r : list) {
                bw.write(r.getRecordId() + "," + r.getResourceId() + ","
                        + r.getApplicantUsername() + "," + r.getApplyTime() + ","
                        + r.getReserveStartTime() + "," + r.getReserveEndTime() + ","
                        + r.getStatus().name() + ","
                        + (r.getRemark() == null ? "" : r.getRemark()) + ","
                        + (r.getRejectReason() == null ? "" : r.getRejectReason()) + ","
                        + (r.getAuditTime() == null ? "" : r.getAuditTime()));

                bw.newLine();
            }
            bw.flush();
        } catch (IOException e) {
            throw new BusinessException("保存预约数据失败", e);
        }
    }

}
