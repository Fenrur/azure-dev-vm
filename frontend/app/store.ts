import {create} from 'zustand'
import {devtools, persist} from 'zustand/middleware'
import {Credential, Theme, User} from "@/app/types";

export interface CredentialState {
    credential: Credential | null,
    setCredential: (by: Credential | null) => void
}

export const useCredential = create<CredentialState>()(
    devtools(
        persist(
            (set) => ({
                credential: null,
                setCredential: (by) => set({credential: by})
            }),
            {
                name: "credential-storage"
            }
        )
    )
)

export interface UserState {
    user: User | null,
    setUser: (by: User | null) => void
}

export const useUser = create<UserState>()(
    devtools(
        persist(
            (set) => ({
                user: null,
                setUser: (by) => set({user: by})
            }),
            {
                name: "user-storage"
            }
        )
    )
)

export interface ThemeState {
    theme: Theme,
    setTheme: (by: Theme) => void
}

export const useTheme = create<ThemeState>()(
    devtools(
        persist(
            (set) => ({
                theme: "system",
                setTheme: (by) => set({theme: by})
            }),
            {
                name: "theme-storage"
            }
        )
    )
)