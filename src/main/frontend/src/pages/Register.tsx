import {useEffect, useState} from "react";
import {baseUrl} from "../App";
import QRCode from "qrcode.react";
import useInterval from "../hooks/useInterval";
import {updateUser, useUser} from "../hooks/useUser";
import {PageProps, ThingState} from "./Blog";
import {Link} from "react-router-dom";

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
    state: ThingState
}


export const LSATView = (props: PageProps) => {
    const [user, setUser] = useUser()
    const [state, setState] = useState<State>({invoice: undefined, state: ThingState.INITIAL})

    useEffect(() => {
        props.onChange("Registration")
    })

    useInterval(() => {
        if (state.state === ThingState.PENDING) {
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
                                state: ThingState.ACCESS
                            })
                        }
                    })
            }
        }

    }, 1000)

    return <div className="lsat-view">
        <p>Most sites require you to have an account to properly access their content. We are forced to spread our
            personal details on servers all over the world. With payments its even worse when hacked servers can lead to
            credit cards getting charged and money stolen. My site has none of that.</p>

        <h3>Lightning Service Authentication Token(LSAT)</h3>
        <p>Registering is as simple as paying a lightning invoice to purchase a token from my server. This token is
            cryptographically linked to your payment receipt and stored securely on your device. This powerful technique
            enables paid signups without exposing any personal details to my server at all.</p>

        {!localStorage.getItem("macaroon") && <button onClick={() => {
            register()
                .then(res => {
                    setState({
                        ...state, invoice: {
                            paymentRequest: res.invoice,
                            settled: false,
                        }
                    })
                    localStorage.setItem("macaroon", res.macaroon)
                })
        }}>Aquire a token
        </button>}
        {localStorage.getItem("macaroon") && <button onClick={() => {
            localStorage.removeItem("macaroon");
            localStorage.removeItem("preimage");
            setState({...state, invoice:undefined, state: ThingState.INITIAL})
        }}>Reset login</button>}
        {localStorage.getItem("macaroon") && !localStorage.getItem("preimage") &&
        <button onClick={() => {
            updateTokenInvoice()
                .then(_invoice => {
                    if (_invoice.preimage) {
                        localStorage.setItem("preimage", _invoice.preimage!!)
                    }
                    setState({...state, invoice: {
                            id: _invoice.id,
                            paymentRequest: _invoice.paymentRequest,
                            settled: _invoice.settled !== null
                        }})
                })
        }}>Check for payment</button>}
        {state.state === ThingState.PENDING && state.invoice && !state.invoice.settled && (<div>
            <QRCode value={state.invoice.paymentRequest}/>
            <p>Please scan QR code with your favorite lightning wallet and pay the invoice</p>
        </div>)}
        {localStorage.getItem("macaroon") && localStorage.getItem("preimage") &&
        <p className="authenticated">Congratulations, you are authenticated 🤝</p>}
        <p>Want to learn more? Read the <a href="https://lsat.tech">docs</a> over at at Lightning Labs</p>
        <Link to="/">Back</Link>
    </div>
}

interface Challenge {
    macaroon: string
    invoice: string,
}

export const register = (): Promise<Challenge> => {
    return fetch(`${baseUrl}/register`, {
            method: 'PUT',
            headers: {
                'Access-Control-Allow-Origin': '*',
            },
        }
    )
        .then(response => {
            if (response.status === 402) {
                const wwwchallenge = response.headers.get('WWW-Authenticate')!
                const type = wwwchallenge.split(' ')[0]
                const macaroon = wwwchallenge.split(' ')[1].slice(0, -1).split('=')[1].slice(1, -1)
                const invoice = wwwchallenge.split(' ')[2].split('=')[1].slice(1, -1)

                return {
                    macaroon: macaroon,
                    invoice: invoice
                }
            } else {
                return {
                    macaroon: "macaroon",
                    invoice: "invoice"
                }
            }


        })

        .catch(err => {
            console.log(err)
            return Promise.reject()
        });
}