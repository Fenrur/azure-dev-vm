package fr.livio.azuredevvm.resource;

import com.azure.resourcemanager.AzureResourceManager;
import fr.livio.azuredevvm.VirtualMachineService;

import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@Path("/test")
@RunOnVirtualThread
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
    public List<UUID> test() throws InterruptedException {
        return null;
    }
}
