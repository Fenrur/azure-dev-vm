import React from 'react';
import "./status-indicator.css";

// Définition des props du composant avec TypeScript
interface StatusIndicatorProps {
    status: 'creating' | 'running' | 'deleting' | 'deleted';
}

const StatusIndicator: React.FC<StatusIndicatorProps> = ({status}) => {

    const statusColor = status === 'creating'
        ? 'bg-orange-500'
        : status === 'running'
            ? 'bg-green-500'
            : 'bg-red-500';

    const text = status === 'creating'
        ? 'creation'
        : status === 'running'
            ? 'en ligne'
            : 'suppression';

    const animationClass = 'animate-pulse';

    return (
        <div className="flex items-center">
            <span className={`inline-block size-2 rounded-full ${statusColor} ${animationClass} mr-2`}></span>
            <span className={"text-xs"}>{text}</span>
        </div>
    );
};

export default StatusIndicator;
