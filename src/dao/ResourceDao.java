package dao;


import entity.CampusResource;
import entity.ResourceStatus;
import exception.BusinessException;
import utils.FileUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * 资源数据访问层：只负责 resource.txt 的读写
 */

public class ResourceDao {
    private static final String FILE = "resource.txt";

    public List<CampusResource> loadAll() {
        List<CampusResource> list = new ArrayList<>();
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
                if (a.length < 6) {
                    continue;                       // 字段缺失的脏行跳过，不让程序崩
                }
                list.add(new CampusResource(a[0], a[1], a[2],
                        ResourceStatus.valueOf(a[3]), a[4], a[5]));
            }
        } catch (IOException e) {
            throw new BusinessException("加载资源数据失败", e);
        }
        return list;
    }
    public void saveAll(List<CampusResource> list) {
        FileUtil.ensureFile(FILE);
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(FILE), StandardCharsets.UTF_8))) {
            for (CampusResource r : list) {
                bw.write(r.getId() + "," + r.getName() + "," + r.getType() + ","
                        + r.getStatus().name() + "," + r.getLocation() + ","
                        + (r.getDescription() == null ? "" : r.getDescription()));
                bw.newLine();
            }
            bw.flush();
        } catch (IOException e) {
            throw new BusinessException("保存资源数据失败", e);
        }
    }
}
