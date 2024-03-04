"use client"

import {useCredential, useUser, useVirtualMachineMaxThreshold} from "@/app/store";
import {useRouter} from 'next/navigation';
import {Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle,} from "@/components/ui/card"
import {Button} from "@/components/ui/button";

import {
    Carousel,
    CarouselApi,
    CarouselContent,
    CarouselItem,
    CarouselNext,
    CarouselPrevious,
} from "@/components/ui/carousel"
import {useEffect, useMemo, useRef, useState} from "react";
import {Theme} from "@/components/theme";
import {findSelectableVm, selectableVms} from "@/app/vm";
import {User} from "@/components/user";
import {Coins} from "@/components/coin";
import useSWR from "swr";
import {getMe} from "@/app/repository/user-repository";
import {Toaster} from "@/components/ui/sonner";
import {toast} from "sonner";
import {ScrollArea} from "@/components/ui/scroll-area";
import {HoverCard, HoverCardContent, HoverCardTrigger} from "@/components/ui/hover-card";
import {
    createVirtualMachine,
    CreateVirtualMachineRequest,
    deleteVirtualMachine,
    getVirtualMachineMaxThreshold,
    getVirtualMachines,
    VirtualMachinesByUserValue
} from "@/app/repository/vm-repository";
import {CopyIcon, FileSliders, Monitor, Rocket, Trash2} from "lucide-react";
import {Separator} from "@/components/ui/separator";
import {Input} from "@/components/ui/input";
import useSWRMutation from "swr/mutation";
import {Tooltip, TooltipContent, TooltipProvider, TooltipTrigger} from "@/components/ui/tooltip";
import {Badge} from "@/components/ui/badge";
import { Dialog, DialogClose, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle, DialogTrigger } from "@/components/ui/dialog";
import {Label} from "@/components/ui/label";
import StatusIndicator from "@/components/ui/status-indicator";
import * as generator from 'generate-password';
import {BASE_URL} from "@/app/env";

function useGetMe() {
    const {credential} = useCredential();

    const getMeFetcher = () => getMe(BASE_URL, credential !== null ? credential : {
        username: "",
        password: ""
    });
    const {data, error, mutate, isLoading} = useSWR('/api/users/me', getMeFetcher, {})

    return {
        user: data,
        userError: error,
        userIsLoading: isLoading,
        mutateUser: mutate
    }
}

function useGetListVirtualMachinesByUser() {
    const {credential} = useCredential()

    const getVirtualMachinesFetcher = () => getVirtualMachines(BASE_URL, credential !== null ? credential : {
        username: "",
        password: ""
    });

    const {data, error, mutate} = useSWR("/api/vms", getVirtualMachinesFetcher, {
        refreshInterval: 5000
    })

    return {
        virtualMachines: data,
        virtualMachinesError: error,
        mutateVirtualMachines: mutate
    }
}

function useGetVirtualMachineMaxThreshold() {
    const maxThresholdFetcher = () => getVirtualMachineMaxThreshold(BASE_URL);
    const {data} = useSWR("/api/vms/max-threshold", maxThresholdFetcher)

    return {
        maxThreshold: data
    }
}

function useDeleteVirtualMachine(vmId: string) {
    const {credential} = useCredential()
    const deleteVirtualMachineFetcher = () => deleteVirtualMachine(BASE_URL, vmId, credential !== null ? credential : {
        username: "",
        password: ""
    });

    const {trigger, data, error, isMutating} = useSWRMutation(`/api/vms/${vmId}`, deleteVirtualMachineFetcher, {})

    return {
        deleteVirtualMachine: trigger,
        deleteVirtualMachineData: data,
        deleteVirtualMachineError: error,
        deleteVirtualMachineIsMutating: isMutating
    }
}

function useCreateVirtualMachine() {
    const {credential} = useCredential()

    const createVirtualMachineFetcher = (_: any, {arg}: {
        arg: CreateVirtualMachineRequest
    }) => createVirtualMachine(BASE_URL, credential !== null ? credential : {
        username: "",
        password: ""
    }, arg);

    const {trigger, data, error, isMutating} = useSWRMutation("/api/vms", createVirtualMachineFetcher, {})

    return {
        createVirtualMachine: (req: CreateVirtualMachineRequest) => trigger(req), // Voici comment vous pouvez appeler trigger avec des paramètres
        createVirtualMachineData: data,
        createVirtualMachineError: error,
        createVirtualMachineIsMutating: isMutating
    }
}

