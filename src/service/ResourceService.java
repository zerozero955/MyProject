package service;

import common.Result;
import dao.DaoFactory;
import dao.ResourceDao;
import entity.CampusResource;
import entity.ResourceStatus;
import entity.User;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 资源业务层：承接权限、字段校验、Stream 查询；依赖 ResourceDao
 */

public class ResourceService {

    /** 饿汉单例：资源数据只加载一次，避免多份内存副本 */
    private static final ResourceService INSTANCE = new ResourceService();

    public static ResourceService getInstance() {
        return INSTANCE;
    }

    private final ResourceDao resourceDao;
    private final List<CampusResource> resources;

    private ResourceService() {
        this.resourceDao = DaoFactory.getResourceDao();
        this.resources = resourceDao.loadAll();
    }

    public Result<CampusResource> add(String name, String type, String location,
                                      String description, User operator) {
        if (!isAdmin(operator)) {
            return Result.fail("该操作仅管理员可执行");
        }
        if (name == null || name.trim().isEmpty()) {
            return Result.fail("资源名称不能为空");
        }
        CampusResource res = new CampusResource(nextId(), name.trim(), type,
                ResourceStatus.AVAILABLE, location, description);
        resources.add(res);
        resourceDao.saveAll(resources);
        return Result.success("资源新增成功", res);
    }

    public Result<List<CampusResource>> queryAll(User operator) {
        if (!isAdmin(operator)) {
            return Result.fail("该操作仅管理员可执行");
        }
        return Result.success("共 " + resources.size() + " 条资源", sortedById());
    }

    public Result<List<CampusResource>> queryAvailable() {
        List<CampusResource> list = resources.stream()
                .filter(r -> ResourceStatus.AVAILABLE == r.getStatus())
                .sorted(Comparator.comparing(CampusResource::getId))
                .collect(Collectors.toList());
        return Result.success("共 " + list.size() + " 条可预约资源", list);
    }

    public Result<List<CampusResource>> searchByName(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.fail("查询关键词不能为空");
        }
        String kw = keyword.trim().toLowerCase();
        List<CampusResource> list = resources.stream()
                .filter(r -> r.getName() != null && r.getName().toLowerCase().contains(kw))
                .sorted(Comparator.comparing(CampusResource::getId))
                .collect(Collectors.toList());
        return Result.success("共匹配到 " + list.size() + " 条资源", list);
    }

    public Result<CampusResource> update(String id, String name, String location,
                                        String statusText, User operator) {
        if (!isAdmin(operator)) {
            return Result.fail("该操作仅管理员可执行");
        }
        Optional<CampusResource> opt = findById(id);
        if (!opt.isPresent()) {
            return Result.fail("未找到 ID 为 " + id + " 的资源");
        }
        CampusResource target = opt.get();
        if (name != null && !name.trim().isEmpty()) {
            target.setName(name.trim());
        }
        if (location != null && !location.trim().isEmpty()) {
            target.setLocation(location.trim());
        }
        if (statusText != null && !statusText.trim().isEmpty()) {
            try {
                target.setStatus(ResourceStatus.of(statusText.trim()));
            } catch (IllegalArgumentException e) {
                return Result.fail(e.getMessage());
            }
        }
        resourceDao.saveAll(resources);
        return Result.success("资源修改成功", target);
    }

    public Result<Void> delete(String id, User operator) {
        if (!isAdmin(operator)) {
            return Result.fail("该操作仅管理员可执行");
         }
        Optional<CampusResource> opt = findById(id);
         if (!opt.isPresent()) {
            return Result.fail("未找到 ID 为 " + id + " 的资源");
         }
        resources.remove(opt.get());
        resourceDao.saveAll(resources);
        return Result.success("资源删除成功");
    }

    /** 供预约模块联动：通过 / 取消预约时改资源状态 */
    public Result<Void> updateStatus(String id, ResourceStatus status) {
        Optional<CampusResource> opt = findById(id);
        if (!opt.isPresent()) {
            return Result.fail("未找到该资源");
        }
        opt.get().setStatus(status);
        resourceDao.saveAll(resources);
        return Result.success("资源状态已更新为" + status.getDesc());
    }

    public Optional<CampusResource> findById(String id) {
        if (id == null || id.trim().isEmpty()) {
            return Optional.empty();
        }
        return resources.stream().filter(r -> r.getId().equals(id.trim())).findFirst();
    }

    private List<CampusResource> sortedById() {
        return resources.stream()
                .sorted(Comparator.comparing(CampusResource::getId))
                .collect(Collectors.toList());
    }

    private boolean isAdmin(User operator) {
        return operator != null && "admin".equals(operator.getRole());
    }

    private String nextId() {
        int max = resources.stream()
                .mapToInt(r -> Integer.parseInt(r.getId().substring(1)))
                .max()
                .orElse(0);
        return String.format("R%03d", max + 1);
    }
}