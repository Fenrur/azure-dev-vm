package fr.livio.azuredevvm;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.logging.Log;
import io.quarkus.scheduler.Scheduled;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.eclipse.microprofile.context.ManagedExecutor;

import java.time.OffsetDateTime;
import java.util.Map;

public class VirtualMachineCleanerScheduler {

    @Inject
    VirtualMachineService virtualMachineService;

    @Inject
    ManagedExecutor managedExecutor;

//    @Scheduled(every="1m")
//    @WithSession
//    Uni<Void> clean() {
//        return virtualMachineService.getCreationByAppUsername().map(m -> {
//            Log.info("\uD83E\uDDF9 Cleaning virtual machines");
//            for (Map.Entry<String, OffsetDateTime> entry : m.entrySet()) {
//                final String appUsername = entry.getKey();
//                final OffsetDateTime creationDate = entry.getValue();
//                if (OffsetDateTime.now().isAfter(creationDate.plusMinutes(10))) {
//                    managedExecutor.runAsync(() -> {
//                        virtualMachineService.delete(appUsername);
//                    });
//                    Log.info("\u2705 Virtual machine for user " + appUsername + " cleaned");
//                }
//            }
//            Log.info("\uD83E\uDDF9 Virtual machines cleaned");
//
//            return null;
//        });
//    }
}
