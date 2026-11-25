package entity;

import enums.ResourceStatus;

/**
 * 校园资源实体：只承载数据，不写业务逻辑*/
public class CampusResource {

    private String id;                //资源统一编号
    private String name;              //资源名称
    private String type;              //资源类型
    private String description;       //资源描述
    private String location;          //资源位置
    private ResourceStatus status;    //资源状态

    public CampusResource() {
    }

    public CampusResource(String id, String name, String type, ResourceStatus status,
                          String location, String description)
    {
        this.id = id;
        this.name = name;
        this.type = type;
        this.description = description;
        this.location = location;
        this.status = status;
    }

    public String getId(){
        return id;
    }
    public void setId(String id){
        this.id = id;
    }

    public String getName(){
        return name;
    }
    public void setName(String name){
        this.name = name;
    }
    public String getType(){
        return type;
    }
    public void setType(String type){
        this.type = type;
    }

    public String getDescription(){
        return description;
    }
    public void setDescription(String description){
        this.description = description;
    }

    public String getLocation(){
        return location;
    }
    public void setLocation(String location){
        this.location = location;
    }

    public ResourceStatus getStatus(){
        return status;
    }
    public void setStatus(ResourceStatus status){
        this.status = status;
    }

    @Override
    public String toString(){
        return id + " | " + name + " | " + type + " | "
                + (status == null ? "" : status.getDesc()) + " | " + location
                + (description == null || description.isEmpty() ? "" : " | " + description);
    }
}
