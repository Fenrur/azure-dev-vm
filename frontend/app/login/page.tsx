"use client"

import {Button} from "@/components/ui/button"
import {Card, CardContent, CardDescription, CardFooter, CardHeader, CardTitle,} from "@/components/ui/card"
import {Input} from "@/components/ui/input"
import {Label} from "@/components/ui/label"
import {useCredential, useUser} from "@/app/store";
import {useEffect, useState} from "react"
import {getMe} from "@/app/repository/user-repository";
import {toast, Toaster} from "sonner"
import {useRouter} from 'next/navigation';
import useSWRMutation from "swr/mutation";

export function CardWithForm() {
    const {setCredential, credential} = useCredential()
    const {setUser} = useUser()

    const [username, setUsername] = useState("")
    const [password, setPassword] = useState("")

    const getMeFetcher = () => getMe("http://localhost:8080", {username, password});
    const {data: userData, error: errorUserData, trigger, isMutating} = useSWRMutation('/api/users/me', getMeFetcher, {})

    const router = useRouter();

    useEffect(() => {
        if (credential) {
            router.push("/")
        }
    }, [credential]);

    useEffect(() => {
        if (userData) {
            setCredential({username, password})
            setUser(userData)
        }
    }, [userData]);

    useEffect(() => {
        if (errorUserData) {
            toast.error("Utilisateur ou mot de passe invalide", {
                description: "Veuillez réessayer.",
            })
        }
    }, [errorUserData]);

    const handleLoginClick = async () => {
        try {
            await trigger()
        } catch (e) {

        }
    }

    return (
        <Card className="w-[350px]">
            <CardHeader>
                <CardTitle>Connection</CardTitle>
                <CardDescription>Veuillez entrer vos informations de connexion pour accéder à votre espace personnel.</CardDescription>
            </CardHeader>
            <CardContent>
                <form>
                    <div className="grid w-full items-center gap-4">
                        <div className="flex flex-col space-y-1.5">
                            <Label htmlFor="username">Nom d'utilisateur</Label>
                            <Input value={username} onChange={event => setUsername(event.target.value)} id="username"
                                   placeholder="Mon nom d'utilisateur"/>
                        </div>
                        <div className="flex flex-col space-y-1.5">
                            <Label htmlFor="password">Mot de passe</Label>
                            <Input value={password} onChange={event => setPassword(event.target.value)} type="password"
                                   id="password" placeholder="Mon mot de passe"/>
                        </div>
                    </div>
                </form>
            </CardContent>
            <CardFooter className="flex justify-between flex-row-reverse">
                <Button disabled={isMutating} onClick={handleLoginClick}>Login</Button>
            </CardFooter>
        </Card>
    )
}

export default function LoginPage() {
    return (
        <main>
            <div className="grid h-screen place-items-center">
                <CardWithForm/>
            </div>
            <Toaster richColors/>
        </main>
    )
}