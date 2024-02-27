import {z} from "zod"

export interface BasicAuth {
    username: string;
    password: string;
}

export async function fetchGetJsonResource<T extends z.ZodType<any, any>>(
    baseUrl: string,
    path: string,
    schema: T,
    query?: Record<string, string>,
    basicAuth?: BasicAuth
): Promise<z.infer<T>> {

    const fullPath = new URL(path, baseUrl)
    if (query) {
        Object.keys(query).forEach(key => fullPath.searchParams.append(key, query[key]))
    }

    const headers = new Headers();
    if (basicAuth) {
        headers.set('Authorization', `Basic ${btoa(`${basicAuth.username}:${basicAuth.password}`)}`);
    }

    const response = await fetch(fullPath.toString(), {
        headers: headers
    });

    if (!response.ok) {
        throw await response.json()
    }
    const data = await response.json()
    return schema.parse(data)
}

export async function fetchDelete(
    baseUrl: string,
    path: string,
    query?: Record<string, string>,
    basicAuth?: BasicAuth
): Promise<void> {
    const fullPath = new URL(path, baseUrl)
    if (query) {
        Object.keys(query).forEach(key => fullPath.searchParams.append(key, query[key]))
    }

    const headers = new Headers();
    if (basicAuth) {
        headers.set('Authorization', `Basic ${btoa(`${basicAuth.username}:${basicAuth.password}`)}`);
    }

    const response = await fetch(fullPath.toString(), {
        method: 'DELETE',
        headers: headers
    });

    if (!response.ok) {
        throw await response.json()
    }
}

export async function postJsonResource<T extends z.ZodType<any, any>>(
    baseUrl: string,
    path: string,
    schema: T,
    body: object,
    basicAuth?: BasicAuth
): Promise<z.infer<T>> {
    const fullPath = new URL(path, baseUrl)

    const headers = new Headers();
    headers.set('Content-Type', 'application/json');
    if (basicAuth) {
        headers.set('Authorization', `Basic ${btoa(`${basicAuth.username}:${basicAuth.password}`)}`);
    }

    const response = await fetch(fullPath.toString(), {
        method: 'POST',
        headers: headers,
        body: JSON.stringify(body)
    });

    if (!response.ok) {
        throw await response.json()
    }
    const data = await response.json()
    return schema.parse(data)
}