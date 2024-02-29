export interface AzureImage {
    publisher: string,
    offer: string,
    sku: string,
}

export type OsType = 'linux' | 'windows'

export interface SelectableVm {
    displayName: string,
    imageUrl: string,
    azureImage: AzureImage,
    osType: OsType
}

export const vms: SelectableVm[] = [
    {
        displayName: "Ubuntu Server 22.04 LTS",
        imageUrl: "/images/ubuntu.svg",
        azureImage: {
            publisher: "canonical",
            offer: "0001-com-ubuntu-server-jammy",
            sku: "22_04-lts-gen2"
        },
        osType: "linux"
    },
    {
        displayName: "Debian 12",
        imageUrl: "/images/debian.svg",
        azureImage: {
            publisher: "debian",
            offer: "debian-12",
            sku: "12-gen2"
        },
        osType: "linux"
    },
    {
        displayName: "Red Hat Enterprise Linux 9.3",
        imageUrl: "/images/rhel.svg",
        azureImage: {
            publisher: "redhat",
            offer: "RHEL",
            sku: "93-gen2"
        },
        osType: "linux"
    },
    {
        displayName: "Windows 11 Pro 2023",
        imageUrl: "/images/windows11.svg",
        azureImage: {
            publisher: "microsoftwindowsdesktop",
            offer: "windows-11",
            sku: "win11-23h2-pro"
        },
        osType: "windows"
    },
    {
        displayName: "Windows 10 Pro 2022",
        imageUrl: "/images/windows10.svg",
        azureImage: {
            publisher: "microsoftwindowsdesktop",
            offer: "windows-10",
            sku: "win10-22h2-pro"
        },
        osType: "windows"
    },
    {
        displayName: "Windows Server 2022",
        imageUrl: "/images/windows-server.svg",
        azureImage: {
            publisher: "microsoftwindowsserver",
            offer: "windowsserver",
            sku: "2022-datacenter-azure-edition"
        },
        osType: "windows"
    }
]