export default function Home() {
    const {credential} = useCredential();
    const {setUser} = useUser()
    const router = useRouter()
    const {user, userError, mutateUser} = useGetMe()
    const {setMaxThreshold} = useVirtualMachineMaxThreshold()
    const {maxThreshold} = useGetVirtualMachineMaxThreshold()

    if (!credential) {
        router.push("/login")
    }

    useEffect(() => {
        if (maxThreshold) {
            setMaxThreshold(maxThreshold)
        }
    }, [maxThreshold, setMaxThreshold]);

    useEffect(() => {
        if (userError) {
            toast.error("Impossible de charger vos informations utilisateur", {
                description: "Veuillez réessayer.",
            })
        }
    }, [userError]);

    useEffect(() => {
        if (user) {
            setUser(user)
        }
    }, [setUser, user]);

    return (
        <main className="w-screen h-screen p-5 flex flex-col xl:flex-none gap-5 xl:grid xl:grid-cols-5 grid-rows-6">
            <Toaster richColors/>
            <Header className="col-span-5 row-span-1 xl:h-full"></Header>
            <NewVirtualMachine className="col-span-2 row-span-5"/>
            <ManageVirtualMachine className="col-span-3 row-span-5"/>
        </main>
    );
}

interface HeaderProps {
    className?: string
}

function Header({className}: HeaderProps) {
    const {setCredential} = useCredential()
    const {user} = useUser()
    const {maxThreshold} = useVirtualMachineMaxThreshold()
    const router = useRouter()

    const handleLogout = () => {
        setCredential(null)
    }

    const handleAzureLink = () => {
        router.push("https://azure.microsoft.com/fr-fr/")
    }

    const myMaxThreshold = () => {
        switch (user?.role) {
            case "admin":
                return maxThreshold.admin
            case "advanced":
                return maxThreshold.advanced
            case "basic":
                return maxThreshold.basic
            default:
                return 0
        }
    }

    return (
        <header className={`${className}`}>
            <Card className="xl:h-full">
                <CardHeader className="flex-row place-items-center justify-between">
                    <div>
                        <CardTitle>Azure Dev VM</CardTitle>
                        <CardDescription className="hidden md:block">
                            <span>Application de gestion pour des vms jetable sur </span>
                            <Button onClick={handleAzureLink} className="px-0 py-0"
                                    variant="link">azure</Button>
                            <span>.</span>
                        </CardDescription>
                    </div>
                    <div className="flex space-x-2.5 ml-5">
                        <Theme/>
                        <HoverCard openDelay={100} closeDelay={100}>
                            <HoverCardTrigger><Coins className="h-full"
                                                     coins={user !== null ? user.token : 0}/></HoverCardTrigger>
                            <HoverCardContent className={"w-full"}>
                                Vous avez un crédit de <span
                                className="font-bold text-primary">{user?.token} VMs</span> jetable
                            </HoverCardContent>
                        </HoverCard>
                        <HoverCard openDelay={100} closeDelay={100}>
                            <HoverCardTrigger><User className="h-full"
                                                    username={user !== null ? user.username : ""}/></HoverCardTrigger>
                            <HoverCardContent className={"w-full"}>
                                <div>Vous avez le role <span
                                    className={`font-bold ${roleColor(user?.role)}`}>{user?.role}</span></div>
                                <Separator className={"my-2"}></Separator>
                                <div>Vous avez une capacité de <span
                                    className="font-bold text-primary">{myMaxThreshold()} VMs</span></div>
                            </HoverCardContent>
                        </HoverCard>
                        <Button className={"mr-10"} onClick={handleLogout}>Se déconnecter</Button>
                    </div>
                </CardHeader>
            </Card>
        </header>
    )
}

function roleColor(role: string | undefined) {
    if (!role) return ""
    switch (role) {
        case "basic":
            return "text-pink-400"
        case "advanced":
            return "bg-gradient-to-r from-yellow-300 via-pink-300 to-red-500 text-transparent bg-clip-text"
        case "admin":
            return "bg-gradient-to-r from-fuchsia-300 via-purple-300 to-indigo-500 text-transparent bg-clip-text"
        default:
            return "";
    }
}

interface NewVirtualMachineProps {
    className?: string
}

