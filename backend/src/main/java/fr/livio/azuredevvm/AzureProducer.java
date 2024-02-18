package fr.livio.azuredevvm;

import com.azure.core.credential.TokenCredential;
import com.azure.core.http.okhttp.OkHttpAsyncHttpClientBuilder;
import com.azure.core.management.AzureEnvironment;
import com.azure.core.management.profile.AzureProfile;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.resourcemanager.AzureResourceManager;
import jakarta.enterprise.inject.Produces;
import okhttp3.Dispatcher;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.concurrent.Executors;

public class AzureProducer {

    @Produces
    public AzureResourceManager azureResourceManager(
            @ConfigProperty(name = "azure.tenant-id") String tenantId,
            @ConfigProperty(name = "azure.client-id") String clientId,
            @ConfigProperty(name = "azure.client-secret") String clientSecret,
            @ConfigProperty(name = "azure.subscription-id") String subscriptionId
    ) {
        final AzureProfile profile = new AzureProfile(tenantId, subscriptionId, AzureEnvironment.AZURE);

        final TokenCredential credential = new ClientSecretCredentialBuilder()
                .clientId(clientId)
                .clientSecret(clientSecret)
                .tenantId(tenantId)
                .build();

        return AzureResourceManager
                .configure()
//                .withHttpClient(
//                        new OkHttpAsyncHttpClientBuilder()
//                                .dispatcher(new Dispatcher(Executors.newVirtualThreadPerTaskExecutor()))
//                                .build()
//                )
                .authenticate(credential, profile)
                .withDefaultSubscription();
    }
}
