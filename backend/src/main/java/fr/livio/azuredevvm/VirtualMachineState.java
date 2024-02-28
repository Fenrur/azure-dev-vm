package fr.livio.azuredevvm;

import com.fasterxml.jackson.annotation.JsonValue;

public enum VirtualMachineState {
    CREATING,
    RUNNING,
    DELETING,
    DELETED;

    @JsonValue
    public String toLowerCase() {
        return name().toLowerCase();
    }
}
