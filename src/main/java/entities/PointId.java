package entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class PointId implements Serializable {

    @Column(name = "function_id")
    private Long functionId;

    @Column(name = "x_value")
    private Double xValue;

    public PointId() {
    }

    public PointId(Long functionId, Double xValue) {
        this.functionId = functionId;
        this.xValue = xValue;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PointId pointId = (PointId) o;
        return Objects.equals(functionId, pointId.functionId) && Objects.equals(xValue, pointId.xValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(functionId, xValue);
    }
}

