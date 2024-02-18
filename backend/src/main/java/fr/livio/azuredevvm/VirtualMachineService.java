package fr.livio.azuredevvm;

import com.azure.core.management.Region;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.compute.models.KnownLinuxVirtualMachineImage;
import com.azure.resourcemanager.compute.models.VirtualMachine;
import com.azure.resourcemanager.network.models.Network;
import com.azure.resourcemanager.network.models.NetworkInterface;
import com.azure.resourcemanager.network.models.PublicIpAddress;
import com.azure.resourcemanager.resources.models.ResourceGroup;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import fr.livio.azuredevvm.entity.User;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.time.OffsetDateTime;
import java.util.List;
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

    public Uni<Map<String, OffsetDateTime>> getCreationByAppUsername() {
        return User
                .listAllUsers()
                .map(u -> {
                    final List<User> users = u
                            .stream()
                            .filter(user -> user.role.equals("user"))
                            .toList();

                    return users
                            .stream()
                            .flatMap(user -> {
                                try {
                                    final OffsetDateTime timeCreated = arm
                                            .virtualMachines()
                                            .getByResourceGroup(user.username, user.username)
                                            .timeCreated();
                                    return Stream.of(Map.entry(user.username, timeCreated));
                                } catch (Exception ignored) {
                                }
                                return Stream.empty();
                            })
                            .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
                });
    }

    public static class VirtualMachineServiceException extends RuntimeException {
        public VirtualMachineServiceException(String message) {
            super(message);
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    public sealed interface VirtualMachineSpecification {
        @JsonTypeName("linux")
        public record Linux(String hostname, String rootUsername) implements VirtualMachineSpecification { }
        @JsonTypeName("windows")
        public record Windows(String version) implements VirtualMachineSpecification { }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
    public sealed interface CreatedVirtualMachine {
        @JsonTypeName("linux")
        public record Linux(String hostname, String rootUsername, String password) implements CreatedVirtualMachine { }
        @JsonTypeName("windows")
        public record Window() implements CreatedVirtualMachine { }
    }

    public static class ExistingVirtualMachineException extends Exception {
        public ExistingVirtualMachineException() {
            super("Virtual machine already exists");
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

        return createVirtualMachine(machineId, spec, networkInterface);
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

    private CreatedVirtualMachine createVirtualMachine(UUID machineId, VirtualMachineSpecification spec, NetworkInterface networkInterface) {
        switch (spec) {
            case VirtualMachineSpecification.Linux linux -> {
                final String password = "P@ssw0rdP@ssw0rd";

                arm
                        .virtualMachines()
                        .define(machineId.toString())
                        .withRegion(Region.EUROPE_WEST)
                        .withExistingResourceGroup(PREFIX_RESOURCE_GROUP_NAME + machineId)
                        .withExistingPrimaryNetworkInterface(networkInterface)
                        .withPopularLinuxImage(KnownLinuxVirtualMachineImage.DEBIAN_10)
                        .withRootUsername(linux.rootUsername())
                        .withRootPassword(password)
                        .withComputerName(linux.hostname())
                        .withSize("Standard_DS1_v2")
                        .create();

                return new CreatedVirtualMachine.Linux(linux.hostname(), linux.rootUsername(), password);
            }
            case VirtualMachineSpecification.Windows windows -> {
                throw new VirtualMachineServiceException("Not implemented");
            }
        }
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
}
