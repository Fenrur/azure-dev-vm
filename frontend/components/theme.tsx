import {useTheme} from "@/app/store";
import {useEffect} from "react";

import {Moon, Sun} from "lucide-react"

import {Button} from "@/components/ui/button"
import {
    DropdownMenu,
    DropdownMenuCheckboxItem,
    DropdownMenuContent,
    DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"

export function Theme() {
    const {theme, setTheme} = useTheme()

    useEffect(() => {
        const root = window.document.documentElement

        root.classList.remove("light", "dark")

        if (theme === "system") {
            const systemTheme = window.matchMedia("(prefers-color-scheme: dark)")
                .matches
                ? "dark"
                : "light"

            root.classList.add(systemTheme)
            return
        }

        root.classList.add(theme)
    }, [theme])

    return (
        <DropdownMenu>
            <DropdownMenuTrigger asChild>
                <Button variant="outline" size="icon">
                    <Sun
                        className="h-[1.2rem] w-[1.2rem] rotate-0 scale-100 transition-all dark:-rotate-90 dark:scale-0"/>
                    <Moon
                        className="absolute h-[1.2rem] w-[1.2rem] rotate-90 scale-0 transition-all dark:rotate-0 dark:scale-100"/>
                    <span className="sr-only">Toggle theme</span>
                </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent>
                <DropdownMenuCheckboxItem checked={theme == "light"} onClick={() => setTheme("light")}>
                    Jour
                </DropdownMenuCheckboxItem>
                <DropdownMenuCheckboxItem checked={theme == "dark"} onClick={() => setTheme("dark")}>
                    Nuit
                </DropdownMenuCheckboxItem>
                <DropdownMenuCheckboxItem checked={theme == "system"} onClick={() => setTheme("system")}>
                    Système
                </DropdownMenuCheckboxItem>
            </DropdownMenuContent>
        </DropdownMenu>

    )
}