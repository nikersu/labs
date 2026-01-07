package dto;

public class CreateCompositeFunctionRequest {
    private String name;
    private String innerFunction;
    private String outerFunction;

    public CreateCompositeFunctionRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getInnerFunction() {
        return innerFunction;
    }

    public void setInnerFunction(String innerFunction) {
        this.innerFunction = innerFunction;
    }

    public String getOuterFunction() {
        return outerFunction;
    }

    public void setOuterFunction(String outerFunction) {
        this.outerFunction = outerFunction;
    }
}





