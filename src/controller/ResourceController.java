package controller;

import common.BusinessException;
import common.LogAspect;
import common.Result;
import entity.CampusResource;
import entity.User;
import service.ReserveRecordService;
import service.ResourceService;

import java.util.List;

/**
 * 资源控制器：只做参数透传与异常转换，权限与校验全部在 ResourceService
 */
public class ResourceController {

    private final ResourceService resourceService = ResourceService.getInstance();
    private final ReserveRecordService reserveService = ReserveRecordService.getInstance();

    public Result<CampusResource> add(String name, String type, String location,
                                      String desc, User operator) {
        return (Result<CampusResource>) LogAspect.invoke(resourceService, "add",
                new Class<?>[]{String.class, String.class, String.class, String.class, User.class},
                new Object[]{name, type, location, desc, operator}, operator.getUsername());
    }

    public Result<CampusResource> update(String id, String name, String location,
                                         String status, User operator) {
        return (Result<CampusResource>) LogAspect.invoke(resourceService, "update",
                new Class<?>[]{String.class, String.class, String.class, String.class, User.class},
                new Object[]{id, name, location, status, operator}, operator.getUsername());
    }

    public Result<Void> delete(String id, User operator) {
        if (isEmpty(id)) {
            return Result.fail("资源 ID 不能为空");
        }
        // 跨模块校验：有已通过的预约禁删（任务19 会下沉到 Service 组合）
        if (reserveService.isReferencedByReserve(id.trim())) {
            return Result.fail("该资源存在已通过的预约记录，禁止删除");
        }
        return (Result<Void>) LogAspect.invoke(resourceService, "delete",
                new Class<?>[]{String.class, User.class},
                new Object[]{id.trim(), operator}, operator.getUsername());
    }


    public Result<List<CampusResource>> queryAll(User operator){
        return resourceService.queryAll(operator);
    }

    public Result<List<CampusResource>> queryAvailable() {
        return resourceService.queryAvailable();
    }

    public Result<List<CampusResource>> searchByName(String keyword) {
        return resourceService.searchByName(keyword);
    }


    private boolean isEmpty(String s){
        return s == null || s.trim().isEmpty();
    }

}