function NewVirtualMachine({className}: NewVirtualMachineProps) {
    type StateNewVirtualMachine = 'selecting' | 'configuring' | 'deploying'

    const [api, setApi] = useState<CarouselApi>()
    const [current, setCurrent] = useState(0)
    const {user} = useUser()
    const {mutateUser} = useGetMe()
    const [state, setState] = useState<StateNewVirtualMachine>('selecting')
    const vmNameInputRef = useRef<HTMLInputElement>(null);
    const [vmNameInput, setVmNameInput] = useState("")
    const {mutateVirtualMachines} = useGetListVirtualMachinesByUser()
    const {maxThreshold} = useGetVirtualMachineMaxThreshold()
    const {virtualMachines} = useGetListVirtualMachinesByUser()

    const {
        createVirtualMachine,
        createVirtualMachineData,
        createVirtualMachineError,
        createVirtualMachineIsMutating
    } = useCreateVirtualMachine()

    const countVirtualMachines = useMemo(() => {
        if (!virtualMachines) return 0
        // @ts-ignore
        const vms = virtualMachines.virtualMachines[user?.username] || []
        return vms.length
    }, [user?.username, virtualMachines]);

    const getThreshold = useMemo(() => {
        if (!user || !maxThreshold) return 0
        switch (user.role) {
            case "admin":
                return maxThreshold.admin
            case "advanced":
                return maxThreshold.advanced
            case "basic":
                return maxThreshold.basic
            default:
                return 0
        }
    }, [maxThreshold, user]);

    const disableButton = useMemo(() => {
        if (!user) return "L'utilisateur n'est pas défini."

        if (user.token <= 0) {
            return "Vous n'avez plus de jeton pour créer une machine virtuelle.";
        }
        if (countVirtualMachines >= getThreshold) {
            return "Vous avez atteint votre limite de machines virtuelles.";
        }
        if (state === 'configuring' && vmNameInput.trim().length === 0) {
            return "Le nom de la machine virtuelle ne peut pas être vide.";
        } else if (state === 'deploying') {
            return "Déploiement en cours...";
        }
        return null;
    }, [user, countVirtualMachines, getThreshold, state, vmNameInput]);

    useEffect(() => {
        if (createVirtualMachineData) {
            setTimeout(async () => {
                try {
                    await Promise.all([mutateVirtualMachines(), mutateUser()])
                } catch (e) {
                    toast.error("Impossible de mettre à jour la liste des machines virtuelles", {
                        description: "Veuillez réessayer.",
                    })
                }

                setState('selecting')
            })
        }
    }, [createVirtualMachineData, mutateUser, mutateVirtualMachines]);

    useEffect(() => {
        if (createVirtualMachineError) {
            setState('configuring')
            toast.error("Impossible de créer la machine virtuelle", {
                description: "Veuillez réessayer.",
            })
        }
    }, [createVirtualMachineError]);

    useEffect(() => {
        if (createVirtualMachineIsMutating) {
            setState('deploying')
        }
    }, [createVirtualMachineIsMutating]);

    useEffect(() => {
        if (state === 'selecting') {
            setVmNameInput("")
        } else if (state === 'configuring') {
            vmNameInputRef.current?.focus();
        }
    }, [state]);

    useEffect(() => {
        if (!api) {
            return
        }

        setCurrent(api.selectedScrollSnap() + 1)

        api.on("select", () => {
            setCurrent(api.selectedScrollSnap() + 1)
            setState('selecting')
        })
    }, [api])

    const handleClickButton = async () => {
        if (state === 'selecting') {
            setState('configuring')
        } else {
            const selectableVm = selectableVms[current - 1];

            if (!user) return

            const password = generator.generate({
                length: 16,
                numbers: true,
                symbols: true,
                uppercase: true,
                lowercase: true,
                excludeSimilarCharacters: true,
                strict: true
            });


            switch (selectableVm.osType) {
                case "linux":
                    await createVirtualMachine({
                        name: vmNameInput,
                        type: "linux",
                        hostname: user.username,
                        rootUsername: user.username,
                        password: password,
                        azureImage: selectableVm.azureImage
                    })
                    break;
                case "windows":
                    await createVirtualMachine({
                        name: vmNameInput,
                        type: "windows",
                        hostname: user.username,
                        adminUsername: user.username,
                        password: password,
                        azureImage: selectableVm.azureImage
                    })
                    break;
            }
        }
    }

    const visibility = state === 'selecting' ? "hidden" : "visible"

    const ButtonContent = () => {
        const Content = () => {
            switch (state) {
                case 'selecting':
                    return <>
                        <FileSliders className="mx-2"/>
                        Configurer
                    </>
                case 'configuring':
                    return <>
                        <Rocket className="mx-2"/>
                        Déployer
                    </>
                case 'deploying':
                    return <>
                        <Rocket className="mx-2"/>
                        Déploiement...
                    </>
            }
        }

        return (
            disableButton === null ?
                <Button onClick={handleClickButton} className="w-full">
                    <Content/>
                </Button> :
                <TooltipProvider delayDuration={100}>
                    <Tooltip>
                        <TooltipTrigger className="cursor-not-allowed">
                            <Button disabled={true} className="w-full">
                                <Content/>
                            </Button>
                        </TooltipTrigger>
                        <TooltipContent>
                            {disableButton}
                        </TooltipContent>
                    </Tooltip>
                </TooltipProvider>
        )
    }

    return (
        <Card className={`flex flex-col flex-grow ${className}`}>
            <CardHeader>
                <CardTitle>Nouvelle machine virtuelle</CardTitle>
                <CardDescription>Sectionnez un template de machine virtuelle jetable.</CardDescription>
            </CardHeader>
            <CardContent className="flex-grow grid place-items-center">
                <div className="flex flex-col">
                    <Carousel setApi={setApi}>
                        <CarouselContent className="size-64 sm:size-96">
                            {
                                selectableVms.map((vm, index) => (
                                    <CarouselItem key={index}>
                                        <Card>
                                            <CardContent className="grid grid-rows-5 aspect-square p-6">
                                                <div className="row-span-4">
                                                    <picture>
                                                        <img src={vm.imageUrl} alt={vm.displayName}
                                                             className="w-full h-full object-contain select-none"/>
                                                    </picture>
                                                </div>
                                                <div className="row-span-1 grid justify-center content-end">
                                                    <span
                                                        className="text-sm font-semibold text-center">{vm.displayName}</span>
                                                </div>
                                            </CardContent>
                                        </Card>
                                    </CarouselItem>
                                ))
                            }
                        </CarouselContent>
                        <CarouselPrevious/>
                        <CarouselNext/>
                    </Carousel>
                    <Input ref={vmNameInputRef} value={vmNameInput}
                           onChange={event => setVmNameInput(event.target.value)} className={`mb-3 ${visibility}`}
                           placeholder="Nommer la machine virtuelle"></Input>
                    <ButtonContent/>
                </div>
            </CardContent>
        </Card>
    )
}

