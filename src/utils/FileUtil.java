package utils;

import entity.User;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件持久化工具类：自动创建、读取、写回
 * 设计原则：迪米特法则——业务代码不直接接触 IO 流，只调用本类
 * 约束：私有构造，禁止实例化；所有方法 public static
 */
public class FileUtil {

    private FileUtil() {
    }

    public static final String USER_FILE = "user.txt";
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 文件不存在则自动创建（含父目录） */
    public static void ensureFile(String path) {
        File file = new File(path);
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
        } catch (IOException e) {
            throw new RuntimeException("创建数据文件失败：" + path, e);
        }
    }

    /** 读取 user.txt 到 List<entity.User>；文件为空返回空集合 */
    public static List<entity.User> readUsers() {
        List<User> users = new ArrayList<>();
        File file = new File(USER_FILE);
        ensureFile(USER_FILE);
        // try-with-resources 自动关闭流
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;                       // 跳过空行
                }
                String[] arr = line.split(",");
                if (arr.length < 4) {
                    continue;                       // 字段缺失的行直接跳过，保证不崩溃
                }
                LocalDateTime time = arr[3].isEmpty() ? null : LocalDateTime.parse(arr[3], FMT);
                users.add(new User(arr[0], arr[1], arr[2], time));
            }
        } catch (IOException e) {
            throw new RuntimeException("读取用户数据失败", e);
        }
        return users;
    }

    /** 全量写回用户列表：一行一个用户，字段逗号分隔 */
    public static void writeUsers(List<User> users) {
        File file = new File(USER_FILE);
        ensureFile(USER_FILE);
        try (BufferedWriter bw = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            for (User u : users) {
                bw.write(u.getUsername() + "," + u.getPassword() + ","
                        + u.getRole() + "," + u.formatRegisterTime());
                bw.newLine();
            }
            bw.flush();
        } catch (IOException e) {
            throw new RuntimeException("保存用户数据失败", e);
        }
    }
}
