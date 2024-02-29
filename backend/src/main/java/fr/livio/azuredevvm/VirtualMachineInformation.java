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
    record Linux(String hostname, String rootUsername, String password,
                 AzureImage azureImage, Optional<String> publicAddress) implements VirtualMachineInformation {
    }

    @JsonTypeName("windows")
    @RegisterForReflection
    record Windows(String hostname, String adminUsername, String password, AzureImage azureImage,
                   Optional<String> publicAddress) implements VirtualMachineInformation {
    }

    static VirtualMachineInformation from(VirtualMachineService.CreatedVirtualMachine createdVirtualMachine) {
        return switch (createdVirtualMachine) {
            case VirtualMachineService.CreatedVirtualMachine.Linux linux ->
                    new Linux(linux.hostname(), linux.rootUsername(), linux.password(), linux.azureImage(), Optional.of(linux.publicAddress()));
            case VirtualMachineService.CreatedVirtualMachine.Windows windows ->
                    new Windows(windows.hostname(), windows.adminUsername(), windows.password(), windows.azureImage(), Optional.of(windows.publicAddress()));
        };
    }

    static VirtualMachineInformation from(VirtualMachineService.VirtualMachineSpecification spec) {
        return switch (spec) {
            case VirtualMachineService.VirtualMachineSpecification.Linux linux ->
                    new Linux(linux.hostname(), linux.rootUsername(), linux.password(), linux.azureImage(), Optional.empty());
            case VirtualMachineService.VirtualMachineSpecification.Windows windows ->
                    new Windows(windows.hostname(), windows.adminUsername(), windows.password(), windows.azureImage(), Optional.empty());
        };
    }
}
