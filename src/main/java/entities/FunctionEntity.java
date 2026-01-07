package entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "functions")
public class FunctionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "expression", columnDefinition = "TEXT")
    private String expression;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private UserEntity user;

    @Lob
    @Column(name = "x_values", columnDefinition = "TEXT")
    private String xValuesJson;

    @Lob
    @Column(name = "y_values", columnDefinition = "TEXT")
    private String yValuesJson;

    @Column(name = "count")
    private Integer count;

    public FunctionEntity() {
    }

    public FunctionEntity(String name, String expression, UserEntity user) {
        this.name = name;
        this.expression = expression;
        this.user = user;
    }

    public Long getId() {
        return id;
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

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public String getXValuesJson() {
        return xValuesJson;
    }

    public void setXValuesJson(String xValuesJson) {
        this.xValuesJson = xValuesJson;
    }

    public String getYValuesJson() {
        return yValuesJson;
    }

    public void setYValuesJson(String yValuesJson) {
        this.yValuesJson = yValuesJson;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}

