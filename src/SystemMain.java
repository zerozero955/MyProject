import service.ReserveRecordService;
import service.ResourceService;
import service.UserService;
import view.MainView;

/**
 * 程序入口：只负责启动，不写任何业务
 * 调用链：SystemMain -> LoginMenu -> UserService -> UserDao
 */

public class SystemMain {
    public static void main(String[] args) {
        // 三个 Service 都是饿汉单例：这里只"取"，全程序不再出现 new XxxService()
        UserService userService = UserService.getInstance();
        ResourceService resourceService = ResourceService.getInstance();
        ReserveRecordService reserveService = ReserveRecordService.getInstance();

        // 入口只启动视图，业务全部由 view -> controller -> service 完成
        new MainView().start();

    }

}
