package controller;

import common.BusinessException;
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
        return resourceService.add(name, type, location, desc, operator);
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

    public Result<CampusResource> update(String id, String name, String location,
                                        String status, User operator) {
        return resourceService.update(id, name, location, status, operator);
    }

    /**
     * 删除资源：删除前必须先问预约模块"这个资源有没有已通过的预约"
     * 说明：跨模块的这道校验暂时由控制层编排，是已知的小瑕疵；任务19 会把它下沉到 Service 组合
     */
    public Result<Void> delete(String id, User operator){
        if (isEmpty(id)){
            return Result.fail("资源ID不能为空");
        }
        try {
            if (reserveService.isReferencedByReserve(id.trim())){
                return Result.fail("这个资源有预约记录，不能删除");
            }
            return resourceService.delete(id.trim(), operator);
        } catch (BusinessException e){
            return Result.fail(e.getMessage());
        }
    }

    private boolean isEmpty(String s){
        return s == null || s.trim().isEmpty();
    }
}