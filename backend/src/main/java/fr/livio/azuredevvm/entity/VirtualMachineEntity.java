package fr.livio.azuredevvm.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.livio.azuredevvm.VirtualMachineService;
import fr.livio.azuredevvm.VirtualMachineState;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;

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

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    public String specification;

    public VirtualMachineState state;

    public static VirtualMachineEntity add(ObjectMapper mapper, UUID machineId, UserEntity user, VirtualMachineService.VirtualMachineSpecification specification, VirtualMachineState state) {
        VirtualMachineEntity virtualMachine = new VirtualMachineEntity();
        virtualMachine.machineId = machineId;
        virtualMachine.owner = user;
        try {
            virtualMachine.specification = mapper.writeValueAsString(specification);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        virtualMachine.state = state;
        virtualMachine.persistAndFlush();
        return virtualMachine;
    }

    public static VirtualMachineEntity put(ObjectMapper mapper, UUID machineId, UserEntity newOwner, VirtualMachineService.VirtualMachineSpecification specification, VirtualMachineState state) {
        if (exists(machineId)) {
            final var virtualMachine = findByMachineId(machineId);
            virtualMachine.owner = newOwner;
            try {
                virtualMachine.specification = mapper.writeValueAsString(specification);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            virtualMachine.state = state;
            virtualMachine.persistAndFlush();
            return virtualMachine;
        } else {
            return add(mapper, machineId, newOwner, specification, state);
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
        VirtualMachineEntity.find("machineId = ?1 and owner = ?2", machineId, user).firstResult().delete();
        return 1L;
    }

    public static long deleteByMachineId(UUID uuid) {
        return VirtualMachineEntity.delete("machineId", uuid);
    }

    public static VirtualMachineEntity getByMachineId(UUID machineId) {
        return VirtualMachineEntity.find("machineId", machineId).firstResult();
    }

    public static void updateState(UUID machineId, VirtualMachineState state) {
        VirtualMachineEntity
                .update("state = ?1 where machineId = ?2", state, machineId);
    }

    @JsonIgnore
    public VirtualMachineService.VirtualMachineSpecification parseSpecification(ObjectMapper mapper) {
        try {
            return mapper.readValue(specification, VirtualMachineService.VirtualMachineSpecification.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
