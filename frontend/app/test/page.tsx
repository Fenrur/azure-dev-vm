"use client"

import useSWR from 'swr'
import {SWRDevTools} from "swr-devtools";
import {Button} from "@/components/ui/button";
import {test} from "@/app/repository/test-repository";
import {Label} from "@/components/ui/label";

export default function TestPage() {
    const testFetcher = () => test("http://localhost:8080");
    const {data, error, isLoading, mutate} = useSWR("/test", testFetcher)

    if (error) return <div>failed to load</div>
    if (isLoading) return <div>loading...</div>
    if (!data) return <div>no data</div>

    return <div>
        <SWRDevTools>
            <Button onClick={() => mutate()}>reload</Button>
            <div>
                <Label>First Name: {data.firstName}</Label>
            </div>
            <div>
                <Label>Last Name: {data.lastName}</Label>
            </div>
        </SWRDevTools>
    </div>
}