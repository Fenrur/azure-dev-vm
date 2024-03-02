package fr.livio.azuredevvm;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Optional;

@RegisterForReflection
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public sealed interface VirtualMachineInformation {
    @JsonTypeName("linux")
    @RegisterForReflection
    record Linux(String name, String hostname, String rootUsername, String password,
                 AzureImage azureImage, Optional<String> publicAddress) implements VirtualMachineInformation {
    }

    @JsonTypeName("windows")
    @RegisterForReflection
    record Windows(String name, String hostname, String adminUsername, String password, AzureImage azureImage,
                   Optional<String> publicAddress) implements VirtualMachineInformation {
    }
}
