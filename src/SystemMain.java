import service.ReserveRecordService;
import service.ResourceService;
import service.UserService;
import ui.console.LoginMenu;

/**
 * 程序入口：只负责启动，不写任何业务
 * 调用链：SystemMain -> LoginMenu -> UserService -> UserDao
 */

public class SystemMain {
    public static void main(String[] args) {
        UserService userService = new UserService();
        ResourceService resourceService = new ResourceService();
        ReserveRecordService reserveService = new ReserveRecordService(resourceService);

        LoginMenu loginMenu = new LoginMenu(userService, resourceService, reserveService);
        loginMenu.start();
    }

}
