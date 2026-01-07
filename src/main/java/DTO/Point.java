package DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Point {
    @JsonProperty("functionId")
    private Integer functionId;
    
    @JsonProperty("xValue")
    private Double xValue;
    
    @JsonProperty("yValue")
    private Double yValue;

    public Point() {}

    public Point(Integer functionId, Double xValue, Double yValue) {
        this.functionId = functionId;
        this.xValue = xValue;
        this.yValue = yValue;
    }

    // геттеры и сеттеры
    public Integer getFunctionId() { return functionId; }
    public void setFunctionId(Integer functionId) { this.functionId = functionId; }

    public Double getXValue() { return xValue; }
    public void setXValue(Double xValue) { this.xValue = xValue; }

    public Double getYValue() { return yValue; }
    public void setYValue(Double yValue) { this.yValue = yValue; }

    @Override
    public String toString() {
        return "Point{functionId=" + functionId + ", x=" + xValue + ", y=" + yValue + "}";
    }
}