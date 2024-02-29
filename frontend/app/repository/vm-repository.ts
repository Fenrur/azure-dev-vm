import {z} from "zod";
import {BasicAuth, fetchDelete, fetchGetJsonResource, fetchPostJsonResource} from "@/app/repository/util-repository";

const AzureImage = z.object({
    publisher: z.string(),
    offer: z.string(),
    sku: z.string()
})
export type AzureImage = z.infer<typeof AzureImage>;

const VirtualMachineInformationLinux = z.object({
    type: z.literal('linux'),
    hostname: z.string(),
    rootUsername: z.string(),
    password: z.string(),
    azureImage: AzureImage,
    publicAddress: z.string().nullable()
})

const VirtualMachineInformationWindows = z.object({
    type: z.literal('windows'),
    hostname: z.string(),
    adminUsername: z.string(),
    password: z.string(),
    azureImage: AzureImage,
    publicAddress: z.string().nullable()
})

const VirtualMachineInformation = z.union([VirtualMachineInformationLinux, VirtualMachineInformationWindows]);
export type VirtualMachineInformation = z.infer<typeof VirtualMachineInformation>;

const VirtualMachineState = z.enum(['creating', 'running', 'deleting', 'deleted'])
export type VirtualMachineState = z.infer<typeof VirtualMachineState>;

const VirtualMachinesByUserValue = z.object({
    machineId: z.string().uuid(),
    spec: VirtualMachineInformation,
    state: VirtualMachineState
})

const VirtualMachinesByUserResponse = z.object({
    virtualMachines: z.record(z.string(), z.array(VirtualMachinesByUserValue)),
});
export type VirtualMachinesByUserResponse = z.infer<typeof VirtualMachinesByUserResponse>;

export function getVirtualMachines(baseUrl: string, basicAuth: BasicAuth) {
    return fetchGetJsonResource(baseUrl, '/api/vms', VirtualMachinesByUserResponse, undefined, basicAuth);
}

export function deleteVirtualMachine(baseUrl: string, vmId: string, basicAuth: BasicAuth) {
    return fetchDelete(baseUrl, `/api/vms/${vmId}`, undefined, basicAuth);
}

interface CreateVirtualMachineRequestLinux {
    type: 'linux',
    hostname: string,
    rootUsername: string,
    password: string,
    azureImage: AzureImage
}

interface CreateVirtualMachineRequestWindows {
    type: 'windows',
    hostname: string,
    adminUsername: string,
    password: string,
    azureImage: AzureImage
}

export type CreateVirtualMachineRequest = CreateVirtualMachineRequestLinux | CreateVirtualMachineRequestWindows;

const CreateVirtualMachineResponse = z.object({
    machineId: z.string().uuid()
})

export function createVirtualMachine(baseUrl: string, basicAuth: BasicAuth, req: CreateVirtualMachineRequest) {
    return fetchPostJsonResource(baseUrl, '/api/vms', CreateVirtualMachineResponse, req, basicAuth);
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