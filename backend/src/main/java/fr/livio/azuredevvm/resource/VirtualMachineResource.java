package fr.livio.azuredevvm.resource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tietoevry.quarkus.resteasy.problem.HttpProblem;
import fr.livio.azuredevvm.*;
import fr.livio.azuredevvm.entity.UserEntity;
import fr.livio.azuredevvm.entity.VirtualMachineEntity;
import io.quarkus.logging.Log;
import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.transaction.UserTransaction;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Stream;

@Path("/api/vms")
@RunOnVirtualThread
public class VirtualMachineResource {

    @Inject
    VirtualMachineService virtualMachineService;

    @Inject
    MaxThresholdVirtualMachine maxThresholdVirtualMachine;

    @Inject
    UserTransaction userTransaction;

    @Inject
    ObjectMapper mapper;

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
            throw HttpProblem.builder()
                    .withTitle("User not found")
                    .withStatus(Response.Status.NOT_FOUND)
                    .build();
        }

        try {
            VirtualMachineEntity.put(mapper, body.machineId, user, null, VirtualMachineState.CREATING);
        } catch (Exception e) {
            throw HttpProblem.builder()
                    .withTitle("Virtual machine not found")
                    .withStatus(Response.Status.NOT_FOUND)
                    .build();
        }
    }

    public record VirtualMachinesByUserValue(@JsonIgnore long id, UUID machineId, VirtualMachineInformation info,
                                             VirtualMachineState state) {

        @Override
        @JsonIgnore
        public long id() {
            return id;
        }
    }

    public record VirtualMachinesByUserResponseBody(Map<String, List<VirtualMachinesByUserValue>> virtualMachines) {
    }

    @GET
    @RolesAllowed({Role.Name.ADMIN, Role.Name.ADVANCED, Role.Name.BASIC})
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(200)
    @Transactional
    public VirtualMachinesByUserResponseBody getVirtualMachinesByUser(@Context SecurityContext securityContext) throws IOException {
        final String appUsername = securityContext.getUserPrincipal().getName();

        if (securityContext.isUserInRole(Role.Name.ADMIN)) {
            final MultivaluedHashMap<String, VirtualMachinesByUserValue> collect = VirtualMachineEntity
                    .listAllVirtualMachines()
                    .stream()
                    .collect(CollectorUtils.toMultivaluedMap(
                                    virtualMachine -> virtualMachine.owner.username,
                                    virtualMachine -> new VirtualMachinesByUserValue(virtualMachine.id, virtualMachine.machineId, virtualMachine.parseInformation(mapper), virtualMachine.state)
                            )
                    );

            for (Map.Entry<String, List<VirtualMachinesByUserValue>> entry : collect.entrySet()) {
                entry.getValue().sort(Comparator.comparingLong(VirtualMachinesByUserValue::id));
            }

            return new VirtualMachinesByUserResponseBody(collect);
        }

        final MultivaluedHashMap<String, VirtualMachinesByUserValue> collect = VirtualMachineEntity
                .findByUsername(appUsername)
                .stream()
                .collect(CollectorUtils.toMultivaluedMap(
                                virtualMachine -> virtualMachine.owner.username,
                                virtualMachine -> new VirtualMachinesByUserValue(virtualMachine.id, virtualMachine.machineId, virtualMachine.parseInformation(mapper), virtualMachine.state)
                        )
                );

        for (Map.Entry<String, List<VirtualMachinesByUserValue>> entry : collect.entrySet()) {
            entry.getValue().sort(Comparator.comparingLong(VirtualMachinesByUserValue::id));
        }

        return new VirtualMachinesByUserResponseBody(collect);
    }

    @DELETE
    @Path("/{machineId}")
    @RolesAllowed({Role.Name.ADMIN, Role.Name.ADVANCED, Role.Name.BASIC})
    @ResponseStatus(200)
    @Transactional
    public void deleteVirtualMachine(@Context SecurityContext securityContext, @PathParam("machineId") UUID machineId) {
        final String appUsername = securityContext.getUserPrincipal().getName();

        if (securityContext.isUserInRole(Role.Name.ADMIN)) {
            final VirtualMachineEntity virtualMachine = VirtualMachineEntity.getByMachineId(machineId);
            if (virtualMachine == null) {
                throw HttpProblem.builder()
                        .withTitle("Not found machine in db")
                        .withStatus(Response.Status.NOT_FOUND)
                        .build();
            }

            if (virtualMachine.state != VirtualMachineState.RUNNING) {
                throw HttpProblem.builder()
                        .withTitle("Machine not running")
                        .withStatus(Response.Status.FORBIDDEN)
                        .build();
            }

            VirtualMachineEntity.updateState(machineId, VirtualMachineState.DELETING);

            Thread.startVirtualThread(() -> {
                try {
                    virtualMachineService.delete(machineId);
                    userTransaction.begin();
                    if (VirtualMachineEntity.deleteByMachineId(machineId) == 0) {
                        throw HttpProblem.builder()
                                .withTitle("Not found machine in db")
                                .withStatus(Response.Status.NOT_FOUND)
                                .build();
                    }
                    userTransaction.commit();
                } catch (Exception ignored) {
                }
            });
        } else {
            final UserEntity user = UserEntity.findByUsername(appUsername);

            if (user == null) {
                throw HttpProblem.builder()
                        .withTitle("User not found")
                        .withStatus(Response.Status.NOT_FOUND)
                        .build();
            }

            final VirtualMachineEntity virtualMachine = VirtualMachineEntity.getByMachineId(machineId);
            if (virtualMachine == null) {
                throw HttpProblem.builder()
                        .withTitle("Not found machine in db")
                        .withStatus(Response.Status.NOT_FOUND)
                        .build();
            }
            if (!virtualMachine.owner.username.equals(appUsername)) {
                throw HttpProblem.builder()
                        .withTitle("Not found machine in db")
                        .withStatus(Response.Status.NOT_FOUND)
                        .build();
            }

            if (virtualMachine.state != VirtualMachineState.RUNNING) {
                throw HttpProblem.builder()
                        .withTitle("Machine not running")
                        .withStatus(Response.Status.FORBIDDEN)
                        .build();
            }

            VirtualMachineEntity.updateState(machineId, VirtualMachineState.DELETING);

            Thread.startVirtualThread(() -> {
                try {
                    virtualMachineService.delete(machineId);
                    userTransaction.begin();
                    if (VirtualMachineEntity.deleteByMachineId(machineId) == 0) {
                        throw HttpProblem.builder()
                                .withTitle("Not found machine in db")
                                .withStatus(Response.Status.NOT_FOUND)
                                .build();
                    }
                    userTransaction.commit();
                } catch (Exception ignored) {
                }
            });
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    public sealed interface CreateVirtualMachineRequestBody {

        @JsonTypeName("linux")
        record Linux(String name, String hostname, String password,
                     String rootUsername, AzureImage azureImage) implements CreateVirtualMachineRequestBody {
            @Override
            public VirtualMachineService.VirtualMachineSpecification.Linux toVirtualMachineSpecification() {
                return new VirtualMachineService.VirtualMachineSpecification.Linux(hostname, rootUsername, password, azureImage);
            }
        }

        @JsonTypeName("windows")
        record Windows(String name, String hostname, String password,
                       String adminUsername, AzureImage azureImage) implements CreateVirtualMachineRequestBody {
            @Override
            public VirtualMachineService.VirtualMachineSpecification.Windows toVirtualMachineSpecification() {
                return new VirtualMachineService.VirtualMachineSpecification.Windows(hostname, adminUsername, password, azureImage);
            }
        }

        String name();

        String hostname();

        String password();

        AzureImage azureImage();

        @JsonIgnore
        VirtualMachineService.VirtualMachineSpecification toVirtualMachineSpecification();
    }

    public record CreateVirtualMachineResponseBody(UUID machineId) {

    }

    @POST
    @RolesAllowed({Role.Name.ADMIN, Role.Name.ADVANCED, Role.Name.BASIC})
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

        if (VirtualMachineEntity.count() >= maxThresholdVirtualMachine.global()) {
            throw HttpProblem.builder()
                    .withTitle("Max global virtual machines reached")
                    .withStatus(Response.Status.FORBIDDEN)
                    .build();
        }

        if (securityContext.isUserInRole(Role.Name.ADMIN) && VirtualMachineEntity.findByUsername(appUsername).size() >= maxThresholdVirtualMachine.byRole(Role.ADMIN)) {
            throw HttpProblem.builder()
                    .withTitle("Max virtual machines reached")
                    .withDetail("You have reached the maximum number '%s' of virtual machines for your role".formatted(maxThresholdVirtualMachine.byRole(Role.ADMIN)))
                    .withStatus(Response.Status.FORBIDDEN)
                    .build();
        }

        if (securityContext.isUserInRole(Role.Name.ADVANCED) && VirtualMachineEntity.findByUsername(appUsername).size() >= maxThresholdVirtualMachine.byRole(Role.ADVANCED)) {
            throw HttpProblem.builder()
                    .withTitle("Max virtual machines reached")
                    .withDetail("You have reached the maximum number '%s' of virtual machines for your role".formatted(maxThresholdVirtualMachine.byRole(Role.ADVANCED)))
                    .withStatus(Response.Status.FORBIDDEN)
                    .build();
        }

        if (securityContext.isUserInRole(Role.Name.BASIC) && VirtualMachineEntity.findByUsername(appUsername).size() >= maxThresholdVirtualMachine.byRole(Role.BASIC)) {
            throw HttpProblem.builder()
                    .withTitle("Max virtual machines reached")
                    .withDetail("You have reached the maximum number '%s' of virtual machines for your role".formatted(maxThresholdVirtualMachine.byRole(Role.BASIC)))
                    .withStatus(Response.Status.FORBIDDEN)
                    .build();
        }

        final UserEntity user = UserEntity.findByUsername(appUsername);
        if (user == null) {
            throw HttpProblem.builder()
                    .withTitle("User not found")
                    .withStatus(Response.Status.NOT_FOUND)
                    .build();
        }

        if (user.token <= 0) {
            throw HttpProblem.builder()
                    .withTitle("Not enough tokens")
                    .withStatus(Response.Status.FORBIDDEN)
                    .build();
        }

        user.token -= 1;
        user.persistAndFlush();

        try {
            var information = switch (body) {
                case CreateVirtualMachineRequestBody.Linux linux ->
                    new VirtualMachineInformation.Linux(linux.name, linux.hostname, linux.rootUsername, linux.password, linux.azureImage, Optional.empty());
                case CreateVirtualMachineRequestBody.Windows windows ->
                    new VirtualMachineInformation.Windows(windows.name, windows.hostname, windows.adminUsername, windows.password, windows.azureImage, Optional.empty());
            };

            VirtualMachineEntity.put(mapper, machineId, user, information, VirtualMachineState.CREATING);
        } catch (Exception e) {
            throw HttpProblem.builder()
                    .withTitle("Not found user in db for creating")
                    .withStatus(Response.Status.NOT_FOUND)
                    .build();
        }

        Thread.startVirtualThread(() -> {
            try {
                final var createdVirtualMachine = virtualMachineService.create(machineId, spec);

                userTransaction.begin();
                final VirtualMachineEntity virtualMachine = VirtualMachineEntity.getByMachineId(machineId);
                virtualMachine.state = VirtualMachineState.RUNNING;

                var information = switch (body) {
                    case CreateVirtualMachineRequestBody.Linux linux ->
                        new VirtualMachineInformation.Linux(linux.name, linux.hostname, linux.rootUsername, linux.password, linux.azureImage, Optional.of(createdVirtualMachine.publicAddress()));
                    case CreateVirtualMachineRequestBody.Windows windows ->
                        new VirtualMachineInformation.Windows(windows.name, windows.hostname, windows.adminUsername, windows.password, windows.azureImage, Optional.of(createdVirtualMachine.publicAddress()));
                };

                virtualMachine.information = mapper.writeValueAsString(information);
                virtualMachine.persistAndFlush();

                userTransaction.commit();
            } catch (Exception e) {
                try {
                    userTransaction.rollback();
                } catch (Exception ex) {
                    Log.error("Transaction rollback failed", ex);
                }
                Log.error("Error during virtual machine creation", e);
            }
        });

        return new CreateVirtualMachineResponseBody(machineId);
    }

    public record MaxThresholdResponseBody(int global, int admin, int advanced, int basic) {
    }

    @GET
    @Path("/max-threshold")
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(200)
    public MaxThresholdResponseBody getMaxThreshold() {
        return new MaxThresholdResponseBody(
                maxThresholdVirtualMachine.global(),
                maxThresholdVirtualMachine.byRole(Role.ADMIN),
                maxThresholdVirtualMachine.byRole(Role.ADVANCED),
                maxThresholdVirtualMachine.byRole(Role.BASIC)
        );
    }
}
