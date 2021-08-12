import {useEffect, useState} from "react";
import {Invoice, InvoiceRaw, updateInvoice} from "./invoice/invoices";
import useInterval from "./hooks/useInterval";
import QRCode from "qrcode.react";
import {baseUrl} from "./App";

enum BlogState { INITIAL, NO_ACCESS, ACCESS}

export const PaywallView = () => {
    let state = BlogState.INITIAL
    const [blog, setBlog] = useState<Blog | undefined>(undefined);
    const [invoice, setInvoice] = useState<Invoice | undefined>(undefined);

    useEffect(() => {
        if (!blog) {
            fetchProduct("261dd820-cfc4-4c3e-a2c8-59d41eb44dfc") // burde hinte om invoice
                .then(blog => setBlog((blog)))
                .catch(_ => state = BlogState.NO_ACCESS)
        }
    })

    useInterval(() => {
        if (invoice && invoice?.inProgress) {
            updateInvoice(invoice)
                .then(_invoice => setInvoice(_invoice))
        }
        if (invoice && !invoice?.inProgress && state === BlogState.NO_ACCESS) {
            fetchProduct("261dd820-cfc4-4c3e-a2c8-59d41eb44dfc")
                .then(blog => {
                    setBlog(blog);
                    state = BlogState.ACCESS
                    setInvoice(undefined)
                })
                .catch(_ => state = BlogState.NO_ACCESS)
        }
    }, 500)

    const createOrder = () => {
        createOrderInvoice("261dd820-cfc4-4c3e-a2c8-59d41eb44dfc")
            .then(invoice => setInvoice(invoice))
    }

    return <div className="blog">
        <h2>Introducing paywalled content</h2>
        <p>Tired of finding articles behind paywalls requiring a monthly subscription on a news site you visit once a
            year? To read the rest of this article you need to buy it, however in the world of micropayments that does
            not need to be a cumbersome experience. Simply scan the QR-code and pay the invoice for access</p>
        {!blog && <button onClick={createOrder}>Hit me</button>}
        {invoice &&
        <div className={"invoice-view"}>
            <QRCode value={invoice.paymentRequest}/>
            <p>Please scan QR code with your phone</p>
        </div>}
        {blog && <div dangerouslySetInnerHTML={{__html: blog.payload}}/>}


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