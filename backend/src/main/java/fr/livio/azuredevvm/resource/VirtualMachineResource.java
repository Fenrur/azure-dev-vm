package fr.livio.azuredevvm.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.tietoevry.quarkus.resteasy.problem.HttpProblem;
import fr.livio.azuredevvm.CollectorUtils;
import fr.livio.azuredevvm.Role;
import fr.livio.azuredevvm.VirtualMachineService;
import fr.livio.azuredevvm.entity.UserEntity;
import fr.livio.azuredevvm.entity.VirtualMachineEntity;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Path("/api/vms")
@RunOnVirtualThread
public class VirtualMachineResource {

    @Inject
    VirtualMachineService virtualMachineService;

    @ConfigProperty(name = "azure.global.max-vms", defaultValue = "0")
    int maxGlobalVirtualMachines;

    @ConfigProperty(name = "azure.global.max-vms.role.advanced", defaultValue = "0")
    int maxVirtualMachinesWithAdvancedRole;

    @ConfigProperty(name = "azure.global.max-vms.role.basic", defaultValue = "0")
    int maxBasicVirtualMachinesWithBasicRole;

    public record UpdateVirtualMachineRequestBody(UUID machineId, String username) {
    }

    @PUT
    @RolesAllowed({Role.Name.ADMIN})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(200)
    @Transactional
    public void putVirtualMachine(UpdateVirtualMachineRequestBody body) {
        final UserEntity user = UserEntity.findByUsername(body.username);
        if (user == null) {
            throw HttpProblem.builder().withTitle("User not found").withStatus(Response.Status.NOT_FOUND).build();
        }

        try {
            VirtualMachineEntity.put(body.machineId, user);
        } catch (Exception e) {
            throw HttpProblem.builder().withTitle("Virtual machine not found").withStatus(Response.Status.NOT_FOUND).build();
        }
    }

    public record VirtualMachinesByUserResponseBody(Map<String, List<UUID>> virtualMachineIds) {
    }

    @GET
    @RolesAllowed({Role.Name.ADMIN, Role.Name.ADVANCED, Role.Name.BASIC})
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(200)
    public VirtualMachinesByUserResponseBody getVirtualMachinesByUser(@Context SecurityContext securityContext) throws IOException {
        final String appUsername = securityContext.getUserPrincipal().getName();

        if (securityContext.isUserInRole(Role.Name.ADMIN)) {
            return new VirtualMachinesByUserResponseBody(
                    VirtualMachineEntity
                            .listAllVirtualMachines()
                            .stream()
                            .collect(CollectorUtils.toMultivaluedMap(
                                    virtualMachine -> virtualMachine.owner.username,
                                    virtualMachine -> virtualMachine.machineId
                            ))
            );
        }

        return new VirtualMachinesByUserResponseBody(
                VirtualMachineEntity
                        .findByUsername(appUsername)
                        .stream()
                        .collect(CollectorUtils.toMultivaluedMap(
                                virtualMachine -> virtualMachine.owner.username,
                                virtualMachine -> virtualMachine.machineId
                        ))
        );
    }

    public record DeleteVirtualMachineRequestBody(UUID machineId) {

    }

