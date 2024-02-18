package fr.livio.azuredevvm.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.security.jpa.Password;
import io.quarkus.security.jpa.Roles;
import io.quarkus.security.jpa.UserDefinition;
import io.quarkus.security.jpa.Username;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

@Entity
@Table(name = "users", uniqueConstraints = {
        @jakarta.persistence.UniqueConstraint(columnNames = {"username"})
})
@UserDefinition
public class User extends PanacheEntity {
    @Username
    public String username;
    @Password
    public String password;
    @Roles
    public String role;
    @PositiveOrZero
    public int token;

    public static Uni<PanacheEntityBase> add(String username, String password, String role, int token) {
        User user = new User();
        user.username = username;
        user.password = BcryptUtil.bcryptHash(password);
        user.role = role;
        user.token = token;
        return user.persist();
    }

    public static Uni<Boolean> exists(String username) {
        return User
                .count("username", username)
                .map(count -> count > 0);
    }

    public static Uni<List<User>> listAllUsers() {
        return User.listAll();
    }

    public static Uni<User> findByUsername(String username) {
        return User.find("username", username).firstResult();
    }

    //set token by username
    public static Uni<Void> setToken(String username, int token) {
        return findByUsername(username)
                .onItem().transform(user -> {
                    user.token = token;
                    return user;
                })
                .onItem().transformToUni(PanacheEntityBase::flush);
    }
}
