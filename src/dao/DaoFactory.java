package dao;

/**
 * DAO 简单工厂：统一生产数据访问对象
 * 现状：只生产文件版 DAO；任务19 会按 application.properties 的 storageType 切换文件/数据库实现
 * 约束：Service 不再出现 new UserDao()，一律通过本工厂取
 */
public class DaoFactory {
    private DaoFactory(){
    }

    public static UserDao getUserDao(){
        return UserDao.getInstance();
    }

    public static ResourceDao getResourceDao(){
        return ResourceDao.getInstance();
    }

    public static ReserveRecordDao getReserveRecordDao(){
        return ReserveRecordDao.getInstance();
    }
}
