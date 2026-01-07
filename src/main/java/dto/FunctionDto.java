package dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FunctionDto {
    private Long id;
    private String name;
    private String expression;
    private Long userId;
    private List<PointDto> points;
    
    @JsonProperty("xValues")
    private double[] xValues;
    
    @JsonProperty("yValues")
    private double[] yValues;
    
    private Integer count;
    
    @JsonProperty("isInsertable")
    private boolean isInsertable;
    
    @JsonProperty("isRemovable")
    private boolean isRemovable;

    public FunctionDto() {
    }

    public FunctionDto(Long id, String name, String expression, Long userId) {
        this.id = id;
        this.name = name;
        this.expression = expression;
        this.userId = userId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expression) {
        this.expression = expression;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public List<PointDto> getPoints() {
        return points;
    }

    public void setPoints(List<PointDto> points) {
        this.points = points;
    }

    @JsonProperty("xValues")
    public double[] getXValues() {
        return xValues;
    }

    @JsonProperty("xValues")
    public void setXValues(double[] xValues) {
        this.xValues = xValues;
    }

    @JsonProperty("yValues")
    public double[] getYValues() {
        return yValues;
    }

    @JsonProperty("yValues")
    public void setYValues(double[] yValues) {
        this.yValues = yValues;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    @JsonProperty("isInsertable")
    public boolean isInsertable() {
        return isInsertable;
    }

    public void setInsertable(boolean insertable) {
        this.isInsertable = insertable;
    }

    @JsonProperty("isRemovable")
    public boolean isRemovable() {
        return isRemovable;
    }

    public void setRemovable(boolean removable) {
        this.isRemovable = removable;
    }
}



