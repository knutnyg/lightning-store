import {useEffect, useState} from "react";
import {Invoice, InvoiceRaw} from "../invoice/invoices";
import {baseUrl} from "../App";
import {InvoiceView} from "../invoice/Invoice";

export enum ThingState { INITIAL, NO_ACCESS, ACCESS, PENDING}

interface State {
    access: ThingState
    invoice?: Invoice,
    blog?: Blog,
}

export interface PageProps {
    onChange: (title:string) => void;
}

export const PaywallView = (props: PageProps) => {
    const [state, setState] = useState<State>({
        invoice: {
            id: '123',
            paymentRequest: 'string',
            settled: 'false',
            memo: 'memo',
            inProgress: true
        },
        blog: undefined,
        access: ThingState.PENDING
    })

    useEffect(() => {
        props.onChange("Paywall")
        if (state.access === ThingState.INITIAL) {
            fetchProduct("261dd820-cfc4-4c3e-a2c8-59d41eb44dfc")
                .then(blog => setState({...state, blog: blog, access: ThingState.ACCESS}))
                .catch(res => setState({...state, access: ThingState.NO_ACCESS}))
        }
    })

    // useInterval(() => {
    //     if (state.access === BlogState.PENDING && 1===2) {
    //         if (state.invoice && !state.invoice?.settled) {
    //             updateInvoice(state.invoice)
    //                 .then(invoice => {
    //                         setState({...state, invoice: invoice})
    //                     }
    //                 )
    //         }
    //         if (state.invoice?.settled) {
    //             fetchProduct("261dd820-cfc4-4c3e-a2c8-59d41eb44dfc")
    //                 .then(blog => {
    //                     setState({...state, blog: blog, access: BlogState.ACCESS, invoice: undefined})
    //                 })
    //                 .catch(_ => setState({...state, access: BlogState.NO_ACCESS}))
    //         }
    //     }
    // }, 1000)

    const createOrder = () => {
        createOrderInvoice("261dd820-cfc4-4c3e-a2c8-59d41eb44dfc")
            .then(invoice => setState({...state, invoice: invoice, access: ThingState.PENDING}))
    }

    return <div className="blog">
        <h2>Introducing paywalled content</h2>
        <p>Tired of finding articles behind paywalls requiring a monthly subscription on a news site you visit once a
            year? To read the rest of this article you need to buy it, however in the world of micropayments that does
            not need to be a cumbersome experience. Simply scan the QR-code and pay the invoice for access</p>

        {state.access === ThingState.NO_ACCESS && <button onClick={createOrder}>Hit me</button>}
        {state.access === ThingState.PENDING && state.invoice &&
            <InvoiceView paymentReq={state.invoice.paymentRequest}/>}
        {state.access === ThingState.ACCESS && state.blog &&
        <div dangerouslySetInnerHTML={{__html: state.blog.payload}}/>}
    </div>
}

interface Blog {
    payload: string
}

const createOrderInvoice = (productId: string): Promise<Invoice> => {
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

const fetchProduct = (id: string): Promise<Blog | undefined> => {
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
                return res.json() as Promise<Blog>
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