package dao;

import entity.User;
import exception.BusinessException;
import utils.FileUtil;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问层：只负责user.txt的读写，不做业务逻辑处理
 */
public class UserDao {

    /** 饿汉单例：类加载即创建，JVM 保证线程安全 */
    private static final UserDao INSTANCE = new UserDao();

    public static UserDao getInstance(){
        return INSTANCE;
    }

    /** 私有构造：禁止外部 new，创建动作只能走 getInstance() */
    private UserDao() {
    }

    private static final String FILE = "user.txt";
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 加载全部用户；文件缺失自动创建，脏行跳过 */
    public List<User> loadAll() {
        List<User> list = new ArrayList<>();
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
                if (a.length < 4) {
                    continue;
                }
                LocalDateTime time = a[3].isEmpty() ? null : LocalDateTime.parse(a[3], FMT);
                list.add(new User(a[0], a[1], a[2], time));
            }
        } catch (IOException e) {
            throw new BusinessException("加载用户数据失败", e);
        }
        return list;
    }

    /** 全量覆盖写回 */
    public void saveAll(List<User> users) {
        FileUtil.ensureFile(FILE);
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(FILE), StandardCharsets.UTF_8))) {
            for (User u : users) {
                bw.write(u.getUsername() + "," + u.getPassword() + ","
                        + u.getRole() + "," + u.formatRegisterTime());
                bw.newLine();
            }
            bw.flush();
        } catch (IOException e) {
            throw new BusinessException("保存用户数据失败", e);
        }
    }
    /** 追加单个用户（内部转成全量写，保持实现简单） */
    public void append(User user, List<User> all) {
        saveAll(all);
    }

    /** 按用户名查（Optional 表达可能不存在） */
    public Optional<User> findByUsername(String username, List<User> all) {
        return all.stream().filter(u -> u.getUsername().equals(username)).findFirst();
    }

}
