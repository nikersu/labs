package dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PointDto {
    @JsonProperty("functionId")
    private Long functionId;
    
    @JsonProperty("xValue")
    private Double xValue;
    
    @JsonProperty("yValue")
    private Double yValue;

    public PointDto() {
    }

    public PointDto(Long functionId, Double xValue, Double yValue) {
        this.functionId = functionId;
        this.xValue = xValue;
        this.yValue = yValue;
    }

    public Long getFunctionId() {
        return functionId;
    }

    public void setFunctionId(Long functionId) {
        this.functionId = functionId;
    }

    public Double getXValue() {
        return xValue;
    }

    public void setXValue(Double xValue) {
        this.xValue = xValue;
    }

    public Double getYValue() {
        return yValue;
    }

    public void setYValue(Double yValue) {
        this.yValue = yValue;
    }
}



