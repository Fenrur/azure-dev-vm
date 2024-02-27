import {z} from "zod"
import {BasicAuth, fetchGetJsonResource} from "@/app/repository/util-repository";

const GetMeResponse = z.object({
    role: z.string(),
    token: z.number().int(),
    username: z.string(),
    maxVms: z.number().int()
})
export type GetMeResponse = z.infer<typeof GetMeResponse>

export async function getMe(baseUrl: string, basicAuth: BasicAuth) {
    return fetchGetJsonResource(baseUrl, '/api/users/me', GetMeResponse, undefined, basicAuth);
}