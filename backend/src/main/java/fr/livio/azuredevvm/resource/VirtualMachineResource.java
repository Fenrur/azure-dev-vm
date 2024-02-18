package fr.livio.azuredevvm.resource;

import com.azure.resourcemanager.AzureResourceManager;
import com.tietoevry.quarkus.resteasy.problem.HttpProblem;
import fr.livio.azuredevvm.CollectorUtils;
import fr.livio.azuredevvm.Role;
import fr.livio.azuredevvm.VirtualMachineService;
import fr.livio.azuredevvm.entity.User;
import fr.livio.azuredevvm.entity.VirtualMachine;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Path("/api/vms")
public class VirtualMachineResource {

    @Inject
    VirtualMachineService virtualMachineService;

    @Inject
    AzureResourceManager arm;

    @ConfigProperty(name = "azure.max-vms", defaultValue = "0")
    int maxVirtualMachines;

    public record UpdateVirtualMachineRequestBody(UUID machineId, String username) {
    }

    @PUT
    @RolesAllowed({Role.Name.ADMIN})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(200)
    public Uni<Void> putVirtualMachine(UpdateVirtualMachineRequestBody body) {
        return User
                .findByUsername(body.username)
                .onItem().transformToUni(user -> VirtualMachine.put(body.machineId, user))
                .replaceWithVoid();
    }

    public record VirtualMachinesByUserResponseBody(MultivaluedHashMap<String, UUID> virtualMachineIds) {
    }

    @GET
    @RolesAllowed({Role.Name.ADMIN, Role.Name.ADVANCED, Role.Name.BASIC})
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(200)
    public Uni<VirtualMachinesByUserResponseBody> getVirtualMachinesByUser(@Context SecurityContext securityContext) throws IOException {
        final String appUsername = securityContext.getUserPrincipal().getName();

        if (securityContext.isUserInRole(Role.Name.ADMIN)) {
            return VirtualMachine
                    .listAllVirtualMachines()
                    .map(virtualMachines ->
                            virtualMachines
                                    .stream()
                                    .collect(CollectorUtils.toMultivaluedMap(
                                            virtualMachine -> virtualMachine.owner.username,
                                            virtualMachine -> virtualMachine.machineId
                                    ))
                    )
                    .map(VirtualMachinesByUserResponseBody::new);
        }

        return VirtualMachine
                .findByUsername(appUsername)
                .map(virtualMachines ->
                        virtualMachines
                                .stream()
                                .collect(CollectorUtils.toMultivaluedMap(
                                        virtualMachine -> virtualMachine.owner.username,
                                        virtualMachine -> virtualMachine.machineId
                                ))
                )
                .map(VirtualMachinesByUserResponseBody::new);
    }

    public record DeleteVirtualMachineRequestBody(UUID machineId) {

    }

    @DELETE
    @RolesAllowed({Role.Name.ADMIN, Role.Name.ADVANCED, Role.Name.BASIC})
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(200)
    public Uni<Void> deleteVirtualMachine(@Context SecurityContext securityContext, DeleteVirtualMachineRequestBody body) {
        final String appUsername = securityContext.getUserPrincipal().getName();

        if (securityContext.isUserInRole(Role.Name.ADMIN)) {
            try {
                virtualMachineService
                        .delete(body.machineId());
            } catch (Exception e) {
                return Uni
                        .createFrom()
                        .failure(HttpProblem.builder().withTitle("Not found machine in azure").withStatus(Response.Status.NOT_FOUND).build());
            }

            return Uni.createFrom().voidItem();
        }

        return User.findByUsername(appUsername)
                .onItem().transformToUni(user -> VirtualMachine.deleteFromUser(body.machineId(), user))
                .onFailure().transform(throwable -> HttpProblem.builder().withTitle("Not found machine in db for deleting").withStatus(Response.Status.NOT_FOUND).build())
                .onItem().invoke(() -> virtualMachineService.delete(body.machineId()))
                .onFailure().transform(throwable -> HttpProblem.builder().withTitle("Not found machine in azure").withStatus(Response.Status.NOT_FOUND).build())
                .replaceWithVoid();
    }

    public record CreateVirtualMachineResponseBody(UUID machineId, VirtualMachineService.CreatedVirtualMachine createdVirtualMachine) {

    }

    @POST
    @RolesAllowed({Role.Name.ADVANCED, Role.Name.BASIC})
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @ResponseStatus(201)
    public Uni<CreateVirtualMachineResponseBody> createVirtualMachine(@Context SecurityContext securityContext, VirtualMachineService.VirtualMachineSpecification spec) throws NoSuchAlgorithmException {
        final String appUsername = securityContext.getUserPrincipal().getName();
//
//        VirtualMachine
//                .count()
//                .onItem().transform(Unchecked.function(count -> {
//                    if (count >= maxVirtualMachines) {
//                        throw HttpProblem.builder().withTitle("Max virtual machines reached").withStatus(Response.Status.CONFLICT).build();
//                    }
//                    return count;
//                }))
//
//
//        if (securityContext.isUserInRole(Role.Name.BASIC)) {
//            Role.BASIC.maxVirtualMachines();
//            VirtualMachine
//                    .findByUsername(appUsername);
//
//        }
//        else if (securityContext.isUserInRole(Role.Name.ADVANCED)) {
//
//        }
//        else {
//            throw HttpProblem.builder().withTitle("Not found role").withStatus(Response.Status.NOT_FOUND).build();
//        }

        final UUID machineId = UUID.randomUUID();

        try {
            final VirtualMachineService.CreatedVirtualMachine createdVirtualMachine = virtualMachineService.create(machineId, spec);

            return User
                    .findByUsername(appUsername)
                    .onItem().transformToUni(user -> VirtualMachine.put(machineId, user))
                    .onFailure().transform(throwable -> HttpProblem.builder().withTitle("Not found user in db for creating").withStatus(Response.Status.NOT_FOUND).build())
                    .onItem().transformToUni(user -> Uni.createFrom().item(new CreateVirtualMachineResponseBody(machineId, createdVirtualMachine)));
        } catch (VirtualMachineService.ExistingVirtualMachineException e) {
            throw HttpProblem.builder()
                    .withTitle("Virtual machine already exists")
                    .withStatus(Response.Status.CONFLICT)
                    .build();
        }
    }
}
