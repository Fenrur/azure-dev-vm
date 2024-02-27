package fr.livio.azuredevvm.resource;

import com.tietoevry.quarkus.resteasy.problem.HttpProblem;
import fr.livio.azuredevvm.MaxThresholdVirtualMachine;
import fr.livio.azuredevvm.Role;
import fr.livio.azuredevvm.entity.UserEntity;
import io.quarkus.logging.Log;
import io.quarkus.security.runtime.QuarkusPrincipal;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.List;
import java.util.Optional;

@Path("/api/users")
@RunOnVirtualThread
public class UserResource {

    @Inject
    MaxThresholdVirtualMachine maxThresholdVirtualMachine;

    public record UserResponseRequest(String username, String role, int token, int maxVms) {
    }

    @GET
    @RolesAllowed({Role.Name.ADMIN, Role.Name.ADVANCED, Role.Name.BASIC})
    @Path("/me")
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(200)
    public UserResponseRequest me(@Context SecurityContext securityContext) {
        final UserEntity userEntity = UserEntity.findByUsername(securityContext.getUserPrincipal().getName());
        final int maxVms;
        if (securityContext.isUserInRole(Role.Name.ADMIN)) {
            maxVms = maxThresholdVirtualMachine.byRole(Role.ADMIN);
        }
        else if (securityContext.isUserInRole(Role.Name.ADVANCED)) {
            maxVms = maxThresholdVirtualMachine.byRole(Role.ADVANCED);
        }
        else if (securityContext.isUserInRole(Role.Name.BASIC)) {
            maxVms = maxThresholdVirtualMachine.byRole(Role.BASIC);
        }
        else {
            throw HttpProblem.builder().withStatus(Response.Status.INTERNAL_SERVER_ERROR).withTitle("Can't get max vms for your role").build();
        }

        return new UserResponseRequest(userEntity.username, userEntity.role, userEntity.token, maxVms);
    }

    public record ListUsersResponseRequest(List<UserResponseRequest> users) {
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
                        .map(user -> new UserResponseRequest(user.username, user.role, user.token, maxThresholdVirtualMachine.byRole(Role.fromString(user.role))))
                        .toList()
        );
    }

    public record CreateUserRequestBody(String username, String password, String role, int token) {
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed({Role.Name.ADMIN})
    @ResponseStatus(201)
    @Transactional
    public void create(CreateUserRequestBody body) {
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
            throw HttpProblem.builder()
                    .withTitle("User not found")
                    .withStatus(Response.Status.NOT_FOUND)
                    .build();
        }
    }

    public record GetTokenResponseBody(int token) { }
}
