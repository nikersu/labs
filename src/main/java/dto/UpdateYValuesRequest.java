package dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class UpdateYValuesRequest {
    @JsonProperty("yValues")
    private double[] yValues;

    public UpdateYValuesRequest() {
    }

    public double[] getYValues() {
        return yValues;
    }

    public void setYValues(double[] yValues) {
        this.yValues = yValues;
    }
}

