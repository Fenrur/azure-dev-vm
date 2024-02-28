package fr.livio.azuredevvm.entity;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import io.quarkus.security.jpa.Password;
import io.quarkus.security.jpa.Roles;
import io.quarkus.security.jpa.UserDefinition;
import io.quarkus.security.jpa.Username;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

@Entity
@Table(name = "users", uniqueConstraints = {
        @jakarta.persistence.UniqueConstraint(columnNames = {"username"})
})
@UserDefinition
public class UserEntity extends PanacheEntity {
    @Username
    public String username;
    @Password
    public String password;
    @Roles
    public String role;
    @PositiveOrZero
    public int token;

    public static void add(String username, String password, String role, int token) {
        UserEntity user = new UserEntity();
        user.username = username;
        user.password = BcryptUtil.bcryptHash(password);
        user.role = role;
        user.token = token;
        user.persist();
    }

    public static boolean exists(String username) {
        return UserEntity.count("username", username) > 0;
    }

    public static List<UserEntity> listAllUsers() {
        return UserEntity.listAll();
    }

    public static UserEntity findByUsername(String username) {
        return UserEntity.find("username", username).firstResult();
    }

    public static UserEntity updateToken(String username, int token) {
        final UserEntity user = findByUsername(username);
        user.token = token;
        user.persistAndFlush();
        return user;
    }
}