    @DELETE
    @RolesAllowed({Role.Name.ADMIN, Role.Name.ADVANCED, Role.Name.BASIC})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(200)
    @Transactional
    public void deleteVirtualMachine(@Context SecurityContext securityContext, DeleteVirtualMachineRequestBody body) {
        final String appUsername = securityContext.getUserPrincipal().getName();

        if (securityContext.isUserInRole(Role.Name.ADMIN)) {
            if (VirtualMachineEntity.deleteByMachineId(body.machineId()) == 0) {
                throw HttpProblem.builder().withTitle("Not found machine in db").withStatus(Response.Status.NOT_FOUND).build();
            }

            try {
                virtualMachineService.delete(body.machineId());
            } catch (Exception e) {
                throw HttpProblem.builder().withTitle("Not found machine in azure").withStatus(Response.Status.NOT_FOUND).build();
            }
        } else {
            final UserEntity user = UserEntity.findByUsername(appUsername);

            if (user == null) {
                throw HttpProblem.builder().withTitle("User not found").withStatus(Response.Status.NOT_FOUND).build();
            }

            if (VirtualMachineEntity.deleteFromUser(body.machineId(), user) == 0) {
                throw HttpProblem.builder().withTitle("Not found machine in db").withStatus(Response.Status.NOT_FOUND).build();
            }

            try {
                virtualMachineService.delete(body.machineId());
            } catch (Exception e) {
                throw HttpProblem.builder().withTitle("Not found machine in azure").withStatus(Response.Status.NOT_FOUND).build();
            }
        }

    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    public sealed interface CreateVirtualMachineRequestBody {

        @JsonTypeName("linux")
        record Linux(String hostname,
                     String rootUsername) implements CreateVirtualMachineRequestBody {
            @Override
            public VirtualMachineService.VirtualMachineSpecification.Linux toVirtualMachineSpecification() {
                return new VirtualMachineService.VirtualMachineSpecification.Linux(hostname, rootUsername);
            }
        }

        @JsonTypeName("windows")
        record Windows(String version) implements CreateVirtualMachineRequestBody {
            @Override
            public VirtualMachineService.VirtualMachineSpecification.Windows toVirtualMachineSpecification() {
                return new VirtualMachineService.VirtualMachineSpecification.Windows(version);
            }
        }

        @JsonIgnore
        VirtualMachineService.VirtualMachineSpecification toVirtualMachineSpecification();
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    public interface CreateVirtualMachineResponseBody {
        @JsonTypeName("linux")
        record Linux(UUID machineId, String hostname, String rootUsername, String password) implements CreateVirtualMachineResponseBody { }
        @JsonTypeName("windows")
        record Windows(UUID machineId) implements CreateVirtualMachineResponseBody { }

        UUID machineId();

        static CreateVirtualMachineResponseBody from(UUID machineId, VirtualMachineService.CreatedVirtualMachine createdVirtualMachine) {
            return switch (createdVirtualMachine) {
                case VirtualMachineService.CreatedVirtualMachine.Linux linux ->
                    new Linux(machineId, linux.hostname(), linux.rootUsername(), linux.password());
                case VirtualMachineService.CreatedVirtualMachine.Windows windows ->
                    new Windows(machineId);
            };
        }
    }

    @POST
    @RolesAllowed({Role.Name.ADVANCED, Role.Name.BASIC})
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    @Transactional
    public CreateVirtualMachineResponseBody createVirtualMachine(@Context SecurityContext securityContext, CreateVirtualMachineRequestBody body) throws NoSuchAlgorithmException {
        final String appUsername = securityContext.getUserPrincipal().getName();
        final UUID machineId = UUID.randomUUID();
        final var spec = body.toVirtualMachineSpecification();

        if (VirtualMachineEntity.exists(machineId)) {
            throw HttpProblem.builder()
                    .withTitle("Virtual machine already exists")
                    .withStatus(Response.Status.CONFLICT)
                    .build();
        }

        if (VirtualMachineEntity.count() >= maxGlobalVirtualMachines) {
            throw HttpProblem.builder()
                    .withTitle("Max virtual machines reached")
                    .withStatus(Response.Status.FORBIDDEN)
                    .build();
        }

        if (securityContext.isUserInRole(Role.Name.ADVANCED) && VirtualMachineEntity.findByUsername(appUsername).size() >= maxVirtualMachinesWithAdvancedRole) {
            throw HttpProblem.builder()
                    .withTitle("Max virtual machines per user reached")
                    .withDetail("You have reached the maximum number '%s' of virtual machines for your role".formatted(maxVirtualMachinesWithAdvancedRole))
                    .withStatus(Response.Status.FORBIDDEN)
                    .build();
        }

        if (securityContext.isUserInRole(Role.Name.BASIC) && VirtualMachineEntity.findByUsername(appUsername).size() >= maxBasicVirtualMachinesWithBasicRole) {
            throw HttpProblem.builder()
                    .withTitle("Max virtual machines per user reached")
                    .withDetail("You have reached the maximum number '%s' of virtual machines for your role".formatted(maxBasicVirtualMachinesWithBasicRole))
                    .withStatus(Response.Status.FORBIDDEN)
                    .build();
        }

        final UserEntity user = UserEntity.findByUsername(appUsername);
        if (user == null) {
            throw HttpProblem.builder().withTitle("User not found").withStatus(Response.Status.NOT_FOUND).build();
        }

        user.token -= 1;

        if (user.token < 0) {
            throw HttpProblem.builder().withTitle("Not enough tokens").withStatus(Response.Status.FORBIDDEN).build();
        }

        user.persistAndFlush();

        try {
            final var createdVirtualMachine = virtualMachineService.create(machineId, spec);

            try {
                VirtualMachineEntity.put(machineId, user);
            } catch (Exception e) {
                throw HttpProblem.builder().withTitle("Not found user in db for creating").withStatus(Response.Status.NOT_FOUND).build();
            }

            return CreateVirtualMachineResponseBody.from(machineId, createdVirtualMachine);
        } catch (VirtualMachineService.ExistingVirtualMachineException e) {
            throw HttpProblem.builder()
                    .withTitle("Virtual machine already exists")
                    .withStatus(Response.Status.CONFLICT)
                    .build();
        }
    }
}
