package entities;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "points")
public class PointEntity {

    @EmbeddedId
    private PointId id;

    @MapsId("functionId")
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "function_id", nullable = false)
    private FunctionEntity function;

    @Column(name = "y_value", nullable = false)
    private double yValue;

    public PointEntity() {
    }

    public PointEntity(FunctionEntity function, Double xValue, double yValue) {
        this.function = function;
        this.id = new PointId(function.getId(), xValue);
        this.yValue = yValue;
    }

    public PointId getId() {
        return id;
    }

    public void setId(PointId id) {
        this.id = id;
    }

    public FunctionEntity getFunction() {
        return function;
    }

    public void setFunction(FunctionEntity function) {
        this.function = function;
    }

    public Double getXValue() {
        return id != null ? id.getXValue() : null;
    }

    public void setXValue(Double xValue) {
        if (id == null) {
            id = new PointId();
        }
        id.setXValue(xValue);
    }

    public double getYValue() {
        return yValue;
    }

    public void setYValue(double yValue) {
        this.yValue = yValue;
    }

    // Convenience alias for tests expecting getY()/setY()
    public double getY() {
        return yValue;
    }

    public void setY(double y) {
        this.yValue = y;
    }
}


