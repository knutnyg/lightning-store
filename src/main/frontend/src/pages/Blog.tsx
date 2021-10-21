import {useEffect, useState} from "react";
import {Invoice, updateInvoice} from "../invoice/invoices";
import {InvoiceView} from "../invoice/Invoice";
import useInterval from "../hooks/useInterval";
import {Link} from "react-router-dom";
import {createOrderInvoice, fetchProduct, Product} from "../product/products";

export enum AccessState {
    INITIAL,
    PAYMENT_REQUIRED,
    PAYMENT_PENDING,
    ACCESS,
    PENDING_REGISTER
}

export interface State {
    access: AccessState
    invoice?: Invoice,
    product?: Product,
}

export interface PageProps {
    onChange: (title: string) => void;
}

export const PaywallView = (props: PageProps) => {
    const [state, setState] = useState<State>({
        invoice: undefined,
        product: undefined,
        access: AccessState.INITIAL
    })

    useEffect(() => {
        props.onChange("Introducing paywalled content")
        if (state.access === AccessState.INITIAL) {
            fetchProduct("261dd820-cfc4-4c3e-a2c8-59d41eb44dfc")
                .then(blog => setState({...state, product: blog, access: AccessState.ACCESS}))
                .catch(res => setState({...state, access: AccessState.PAYMENT_REQUIRED}))
        }
    })

    useInterval(() => {
        if (state.access === AccessState.PAYMENT_PENDING) {
            if (state.invoice && !state.invoice?.settled) {
                updateInvoice(state.invoice.id)
                    .then(invoice => {
                            setState({...state, invoice: invoice})
                        }
                    )
            }
            if (state.invoice?.settled) {
                fetchProduct("261dd820-cfc4-4c3e-a2c8-59d41eb44dfc")
                    .then(blog => {
                        setState({...state, product: blog, access: AccessState.ACCESS, invoice: undefined})
                    })
                    .catch(_ => setState({...state, access: AccessState.PAYMENT_REQUIRED, invoice: undefined}))
            }
        }
    }, 1000)

    const createOrder = () => {
        createOrderInvoice("261dd820-cfc4-4c3e-a2c8-59d41eb44dfc")
            .then(invoice => setState({...state, invoice: invoice, access: AccessState.PAYMENT_PENDING}))
    }

    return <div className="page">
        <p>Tired of finding articles behind paywalls requiring a monthly subscription on a news site you visit once a
            year? To read the rest of this article you need to buy it, however in the world of micropayments that does
            not need to be a cumbersome experience. Simply scan the QR-code and pay the invoice for access.</p>

        {state.access === AccessState.PAYMENT_REQUIRED && <button onClick={createOrder}>Buy article</button>}
        {state.access === AccessState.PAYMENT_PENDING && state.invoice &&
        <InvoiceView paymentReq={state.invoice.paymentRequest} description={""}/>}
        {state.access === AccessState.ACCESS && state.product &&
        <div className="block" dangerouslySetInnerHTML={{__html: state.product.payload}}/>}
        <Link to="/">Back</Link>
    </div>
}


