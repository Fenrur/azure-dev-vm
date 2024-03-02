import StatusIndicator from "@/components/ui/status-indicator";

export default function Test() {
    return (
        <div>
            <StatusIndicator status="creating"/>
            <StatusIndicator status="running"/>
            <StatusIndicator status="deleting"/>
        </div>
    )
}