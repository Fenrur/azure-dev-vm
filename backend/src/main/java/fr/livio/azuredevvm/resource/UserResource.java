package fr.livio.azuredevvm.resource;

import com.tietoevry.quarkus.resteasy.problem.HttpProblem;
import fr.livio.azuredevvm.Role;
import fr.livio.azuredevvm.entity.UserEntity;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.List;

@Path("/api/users")
@RunOnVirtualThread
public class UserResource {

    @GET
    @RolesAllowed({Role.Name.ADMIN, Role.Name.ADVANCED, Role.Name.BASIC})
    @Path("/me")
    @Produces(MediaType.TEXT_PLAIN)
    @ResponseStatus(200)
    public String me(@Context SecurityContext securityContext) {
        return securityContext.getUserPrincipal().getName();
    }

    public record ListUsersResponseRequest(List<String> users) {
    }

    @GET
    @RolesAllowed({Role.Name.ADMIN})
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(200)
    public ListUsersResponseRequest listUsers() {
        return new ListUsersResponseRequest(
                UserEntity
                        .listAllUsers()
                        .stream()
                        .map(user -> user.username)
                        .toList()
        );
    }

    public record CreateUserBodyRequest(String username, String password, String role, int token) {
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Role.Name.ADMIN})
    @ResponseStatus(201)
    @Transactional
    public void create(CreateUserBodyRequest body) {
        if (UserEntity.exists(body.username)) {
            throw HttpProblem.builder().withTitle("User already exists").withStatus(Response.Status.CONFLICT).build();
        } else {
            UserEntity.add(body.username(), body.password(), body.role(), body.token());
        }
    }

    public record SetTokenBodyRequest(String username, int token) { }

    @PUT
    @Path("/token")
    @RolesAllowed({Role.Name.ADMIN})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(200)
    @Transactional
    public void setToken(SetTokenBodyRequest body) {
        try {
            UserEntity.updateToken(body.username(), body.token());
        } catch (Exception e) {
            Log.error("Error updating token", e);
            throw HttpProblem.builder().withTitle("User not found").withStatus(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Path("/token")
    @RolesAllowed({Role.Name.ADMIN})
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(200)
    public Integer getToken(@QueryParam("username") String username) {
        final UserEntity user = UserEntity.findByUsername(username);
        if (user == null) {
            throw HttpProblem.builder().withTitle("User not found").withStatus(Response.Status.NOT_FOUND).build();
        }

        return user.token;
    }

    @GET
    @Path("/token/me")
    @RolesAllowed({Role.Name.ADMIN, Role.Name.ADVANCED, Role.Name.BASIC})
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(200)
    public int getMyToken(@Context SecurityContext securityContext) {
        final UserEntity user = UserEntity.findByUsername(securityContext.getUserPrincipal().getName());
        if (user == null) {
            throw HttpProblem.builder().withTitle("User not found").withStatus(Response.Status.NOT_FOUND).build();
        }

        return user.token;
    }
}
