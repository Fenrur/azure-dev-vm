package fr.livio.azuredevvm.resource;

import com.azure.resourcemanager.AzureResourceManager;
import fr.livio.azuredevvm.VirtualMachineService;

import io.smallrye.common.annotation.RunOnVirtualThread;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.Cache;
import org.jboss.resteasy.reactive.NoCache;
import org.jboss.resteasy.reactive.ResponseStatus;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.List;

@Path("/test")
@RunOnVirtualThread
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
    public TestResponseBody test() throws InterruptedException, NoSuchAlgorithmException {
        SecureRandom random = SecureRandom
                .getInstanceStrong();
        String firstName = FIRST_NAMES.get(random.nextInt(FIRST_NAMES.size()));
        String lastName = LAST_NAMES.get(random.nextInt(LAST_NAMES.size()));
        return new TestResponseBody(firstName, lastName);
    }
}
