import {User as U} from "lucide-react";

interface UserProps {
    username: string;
    className?: string;
}

export function User({username, className}: UserProps) {
    return (
        <div className={`px-2 inline-flex items-center justify-center whitespace-nowrap rounded-md text-sm font-medium ring-offset-background transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:pointer-events-none disabled:opacity-50 border border-input bg-background hover:bg-accent hover:text-accent-foreground ${className}`}>
            <U></U>
            <div className="ml-2">{username}</div>
        </div>
    )
}