interface ManageVirtualMachineProps {
    className?: string
}

function ManageVirtualMachine({className}: ManageVirtualMachineProps) {
    const {credential} = useCredential()
    const {virtualMachines} = useGetListVirtualMachinesByUser()

    return (
        <Card className={`flex flex-col ${className} h-full`}>
            <CardHeader>
                <CardTitle>Gestion des machines virtuelles</CardTitle>
                <CardDescription>Controller vos machines virtuelles en 1 clic.</CardDescription>
            </CardHeader>
            <CardContent className="flex flex-grow overflow-hidden">
                <ScrollArea className="w-full h-full overflow-y-auto">
                    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                        {
                            virtualMachines ? Object
                                .values(virtualMachines.virtualMachines)
                                .map((vms, index) => {
                                    return vms.map((vm, index) => {
                                        return <ManagedVirtualMachine key={index} value={vm}/>
                                    })
                                }) : null
                        }
                    </div>
                </ScrollArea>
            </CardContent>
        </Card>
    )
}

interface ManagedVirtualMachineProps {
    value: VirtualMachinesByUserValue
}

function ManagedVirtualMachine({value}: ManagedVirtualMachineProps) {
    const {mutateVirtualMachines} = useGetListVirtualMachinesByUser()
    const {
        deleteVirtualMachine,
        deleteVirtualMachineIsMutating
    } = useDeleteVirtualMachine(value.machineId)

    const handleDelete = async () => {
        try {
            await deleteVirtualMachine()
        } catch (e) {
            toast.error("Impossible de supprimer la machine virtuelle", {
                description: "Veuillez réessayer.",
            })
            return
        }
        try {
            await mutateVirtualMachines()
        } catch (e) {
            toast.error("Impossible de mettre à jour la liste des machines virtuelles", {
                description: "Veuillez réessayer.",
            })
        }
    }

    const selectableVm = findSelectableVm(value.info.azureImage, value.info.type)

    return (
        <Card className="h-64 sm:h-72 flex flex-col">
            <CardHeader className="pb-2">
                <div className="flex">
                    <Badge variant="secondary">{value.info.name}</Badge>
                    <div className="flex-grow"></div>
                </div>
                <StatusIndicator status={value.state}/>
            </CardHeader>
            <CardContent className="flex-grow">
                <div className="flex flex-col gap-5 content-center mt-5">
                    {
                        selectableVm === null ?
                            <>
                                <Monitor className="w-full h-12"/>
                                <CardDescription className="text-center">{value.info.type} OS</CardDescription>
                            </> :
                            <>
                                <picture>
                                    <img src={selectableVm.imageUrl} alt={"a"} className="object-contain w-full h-12"/>
                                </picture>
                                <CardDescription className="text-center">{selectableVm.displayName}</CardDescription>
                            </>
                    }
                </div>
            </CardContent>
            <CardFooter className="flex-row gap-4">
                <InformationButton value={value}/>
                <Button disabled={deleteVirtualMachineIsMutating || value.state === "deleting" || value.state === "creating"} onClick={handleDelete} variant="destructive"><Trash2/></Button>
            </CardFooter>
        </Card>
    )
}

