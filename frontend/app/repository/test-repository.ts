import {z} from "zod";
import {fetchGetJsonResource} from "@/app/repository/util-repository";

const TestResponse = z.object({
    firstName: z.string(),
    lastName: z.string(),
})

export type TestResponse = z.infer<typeof TestResponse>;

export function test(baseUrl: string) {
    return fetchGetJsonResource(baseUrl, '/test', TestResponse);
}