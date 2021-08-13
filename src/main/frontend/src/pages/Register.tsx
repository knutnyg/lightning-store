import {useEffect, useState} from "react";
import {baseUrl} from "../App";
import {updateUser, useUser} from "../hooks/useUser";
import {AccessState, PageProps} from "./Blog";
import {Link} from "react-router-dom";
import {InvoiceView} from "../invoice/Invoice";
import useInterval from "../hooks/useInterval";

interface Invoice {
    id?: string,
    paymentRequest: string,
    settled: boolean,
    preimage?: string
}

export const updateTokenInvoice = (): Promise<Invoice> => {
    return fetch(`${baseUrl}/open/register`, {
        method: 'GET',
        headers: {
            'Access-Control-Allow-Origin': '*',
            'Accept': 'application/json',
            'Authorization': `LSAT ${localStorage.getItem('macaroon')}`
        },
    })
        .then(response => (response.json() as Promise<Invoice>))
        .catch(err => {
            console.log(err)
            return Promise.reject()
        });
}

interface State {
    invoice?: Invoice,
    state: AccessState
}


export const LSATView = (props: PageProps) => {
    const [user, setUser] = useUser()
    const [state, setState] = useState<State>({
        invoice: undefined,
        state: AccessState.INITIAL
    })

    useEffect(() => {
        props.onChange("Registration")
    })
    useInterval(() => {
        if (state.state === AccessState.PAYMENT_PENDING) {
            if (state.invoice && !state.invoice.settled) {
                updateTokenInvoice()
                    .then(_invoice => {
                        if (_invoice.settled && _invoice.preimage) {
                            localStorage.setItem("preimage", _invoice.preimage!!)

                            updateUser()
                                .then(res => setUser(res))

                            setState({
                                invoice: {
                                    id: _invoice.id,
                                    paymentRequest: _invoice.paymentRequest,
                                    settled: _invoice.settled !== null,
                                    preimage: _invoice.preimage
                                },
                                state: AccessState.ACCESS
                            })
                        }
                    })
            }
        }

    }, 1000)

    const handleLogout = () => {
        localStorage.removeItem("macaroon");
        localStorage.removeItem("preimage");
        setState({...state, invoice: undefined, state: AccessState.INITIAL});
    }

    const handleRegister = (): Promise<void> => {
        return fetch(`${baseUrl}/register`, {
            method: 'PUT',
                headers: {
                    'Access-Control-Allow-Origin': '*',
                },
            }
        ).then(response => {
            if (response.status === 402) {
                const wwwchallenge = response.headers.get('WWW-Authenticate')!
                const type = wwwchallenge.split(' ')[0]
                const macaroon = wwwchallenge.split(' ')[1].slice(0, -1).split('=')[1].slice(1, -1)
                const invoice = wwwchallenge.split(' ')[2].split('=')[1].slice(1, -1)

                setState({
                    ...state, invoice: {
                        paymentRequest: invoice,
                        settled: false,
                    }, state: AccessState.PAYMENT_PENDING
                })
                localStorage.setItem("macaroon", macaroon)
            }
        })
            .catch(err => {
                console.log(err)
                return Promise.reject()
            });
    }
    const corruptLogin = () => !user && localStorage.getItem("macaroon")
    const hasMacaroon = () => localStorage.getItem("macaroon")

    return <div className="lsat-view">
        <p>Most sites require you to have an account to properly access their content. We are forced to spread our
            personal details on servers all over the world. With payments its even worse when hacked servers can lead to
            credit cards getting charged and money stolen. My site has none of that.</p>

        <h3>Lightning Service Authentication Token(LSAT)</h3>
        <p>Registering is as simple as paying a lightning invoice to purchase a token from my server. This token is
            cryptographically linked to your payment receipt and stored securely on your device. This powerful technique
            enables paid signups without exposing any personal details to my server.</p>

        {!hasMacaroon() && state.state !== AccessState.PAYMENT_PENDING &&
        <button onClick={handleRegister}>Aquire a token</button>}

        {state.state === AccessState.PAYMENT_PENDING && state.invoice && !state.invoice.settled && (
            <InvoiceView paymentReq={state.invoice.paymentRequest}/>)}

        {localStorage.getItem("macaroon") && localStorage.getItem("preimage") &&
        <p className="authenticated">Congratulations, you are authenticated 🤝</p>}
        <p>Want to learn more? Read the <a href="https://lsat.tech">docs</a> over at at Lightning Labs</p>

        <div className={"troubleshoot"}>
            <Link to="/">Back</Link>
            {corruptLogin() && <button onClick={handleLogout}>Destroy account</button>}
        </div>
    </div>
}