interface InformationButtonProps {
    value: VirtualMachinesByUserValue
}

function InformationButton({value}: InformationButtonProps) {
    type ElementType = 'text' | 'password'

    interface ElementProps {
        type: ElementType,
        label: string,
        labelNameValue: string,
        labelValue: string
    }

    const Element = ({label, labelValue, labelNameValue, type}: ElementProps) => {
        const handleCopy = async () => {
            await navigator.clipboard.writeText(labelValue)
        }

        return (
            <>
                <Label htmlFor={label} className="text-right">
                    {labelNameValue}
                </Label>
                <div className="col-span-3 flex gap-2 items-center">
                    <Input type={type} id={label} value={labelValue}/>
                    <Button onClick={handleCopy} type="submit" size="sm" className="px-3">
                        <span className="sr-only">Copy</span>
                        <CopyIcon className="size-4"/>
                    </Button>
                </div>
            </>
        )
    }

    const SSHButton = () => {
        const router = useRouter()

        const handleSSHLink = () => {
            router.push("https://kinsta.com/fr/blog/comment-utiliser-ssh/")
        }

        return (
            <Button onClick={handleSSHLink} className="px-0 py-0"
                    variant="link">SSH</Button>
        )
    }

    const RDPButton = () => {
        const router = useRouter()

        const handleSSHLink = () => {
            router.push("https://support.microsoft.com/fr-fr/windows/utilisation-du-bureau-%C3%A0-distance-5fe128d5-8fb1-7a23-3b8a-41e636865e8c")
        }

        return (
            <Button onClick={handleSSHLink} className="px-0 py-0 m-0"
                    variant="link">RDP</Button>
        )
    }

    return (
        <Dialog>
            <DialogTrigger asChild>
                <Button disabled={value.state === "deleting" || value.state === "creating"} variant="secondary" className="flex-grow">Information</Button>
            </DialogTrigger>
            <DialogContent className="sm:max-w-md">
                <DialogHeader>
                    <DialogTitle>Information de <span className="text-primary">{value.info.name}</span></DialogTitle>
                    <DialogDescription>
                        Pour vous connecter à la machine virtuelle, utilisez un client {value.info.type === "linux" ? <SSHButton/> : <RDPButton/>}.
                    </DialogDescription>
                </DialogHeader>
                <div className="grid gap-4 py-4">
                    <div className="grid grid-cols-4 items-center gap-4">
                        <Element label="hostname" labelValue={value.info.hostname} labelNameValue="Hostname" type="text"/>
                        {value.info.type === "linux" ?
                            <Element label="rootUsername" labelValue={value.info.rootUsername} labelNameValue="Root username" type="text"/> :
                            <Element label="adminUsername" labelValue={value.info.adminUsername} labelNameValue="Admin username" type="text"/>
                        }
                        <Element label="ip" labelValue={value.info.publicAddress !== null ? value.info.publicAddress : ""} labelNameValue="Addresse IP" type="text"/>
                        <Element label="password" labelValue={value.info.password} labelNameValue="Password" type="password"/>
                    </div>
                </div>
                <DialogFooter className="sm:justify-start">
                    <DialogClose asChild>
                        <Button type="button" variant="secondary">
                            Fermer
                        </Button>
                    </DialogClose>
                </DialogFooter>
            </DialogContent>
        </Dialog>
    )
}