package entities;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "composite_functions")
public class CompositeFunctionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "inner_function", nullable = false)
    private String innerFunction;

    @Column(name = "outer_function", nullable = false)
    private String outerFunction;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserEntity user;

    public CompositeFunctionEntity() {
    }

    public CompositeFunctionEntity(String name, String innerFunction, String outerFunction, UserEntity user) {
        this.name = name;
        this.innerFunction = innerFunction;
        this.outerFunction = outerFunction;
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

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }
}

