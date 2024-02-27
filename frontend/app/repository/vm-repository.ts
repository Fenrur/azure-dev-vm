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