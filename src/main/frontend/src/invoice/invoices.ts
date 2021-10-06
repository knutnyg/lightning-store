import {baseUrl} from "../App";

export interface InvoiceRaw {
    id: string
    paymentRequest: string,
    settled?: string,
    memo: string
}

export interface Invoice extends InvoiceRaw {
    inProgress: boolean,
}

export const updateInvoice = (invoiceId: string): Promise<Invoice> => {
    console.log('Updating invoice:', invoiceId)
    return fetch(`${baseUrl}/invoices/${invoiceId}`, {
        method: 'GET',
        headers: {
            'Access-Control-Allow-Origin': '*',
            'Accept': 'application/json',
            'Authorization': `LSAT ${localStorage.getItem('macaroon')}:${localStorage.getItem("preimage")}`
        },
    })
        .then(response => (response.json() as Promise<InvoiceRaw>))
        .then((raw) => {
            return {
                ...raw,
                inProgress: !raw.settled
            }
        })
}

export const createInvoice = (amount: number, memo: string): Promise<Invoice> => {
    return fetch('https://store-api.nygaard.xyz/invoices', {
        method: 'POST',
        headers: {
            'Access-Control-Allow-Origin': '*',
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        },
        body: JSON.stringify({
            memo: memo,
            amount: amount
        })
    })
        .then(response => (response.json() as Promise<InvoiceRaw>))
        .then(raw =>
            ({...raw, inProgress: true})
        )
        .catch(err => {
            console.log(err)
            return Promise.reject()
        });
}