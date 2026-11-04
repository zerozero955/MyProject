package entity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 用户实体：只承载数据，不包含任何业务逻辑（封装原则 / 单一职责）
 */
public class User {

    private String username;
    private String password;
    private String role;
    private LocalDateTime registerTime;

    public User() {
    }

    public User(String username, String password, String role, LocalDateTime registerTime) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.registerTime = registerTime;
    }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public LocalDateTime getRegisterTime() { return registerTime; }
    public void setRegisterTime(LocalDateTime registerTime) { this.registerTime = registerTime; }

    /** 打印时不输出密码*/
    @Override
    public String toString() {
        return "entity.User{username='" + username + "', role='" + role
                + "', registerTime=" + registerTime + "}";
    }

    /** 文件持久化用的格式化时间 */
    public String formatRegisterTime() {
        return registerTime == null ? ""
                : registerTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
