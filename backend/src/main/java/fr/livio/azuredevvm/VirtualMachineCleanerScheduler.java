package fr.livio.azuredevvm;

import fr.livio.azuredevvm.entity.VirtualMachineEntity;
import io.quarkus.logging.Log;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.scheduler.Scheduled;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.context.ManagedExecutor;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public class VirtualMachineCleanerScheduler {

    @Inject
    VirtualMachineService virtualMachineService;

    @Inject
    ManagedExecutor managedExecutor;

    @Scheduled(every = "1m")
    void clean() {
        Log.info("\uD83E\uDDF9 Cleaning virtual machines");
        for (Map.Entry<UUID, OffsetDateTime> entry : virtualMachineService.getCreatedDateTimeByMachineId().entrySet()) {
            final UUID machineId = entry.getKey();
            final OffsetDateTime timeCreated = entry.getValue();
            if (OffsetDateTime.now().isAfter(timeCreated.plusMinutes(3))) {
                managedExecutor.runAsync(() -> {
                    try {
                        virtualMachineService.delete(machineId);
                        VirtualMachineEntity.deleteByMachineId(machineId);

                        Log.info("\uD83E\uDDF9 Virtual machine " + machineId + " deleted");
                    } catch (VirtualMachineService.VirtualMachineServiceException e) {
                        Log.error("\uD83D\uDEAB Failed to delete virtual machine " + machineId, e);
                    }
                });
            }
        }
        Log.info("\uD83E\uDDF9 Virtual machines cleaned");
    }
}
