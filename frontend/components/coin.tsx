import { Coins as C } from "lucide-react";
import {useMemo} from "react";

interface CoinsProps {
    className?: string;
    coins: number;
}

export function Coins({className, coins}: CoinsProps) {
    const colorCss = coins === 0 ? "text-red-500" : ""

    return (
        <div className={`px-2 inline-flex items-center justify-center whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 border border-input bg-background hover:bg-accent hover:text-accent-foreground ${className}`}>
            <C></C>
            <div className={`ml-2 ${colorCss}`}>{coins}</div>
        </div>
    )
}