package fr.livio.azuredevvm.resource;

import com.tietoevry.quarkus.resteasy.problem.HttpProblem;
import fr.livio.azuredevvm.Role;
import fr.livio.azuredevvm.entity.User;
import io.smallrye.mutiny.Uni;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.List;

@Path("/api/users")
public class UserResource {

    @GET
    @RolesAllowed({Role.Name.ADMIN, Role.Name.ADVANCED, Role.Name.BASIC})
    @Path("/me")
    @Produces(MediaType.TEXT_PLAIN)
    @ResponseStatus(200)
    public Uni<String> me(@Context SecurityContext securityContext) {
        return Uni
                .createFrom()
                .item(securityContext.getUserPrincipal().getName());
    }

    public record ListUsersResponseRequest(List<String> users) {
    }

    @GET
    @RolesAllowed({Role.Name.ADMIN})
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(200)
    public Uni<ListUsersResponseRequest> listUsers() {
        return User
                .listAllUsers()
                .onItem().transform(users -> users.stream().map(user -> user.username).toList())
                .onItem().transform(ListUsersResponseRequest::new);
    }

    public record CreateUserBodyRequest(String username, String password, String role, int token) {
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Role.Name.ADMIN})
    @ResponseStatus(201)
    public Uni<Void> create(CreateUserBodyRequest body) {
        return User
                .exists(body.username())
                .onItem().transformToUni(exists -> exists
                        ? Uni.createFrom().failure(HttpProblem.builder().withTitle("User already exists").withStatus(Response.Status.CONFLICT).build())
                        : Uni.createFrom().nullItem()
                )
                .onItem().call(ignored -> User.add(body.username(), body.password(), body.role(), body.token()))
                .replaceWithVoid();
    }

    public record SetTokenBodyRequest(String username, int token) { }

    @PUT
    @Path("/token")
    @RolesAllowed({Role.Name.ADMIN})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(200)
    public Uni<Void> setToken(SetTokenBodyRequest body) {
        return User
                .setToken(body.username(), body.token());
    }

    @GET
    @Path("/token")
    @RolesAllowed({Role.Name.ADMIN})
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(200)
    public Uni<Integer> getToken(@QueryParam("username") String username) {
        return User
                .findByUsername(username)
                .onItem().transform(user -> user.token);
    }

    @GET
    @Path("/token/me")
    @RolesAllowed({Role.Name.ADMIN, Role.Name.ADVANCED, Role.Name.BASIC})
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(200)
    public Uni<Integer> getMyToken(@Context SecurityContext securityContext) {
        return User
                .findByUsername(securityContext.getUserPrincipal().getName())
                .onItem().transform(user -> user.token);
    }
}
