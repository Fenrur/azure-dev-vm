import {VirtualMachineMaxThresholdResponse} from "@/app/repository/vm-repository";

export interface Credential {
    username: string,
    password: string
}

export interface User {
    username: string,
    role: string,
    token: number,
    maxVms: number
}

export type Theme = "light" | "dark" | "system"

export interface VirtualMachineMaxThreshold {
    global: number,
    admin: number,
    advanced: number,
    basic: number
}