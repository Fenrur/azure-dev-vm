package fr.livio.azuredevvm.resource;

import com.azure.resourcemanager.AzureResourceManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.livio.azuredevvm.VirtualMachineService;

import fr.livio.azuredevvm.VirtualMachineState;
import fr.livio.azuredevvm.entity.UserEntity;
import fr.livio.azuredevvm.entity.VirtualMachineEntity;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RunOnVirtualThread
@Path("/api")
public class TestResource {

    @Inject
    AzureResourceManager arm;

    @Inject
    VirtualMachineService virtualMachineService;

    public record TestResponseBody(String firstName, String lastName) {

    }

    public static final List<String> FIRST_NAMES = List.of(
            "Emma",
            "Lucas",
            "Chloé",
            "Hugo",
            "Alice",
            "Léo",
            "Lina",
            "Gabriel",
            "Zoé",
            "Raphaël"
    );

    public static final List<String> LAST_NAMES = List.of(
            "Martin",
            "Bernard",
            "Dubois",
            "Thomas",
            "Robert",
            "Richard",
            "Petit",
            "Durand",
            "Leroy",
            "Moreau"
    );


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ResponseStatus(200)
    @Path("/test")
    public TestResponseBody test() throws InterruptedException, NoSuchAlgorithmException {
        SecureRandom random = SecureRandom
                .getInstanceStrong();
        String firstName = FIRST_NAMES.get(random.nextInt(FIRST_NAMES.size()));
        String lastName = LAST_NAMES.get(random.nextInt(LAST_NAMES.size()));
        return new TestResponseBody(firstName, lastName);
    }

    @Inject
    ObjectMapper mapper;

    @POST
    @ResponseStatus(200)
    @Path("/test2")
    @Transactional
    @Produces(MediaType.APPLICATION_JSON)
    public VirtualMachineEntity test2() {
//        VirtualMachineService.VirtualMachineSpecification specification = new VirtualMachineService.VirtualMachineSpecification.Windows("12");
//        final UUID machineId = UUID.randomUUID();
//        VirtualMachineEntity.put(mapper, machineId, UserEntity.findByUsername("livio"), specification, VirtualMachineState.CREATING);
//        return VirtualMachineEntity.getByMachineId(machineId);
        return null;
    }

    @POST
    @ResponseStatus(200)
    @Path("/test3")
    @Produces(MediaType.APPLICATION_JSON)
    public JsonNode n(JsonNode n) {
        return n;
    }
}
