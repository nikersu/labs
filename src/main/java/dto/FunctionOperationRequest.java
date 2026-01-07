package dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FunctionOperationRequest {
    @JsonProperty("functionId1")
    private Long functionId1;
    
    @JsonProperty("functionId2")
    private Long functionId2;
    
    private String operation; // ADD, SUBTRACT, MULTIPLY, DIVIDE
    private String resultName;
    private String factoryType;

    public FunctionOperationRequest() {
    }

    public Long getFunctionId1() {
        return functionId1;
    }

    public void setFunctionId1(Long functionId1) {
        this.functionId1 = functionId1;
    }

    public Long getFunctionId2() {
        return functionId2;
    }

    public void setFunctionId2(Long functionId2) {
        this.functionId2 = functionId2;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getResultName() {
        return resultName;
    }

    public void setResultName(String resultName) {
        this.resultName = resultName;
    }

    public String getFactoryType() {
        return factoryType;
    }

    public void setFactoryType(String factoryType) {
        this.factoryType = factoryType;
    }
}





