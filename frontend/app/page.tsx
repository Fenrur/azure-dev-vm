"use client"

import {useCredential, useUser} from "@/app/store";
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
import {useEffect, useState} from "react";
import {Theme} from "@/components/theme";
import {vms} from "@/app/vm";
import {User} from "@/components/user";
import {Coins} from "@/components/coin";
import useSWR from "swr";
import {getMe} from "@/app/repository/user-repository";
import {Toaster} from "@/components/ui/sonner";
import {toast} from "sonner";
import {ScrollArea} from "@/components/ui/scroll-area";
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

    if (!credential) {
        router.push("/login")
    }

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
    const router = useRouter()

    const handleLogout = () => {
        setCredential(null)
    }

    const handleAzureLink = () => {
        router.push("https://azure.microsoft.com/fr-fr/")
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
                        <Coins coins={user !== null ? user.token : 0}/>
                        <User username={user !== null ? user.username : ""}/>
                        <Button className={"mr-10"} onClick={handleLogout}>Se déconnecter</Button>
                    </div>
                </CardHeader>
            </Card>
        </header>
    )
}

export function NewVirtualMachine() {
    const [api, setApi] = useState<CarouselApi>()
    const [current, setCurrent] = useState(0)

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
                    <Button onClick={handleDeploy}>Déployer</Button>
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
                <ScrollArea className="h-96">
                    {
                        Array
                            .from({length: 100})
                            .map((_, index) => (
                                <div key={index} className="mb-4 text-sm font-medium">
                                    {index}
                                    <Separator/>
                                </div>
                            ))
                    }
                </ScrollArea>
            </CardContent>
        </Card>
    )
}