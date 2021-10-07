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

    return <div>
        <div className={"flex-container"}>
            <div className="grow">
                <p>Velkommen til galleriet! For å komme inn må du kjøpe en billett. Det kan du gjøre under.</p>
                {state.state === RegisterState.INITIAL &&
                <button className="block-xl" onClick={buyAccess}>Kjøp billett</button>}
                {state.state === RegisterState.REGISTER_PENDING && state.register &&
                <InvoiceView paymentReq={state.register.paymentRequest}/>}
                {state.state === RegisterState.LOGGED_IN && <p>Takk! Velkommen inn ➡️</p>}
            </div>
            <Link to="/about">Om galleriet</Link>
        </div>
    </div>
}
