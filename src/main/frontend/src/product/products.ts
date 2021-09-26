import {baseUrl} from "../App";
import {Invoice, InvoiceRaw} from "../invoice/invoices";

export interface Product {
    payload: string
}

export const fetchProduct = (id: string): Promise<Product | undefined> => {
    return fetch(`${baseUrl}/products/${id}`, {
        method: 'GET',
        headers: {
            'Access-Control-Allow-Origin': '*',
            'Accept': 'application/json',
            'Authorization': `LSAT ${localStorage.getItem('macaroon')}:${localStorage.getItem("preimage")}`
        },
    })
        .then(res => {
            if (res.status === 200) {
                return res.json() as Promise<Product>
            } else if (res.status === 402) {
                console.log("Payment required")
                return Promise.reject("Payment required")
            }
        })
        .catch(err => {
            console.log("err");
            throw err
        })
}

export const createOrderInvoice = (productId: string): Promise<Invoice> => {
    return fetch(`${baseUrl}/orders/invoice/${productId}`, {
        method: 'POST',
        headers: {
            'Access-Control-Allow-Origin': '*',
            'Accept': 'application/json',
            'Authorization': `LSAT ${localStorage.getItem('macaroon')}:${localStorage.getItem("preimage")}`
        },
    }).then(response => (response.json() as Promise<InvoiceRaw>))
        .then((raw) => {
            return {
                ...raw,
                inProgress: !raw.settled
            }
        })
        .catch(err => {
            throw err
        })
}