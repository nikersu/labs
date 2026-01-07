package DTO;

public class Function {
    private Integer id;
    private String name;
    private String expression;
    private Integer userId;
    private String xValuesJson;  // JSON массив X значений (сохранение целиком)
    private String yValuesJson;  // JSON массив Y значений (сохранение целиком)
    private Integer count;        // Количество точек

    public Function() {}

    public Function(String name, String expression, Integer userId) {
        this.name = name;
        this.expression = expression;
        this.userId = userId;
    }

    // геттеры и сеттеры
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getExpression() { return expression; }
    public void setExpression(String expression) { this.expression = expression; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getXValuesJson() { return xValuesJson; }
    public void setXValuesJson(String xValuesJson) { this.xValuesJson = xValuesJson; }

    public String getYValuesJson() { return yValuesJson; }
    public void setYValuesJson(String yValuesJson) { this.yValuesJson = yValuesJson; }

    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }

    @Override
    public String toString() {
        return "Function{id=" + id + ", name='" + name + "', expression='" + expression + "', userId=" + userId + "}";
    }
}