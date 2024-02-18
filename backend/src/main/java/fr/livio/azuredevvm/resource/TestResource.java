package fr.livio.azuredevvm.resource;

import com.azure.resourcemanager.AzureResourceManager;
import fr.livio.azuredevvm.VirtualMachineService;

import fr.livio.azuredevvm.entity.VirtualMachine;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.HashMap;
import java.util.UUID;

@Path("/test")
public class TestResource {

    @Inject
    AzureResourceManager arm;

    @Inject
    VirtualMachineService virtualMachineService;

    public record TestResponseBody(HashMap<String, Long> map) {

    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(200)
    public Uni<VirtualMachine> test() throws InterruptedException {
        return VirtualMachine
                .findByMachineId(UUID.randomUUID())
                .log();
    }
}
