import {useState} from "react";
import {baseUrl} from "../App";

export interface User {
    userId: string,
    balance: number
}

export const updateUser = () => {
    const macaroon = localStorage.getItem('macaroon')
    const preimage = localStorage.getItem('preimage')

    if (!macaroon || !preimage) {
        return Promise.reject("User not logged in")
    }
    return fetch(`${baseUrl}/register`, {
            method: 'GET',
            headers: {
                'Access-Control-Allow-Origin': '*',
                'Authorization': `LSAT ${localStorage.getItem('macaroon')}:${localStorage.getItem('preimage')}`
            },
        }
    ).then(res => res.json() as Promise<User>)
}

export const useUser = () => {
    return useState<User | undefined>(undefined);
}

export const useTitle = () => {
    return useState<string>("Lightning Blog ⚡️");
}