import {z} from "zod";
import {BasicAuth, fetchDelete, fetchGetJsonResource} from "@/app/repository/util-repository";

const UUIDList = z.array(z.string().uuid());
const VirtualMachinesByUserResponse = z.record(UUIDList);
export type VirtualMachinesByUserResponse = z.infer<typeof VirtualMachinesByUserResponse>;

export function getVirtualMachines(baseUrl: string, basicAuth: BasicAuth) {
    return fetchGetJsonResource(baseUrl, '/api/vms', VirtualMachinesByUserResponse, undefined, basicAuth);
}

export function deleteVirtualMachine(baseUrl: string, vmId: string, basicAuth: BasicAuth) {
    return fetchDelete(baseUrl, `/api/vms/${vmId}`, undefined, basicAuth);
}

const VirtualMachineMaxThresholdResponse = z.object({
    global: z.number().int(),
    admin: z.number().int(),
    advanced: z.number().int(),
    basic: z.number().int()
})
export type VirtualMachineMaxThresholdResponse = z.infer<typeof VirtualMachineMaxThresholdResponse>;

export function getVirtualMachineMaxThreshold(baseUrl: string) {
    return fetchGetJsonResource(baseUrl, `/api/vms/max-threshold`, VirtualMachineMaxThresholdResponse);
}