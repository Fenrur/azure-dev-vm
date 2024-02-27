package fr.livio.azuredevvm;

import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
public class MaxThresholdVirtualMachine {

    @ConfigProperty(name = "azure.global.max-vms", defaultValue = "0")
    int maxGlobalVirtualMachines;

    @ConfigProperty(name = "azure.global.max-vms.role.advanced", defaultValue = "0")
    int maxVirtualMachinesWithAdvancedRole;

    @ConfigProperty(name = "azure.global.max-vms.role.basic", defaultValue = "0")
    int maxBasicVirtualMachinesWithBasicRole;

    public int byRole(Role role) {
        return switch (role) {
            case ADMIN -> maxGlobalVirtualMachines;
            case ADVANCED -> maxVirtualMachinesWithAdvancedRole;
            case BASIC -> maxBasicVirtualMachinesWithBasicRole;
        };
    }

    public int global() {
        return maxGlobalVirtualMachines;
    }
}
