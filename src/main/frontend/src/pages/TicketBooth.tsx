import {handleRegister, Register, RegisterState, updateTokenInvoice} from "./Register";
import {useEffect, useState} from "react";
import {InvoiceView} from "../invoice/Invoice";
import useInterval from "../hooks/useInterval";
import {PageWithUserProps} from "./Kunstig";
import {Link} from "react-router-dom";

interface State {
    register?: Register,
    state: RegisterState
}

const initialState = {
    state: RegisterState.INITIAL,
    register: undefined
}

const pendingState = {
    state: RegisterState.REGISTER_PENDING,
    register: {
        paymentRequest: "awdawdawd",
        macaroon: "awdawdawdawdd",
    }
}

export const TicketBooth = (props: PageWithUserProps) => {
    const [state, setState] = useState<State>(initialState)

    useEffect(() => {
        props.onChange("Billettluka")
    })

    useInterval(() => {
        if (state.state === RegisterState.REGISTER_PENDING) {
            updateTokenInvoice()
                .then(invoice => {
                    if (invoice.settled) {
                        localStorage.setItem("preimage", invoice.preimage!!)
                        props.updateUser()
                        setState({
                            state: RegisterState.LOGGED_IN,
                            register: undefined
                        })
                    }
                })
                .catch(err => {
                    console.log(err)
                })
        }
    }, 2000)

    const buyAccess = () => {
        handleRegister()
            .then((register) => {
                setState({
                    ...state,
                    register: register,
                    state: RegisterState.REGISTER_PENDING
                })
                localStorage.setItem("macaroon", register.macaroon)
            })
            .catch((error) => {
                console.log(error)
            })
    }

    return <div className="ticketbooth-container">
        <div className="ticketbooth">
            <h1>Kunstig</h1>
            <p>To remember you and your paintings Kunstig creates a momento using the Lightning Network ⚡️</p>
            {state.state === RegisterState.INITIAL &&
                <button className="button block-xl" onClick={buyAccess}>Create momento</button>}
            {state.state === RegisterState.REGISTER_PENDING && state.register &&
                <InvoiceView paymentReq={state.register.paymentRequest}
                             description={"Scan the QR-koden and pay using a ⚡️-wallet."}/>}
            {state.state === RegisterState.LOGGED_IN && <p>Takk! Velkommen inn ➡️</p>}
        </div>
        <Link to="/about">About Kunstig</Link>
        <Link to={"/"}>Back</Link>
    </div>
}
