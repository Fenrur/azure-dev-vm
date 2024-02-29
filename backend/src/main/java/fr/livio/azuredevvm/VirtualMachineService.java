package fr.livio.azuredevvm;

import com.azure.core.management.Region;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.compute.models.PowerState;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.network.models.Network;
import com.azure.resourcemanager.network.models.NetworkInterface;
import com.azure.resourcemanager.network.models.PublicIpAddress;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import fr.livio.azuredevvm.entity.VirtualMachineEntity;
import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class VirtualMachineService {

    public static final String PREFIX_RESOURCE_GROUP_NAME = "azuredevvm_";

    @Inject
    AzureResourceManager arm;

    public Map<UUID, OffsetDateTime> getCreatedDateTimeByMachineId() {
        return VirtualMachineEntity
                .listAllVirtualMachines()
                .stream()
                .flatMap(virtualMachineEntity -> {
                    try {
                        final VirtualMachine virtualMachine = arm
                                .virtualMachines()
                                .getByResourceGroup(PREFIX_RESOURCE_GROUP_NAME + virtualMachineEntity.machineId.toString(), virtualMachineEntity.machineId.toString());

                        final OffsetDateTime timeCreated = virtualMachine.timeCreated();
                        return Stream.of(Map.entry(virtualMachineEntity.machineId, timeCreated));
                    } catch (Exception ignored) {
                    }
                    return Stream.empty();
                })
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public VirtualMachineState getState(UUID machineId) {
        if (arm
                    .resourceGroups()
                    .getByName(PREFIX_RESOURCE_GROUP_NAME + machineId.toString()) == null) {

            return VirtualMachineState.DELETED;
        }

        final Optional<VirtualMachine> virtualMachine = getVirtualMachine(machineId);
        if (virtualMachine.isEmpty()) return VirtualMachineState.DELETED;

        final VirtualMachine vm = virtualMachine.get();
        final PowerState powerState = vm.powerState();
        if (powerState.equals(PowerState.RUNNING)) {
            return VirtualMachineState.RUNNING;
        } else if (powerState.equals(PowerState.STARTING)) {
            return VirtualMachineState.CREATING;
        } else if (powerState.equals(PowerState.DEALLOCATING) || powerState.equals(PowerState.DEALLOCATED) || powerState.equals(PowerState.STOPPING) || powerState.equals(PowerState.STOPPED)) {
            return VirtualMachineState.DELETING;
        } else {
            return VirtualMachineState.DELETED;
        }
    }

    public CreatedVirtualMachine create(UUID machineId, VirtualMachineSpecification spec) throws ExistingVirtualMachineException {
        applyResourceGroup(machineId);

        final PublicIpAddress publicIpAddress = applyPublicIpAddress(machineId);

        final Network network = applyNetwork(machineId);

        final NetworkInterface networkInterface = applyNetworkInterface(machineId, network, publicIpAddress);

        final Optional<VirtualMachine> virtualMachine = getVirtualMachine(machineId);
        if (virtualMachine.isPresent()) {
            throw new ExistingVirtualMachineException();
        }

        deleteDisk(machineId);

        createVirtualMachine(machineId, spec, networkInterface);

        final PublicIpAddress provisionnedPublicIpAddress = getPublicIpAddress(machineId);

        return CreatedVirtualMachine.from(provisionnedPublicIpAddress.ipAddress(), spec);
    }

    public void delete(UUID machineId) {
        arm
                .resourceGroups()
                .deleteByName(PREFIX_RESOURCE_GROUP_NAME + machineId.toString());
    }

    private void deleteDisk(UUID machineId) {
        try {
            arm
                    .disks()
                    .deleteByResourceGroup(PREFIX_RESOURCE_GROUP_NAME + machineId, machineId.toString());
        } catch (Exception ignored) {

        }
    }

    private PublicIpAddress getPublicIpAddress(UUID machineId) {
        return arm
                .publicIpAddresses()
                .getByResourceGroup(PREFIX_RESOURCE_GROUP_NAME + machineId, machineId.toString());
    }

    public Optional<VirtualMachine> getVirtualMachine(UUID machineId) {
        try {
            return Optional.of(
                    arm
                            .virtualMachines()
                            .getByResourceGroup(PREFIX_RESOURCE_GROUP_NAME + machineId, machineId.toString())
            );
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }

    private VirtualMachine createVirtualMachine(UUID machineId, VirtualMachineSpecification spec, NetworkInterface networkInterface) {
        return switch (spec) {
            case VirtualMachineSpecification.Linux linux -> {
                yield arm
                        .virtualMachines()
                        .define(machineId.toString())
                        .withRegion(Region.EUROPE_WEST)
                        .withExistingResourceGroup(PREFIX_RESOURCE_GROUP_NAME + machineId)
                        .withExistingPrimaryNetworkInterface(networkInterface)
                        .withLatestLinuxImage(linux.azureImage().publisher(), linux.azureImage().offer(), linux.azureImage().sku())
                        .withRootUsername(linux.rootUsername())
                        .withRootPassword(linux.password())
                        .withComputerName(linux.hostname())
                        .withSize("Standard_DS1_v2")
                        .create();
            }
            case VirtualMachineSpecification.Windows windows -> {
                yield arm
                        .virtualMachines()
                        .define(machineId.toString())
                        .withRegion(Region.EUROPE_WEST)
                        .withExistingResourceGroup(PREFIX_RESOURCE_GROUP_NAME + machineId)
                        .withExistingPrimaryNetworkInterface(networkInterface)
                        .withLatestWindowsImage(windows.azureImage().publisher(), windows.azureImage().offer(), windows.azureImage().sku())
                        .withAdminUsername(windows.adminUsername())
                        .withAdminPassword(windows.password())
                        .withComputerName(windows.hostname())
                        .withSize("Standard_DS1_v2")
                        .create();
            }
        };
    }

    private NetworkInterface applyNetworkInterface(UUID machineId, Network network, PublicIpAddress publicIpAddress) {
        NetworkInterface networkInterface;
        try {
            networkInterface = arm
                    .networkInterfaces()
                    .getByResourceGroup(PREFIX_RESOURCE_GROUP_NAME + machineId, machineId.toString());
        } catch (Exception e) {
            networkInterface = arm
                    .networkInterfaces()
                    .define(machineId.toString())
                    .withRegion(Region.EUROPE_WEST)
                    .withExistingResourceGroup(PREFIX_RESOURCE_GROUP_NAME + machineId)
                    .withExistingPrimaryNetwork(network)
                    .withSubnet(machineId.toString())
                    .withPrimaryPrivateIPAddressDynamic()
                    .withExistingPrimaryPublicIPAddress(publicIpAddress)
                    .create();
        }
        return networkInterface;
    }

    private Network applyNetwork(UUID machineId) {
        Network network;
        try {
            network = arm
                    .networks()
                    .getByResourceGroup(PREFIX_RESOURCE_GROUP_NAME + machineId, machineId.toString());
        } catch (Exception e) {
            network = arm
                    .networks()
                    .define(machineId.toString())
                    .withRegion(Region.EUROPE_WEST)
                    .withExistingResourceGroup(PREFIX_RESOURCE_GROUP_NAME + machineId)
                    .withAddressSpace("10.0.0.0/16")
                    .withSubnet(machineId.toString(), "10.0.0.0/24")
                    .create();
        }
        return network;
    }

    private PublicIpAddress applyPublicIpAddress(UUID machineId) {
        PublicIpAddress publicIpAddress;
        try {
            publicIpAddress = arm
                    .publicIpAddresses()
                    .getByResourceGroup(PREFIX_RESOURCE_GROUP_NAME + machineId, machineId.toString());
        } catch (Exception e) {
            publicIpAddress = arm
                    .publicIpAddresses()
                    .define(machineId.toString())
                    .withRegion(Region.EUROPE_WEST)
                    .withExistingResourceGroup(PREFIX_RESOURCE_GROUP_NAME + machineId)
                    .withDynamicIP()
                    .create();
        }
        return publicIpAddress;
    }

    private ResourceGroup applyResourceGroup(UUID machineId) {
        ResourceGroup resourceGroup = null;
        if (!arm.resourceGroups().contain(PREFIX_RESOURCE_GROUP_NAME + machineId)) {
            arm
                    .resourceGroups()
                    .define(PREFIX_RESOURCE_GROUP_NAME + machineId)
                    .withRegion(Region.EUROPE_WEST)
                    .create();
        } else {
            resourceGroup = arm
                    .resourceGroups()
                    .getByName(PREFIX_RESOURCE_GROUP_NAME + machineId);
        }

        return resourceGroup;
    }

    @RegisterForReflection
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    public sealed interface CreatedVirtualMachine {
        @JsonTypeName("linux")
        @RegisterForReflection
        record Linux(String hostname, String rootUsername, String password,
                     AzureImage azureImage, String publicAddress) implements CreatedVirtualMachine {
        }

        @JsonTypeName("windows")
        @RegisterForReflection
        record Windows(String hostname, String adminUsername, String password, AzureImage azureImage, String publicAddress) implements CreatedVirtualMachine {
        }

        static CreatedVirtualMachine from(String publicAddress, VirtualMachineSpecification spec) {
            return switch (spec) {
                case VirtualMachineSpecification.Linux linux ->
                        new Linux(linux.hostname, linux.rootUsername, linux.password, linux.azureImage, publicAddress);
                case VirtualMachineSpecification.Windows windows ->
                        new Windows(windows.hostname, windows.adminUsername, windows.password, windows.azureImage, publicAddress);
            };
        }
    }

    public static class VirtualMachineServiceException extends RuntimeException {
        public VirtualMachineServiceException(String message) {
            super(message);
        }
    }

    public static class ExistingVirtualMachineException extends Exception {
        public ExistingVirtualMachineException() {
            super("Virtual machine already exists");
        }
    }

    @RegisterForReflection
    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    public sealed static interface VirtualMachineSpecification {

        @JsonTypeName("linux")
        @RegisterForReflection
        record Linux(String hostname, String rootUsername, String password,
                     AzureImage azureImage) implements VirtualMachineSpecification {
        }

        @JsonTypeName("windows")
        @RegisterForReflection
        record Windows(String hostname, String adminUsername, String password, AzureImage azureImage) implements VirtualMachineSpecification {
        }
    }
}
