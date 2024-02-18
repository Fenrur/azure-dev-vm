package fr.livio.azuredevvm.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "virtual_machines", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"machineId"})
})
public class VirtualMachineEntity extends PanacheEntity {

    public UUID machineId;

    @ManyToOne(cascade = CascadeType.ALL)
    public UserEntity owner;

    public static VirtualMachineEntity add(UUID machineId, UserEntity user) {
        VirtualMachineEntity virtualMachine = new VirtualMachineEntity();
        virtualMachine.machineId = machineId;
        virtualMachine.owner = user;
        virtualMachine.persistAndFlush();
        return virtualMachine;
    }

    public static VirtualMachineEntity put(UUID machineId, UserEntity newOwner) {
        if (exists(machineId)) {
            final var virtualMachine = findByMachineId(machineId);
            virtualMachine.owner = newOwner;
            virtualMachine.persistAndFlush();
            return virtualMachine;
        } else {
            return add(machineId, newOwner);
        }
    }

    public static boolean exists(UUID machineId) {
        return VirtualMachineEntity.count("machineId", machineId) > 0;
    }

    public static List<VirtualMachineEntity> listAllVirtualMachines() {
        return VirtualMachineEntity.listAll();
    }

    public static VirtualMachineEntity findByMachineId(UUID machineId) {
        return VirtualMachineEntity.find("machineId", machineId).firstResult();
    }

    public static List<VirtualMachineEntity> findByUsername(String username) {
        return VirtualMachineEntity
                .find("owner.username", username)
                .list();
    }

    public static Long deleteFromUser(UUID machineId, UserEntity user) {
        return VirtualMachineEntity
                .delete("machineId = ?1 and owner = ?2", machineId, user);
    }

    public static long deleteByMachineId(UUID uuid) {
        return VirtualMachineEntity.delete("machineId", uuid);
    }
}
