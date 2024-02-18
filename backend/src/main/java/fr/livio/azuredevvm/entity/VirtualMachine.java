package fr.livio.azuredevvm.entity;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "virtual_machines", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"machineId"})
})
public class VirtualMachine extends PanacheEntity {

    public UUID machineId;

    @ManyToOne(cascade = CascadeType.ALL)
    public User owner;

    public static Uni<VirtualMachine> add(UUID machineId, User user) {
        VirtualMachine virtualMachine = new VirtualMachine();
        virtualMachine.machineId = machineId;
        virtualMachine.owner = user;
        return virtualMachine.persistAndFlush()
                .onItem().transform(inserted -> virtualMachine);
    }

    public static Uni<VirtualMachine> put(UUID machineId, User newOwner) {
        return exists(machineId)
                .onItem().transformToUni(exists -> {
                    if (exists) {
                        return findByMachineId(machineId)
                                .onItem().transform(vm -> {
                                    vm.owner = newOwner;
                                    return vm;
                                })
                                .onItem().transformToUni(virtualMachine -> virtualMachine.persistAndFlush()
                                        .onItem().transform(inserted -> virtualMachine));
                    } else {
                        return add(machineId, newOwner);
                    }
                });
    }

    public static Uni<Boolean> exists(UUID machineId) {
        return VirtualMachine
                .count("machineId", machineId)
                .map(count -> count > 0);
    }

    public static Uni<List<VirtualMachine>> listAllVirtualMachines() {
        return VirtualMachine.listAll();
    }

    public static Uni<VirtualMachine> findByMachineId(UUID machineId) {
        return VirtualMachine.find("machineId", machineId).firstResult();
    }

    public static Uni<List<VirtualMachine>> findByUsername(String username) {
        return VirtualMachine
                .find("owner.username", username)
                .list()
                .map(panacheEntityBases -> panacheEntityBases
                        .stream()
                        .map(e -> (VirtualMachine) e)
                        .toList()
                );
    }

    public static Uni<Long> deleteFromUser(UUID machineId, User user) {
        return VirtualMachine
                .delete("machineId = ?1 and owner = ?2", machineId, user);
    }
}
