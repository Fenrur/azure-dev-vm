"use client"

import {useCredential, useUser, useVirtualMachineMaxThreshold} from "@/app/store";
import {useRouter} from 'next/navigation';
import {Card, CardContent, CardDescription, CardHeader, CardTitle,} from "@/components/ui/card"
import {Button} from "@/components/ui/button";

import {
    Carousel,
    CarouselApi,
    CarouselContent,
    CarouselItem,
    CarouselNext,
    CarouselPrevious,
} from "@/components/ui/carousel"
import {useEffect, useMemo, useState} from "react";
import {Theme} from "@/components/theme";
import {vms} from "@/app/vm";
import {User} from "@/components/user";
import {Coins} from "@/components/coin";
import useSWR from "swr";
import {getMe} from "@/app/repository/user-repository";
import {Toaster} from "@/components/ui/sonner";
import {toast} from "sonner";
import {ScrollArea} from "@/components/ui/scroll-area";
import {HoverCard, HoverCardContent, HoverCardTrigger} from "@/components/ui/hover-card";
import {getVirtualMachineMaxThreshold} from "@/app/repository/vm-repository";
import {Space} from "lucide-react";
import {Separator} from "@/components/ui/separator";

function useGetMe() {
    const {credential} = useCredential();

    const getMeFetcher = () => getMe("http://localhost:8080", credential !== null ? credential : {
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

export default function Home() {
    const {credential} = useCredential();
    const {setUser} = useUser()
    const router = useRouter()

    const {user, userError, mutateUser} = useGetMe()

    const {setMaxThreshold} = useVirtualMachineMaxThreshold()
    const maxThresholdFetcher = () => getVirtualMachineMaxThreshold("http://localhost:8080");
    const {data: maxThresholdFetcherData} = useSWR("/api/vms/max-threshold", maxThresholdFetcher)

    if (!credential) {
        router.push("/login")
    }

    useEffect(() => {
        if (maxThresholdFetcherData) {
            setMaxThreshold(maxThresholdFetcherData)
        }
    }, [maxThresholdFetcherData]);

    useEffect(() => {
        if (userError) {
            toast.error("Impossible de charger vos informations", {
                description: "Veuillez réessayer.",
            })
        }
    }, [userError]);

    useEffect(() => {
        if (user) {
            setUser(user)
        }
    }, [user]);

    return (
        <main className="p-5 flex flex-col h-screen">
            <Toaster richColors/>
            <Header></Header>
            <div className="flex xl:flex-row xl:space-x-5 xl:space-y-0 space-y-5 flex-col mt-5 h-full flex-grow">
                <NewVirtualMachine/>
                <ManageVirtualMachine/>
            </div>
        </main>
    );
}

export function Header() {
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
        <header className="flex-grow">
            <Card>
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
                    <div className="flex space-x-2.5">
                        <Theme/>
                        <HoverCard openDelay={200} closeDelay={200}>
                            <HoverCardTrigger><Coins className="h-full"
                                                     coins={user !== null ? user.token : 0}/></HoverCardTrigger>
                            <HoverCardContent className={"w-full"}>
                                Vous avez un crédit de <span className="font-bold text-primary">{user?.token} VMs</span> jetable
                            </HoverCardContent>
                        </HoverCard>
                        <HoverCard openDelay={200} closeDelay={200}>
                            <HoverCardTrigger><User className="h-full"
                                                    username={user !== null ? user.username : ""}/></HoverCardTrigger>
                            <HoverCardContent className={"w-full"}>
                                <div>Vous avez le role <span
                                    className={`font-bold ${roleColor(user?.role)}`}>{user?.role}</span></div>
                                <Separator className={"my-2"}></Separator>
                                <div>Vous avez une capacité de <span className="font-bold text-primary">{myMaxThreshold()} VMs</span></div>
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

export function NewVirtualMachine() {
    const [api, setApi] = useState<CarouselApi>()
    const [current, setCurrent] = useState(0)
    const {user} = useUser()

    const disableDeployButton = useMemo(() => {
        if (user) {
            return user.token === 0
        }
        return true;
    }, [user]);

    useEffect(() => {
        if (!api) {
            return
        }

        setCurrent(api.selectedScrollSnap() + 1)

        api.on("select", () => {
            setCurrent(api.selectedScrollSnap() + 1)
        })
    }, [api])

    const handleDeploy = () => {
        const selectableVm = vms[current - 1];

        console.log(selectableVm)
    }

    return (
        <Card className="flex flex-col flex-grow">
            <CardHeader>
                <CardTitle>Nouvelle machine virtuelle</CardTitle>
                <CardDescription>Sectionnez un template de machine virtuelle jetable.</CardDescription>
            </CardHeader>
            <CardContent className="flex-grow grid place-items-center">
                <div className="flex flex-col">
                    <Carousel setApi={setApi}>
                        <CarouselContent className="size-64 sm:size-96">
                            {
                                vms.map((vm, index) => (
                                    <CarouselItem key={index}>
                                        <Card>
                                            <CardContent className="grid grid-rows-5 aspect-square p-6">
                                                <div className="row-span-4">
                                                    <img src={vm.imageUrl} alt={vm.displayName}
                                                         className="w-full h-full object-contain"/>
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
                    <Button disabled={disableDeployButton} onClick={handleDeploy}>Déployer</Button>
                </div>
            </CardContent>
        </Card>
    )
}

export function ManageVirtualMachine() {
    return (
        <Card className="flex-grow">
            <CardHeader>
                <CardTitle>Gestion des machines virtuelles</CardTitle>
                <CardDescription>Deploy your new project in one-click.</CardDescription>
            </CardHeader>
            <CardContent>
                <ScrollArea className="grid">

                </ScrollArea>
            </CardContent>
        </Card>
    )
}

export function ManagedVirtualMachine() {
    return (
        <Card>

        </Card>
    )
}