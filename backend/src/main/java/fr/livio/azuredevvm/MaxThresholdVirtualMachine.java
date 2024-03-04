package fr.livio.azuredevvm;

import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@Singleton
public class MaxThresholdVirtualMachine {

    @ConfigProperty(name = "max-vms.global", defaultValue = "0")
    int maxGlobalVirtualMachines;

    @ConfigProperty(name = "max-vms.role.admin", defaultValue = "0")
    int maxVirtualMachinesWithAdminRole;

    @ConfigProperty(name = "max-vms.role.advanced", defaultValue = "0")
    int maxVirtualMachinesWithAdvancedRole;

    @ConfigProperty(name = "max-vms.role.basic", defaultValue = "0")
    int maxBasicVirtualMachinesWithBasicRole;

    public int byRole(Role role) {
        return switch (role) {
            case ADMIN -> maxVirtualMachinesWithAdminRole;
            case ADVANCED -> maxVirtualMachinesWithAdvancedRole;
            case BASIC -> maxBasicVirtualMachinesWithBasicRole;
        };
    }

    public int global() {
        return maxGlobalVirtualMachines;
    }
}
