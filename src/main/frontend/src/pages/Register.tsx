import {useEffect, useState} from "react";
import {baseUrl} from "../App";
import QRCode from "qrcode.react";
import useInterval from "../hooks/useInterval";
import {updateUser, useUser} from "../hooks/useUser";
import {PageProps} from "./Blog";
import {Link} from "react-router-dom";

interface Invoice {
    id?: string,
    paymentRequest: string,
    settled: boolean,
    preimage?: string
}

interface State {
    invoice?: Invoice,
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

export const LSATView = (props: PageProps) => {
    const [invoice, setInvoice] = useState<Invoice | undefined>(undefined);
    const [inRegister, setInRegister] = useState<Boolean>(false);
    const [user, setUser] = useUser()

    useEffect(() => {
        props.onChange("Registration")
        window.scrollTo(0, 0);
    })

    useInterval(() => {
        if (invoice && !invoice.settled) {
            updateTokenInvoice()
                .then(_invoice => {
                    if (_invoice.preimage) {
                        localStorage.setItem("preimage", _invoice.preimage!!)
                        updateUser()
                            .then(res => setUser(res))
                    }
                    setInvoice({
                        id: _invoice.id,
                        paymentRequest: _invoice.paymentRequest,
                        settled: _invoice.settled !== null
                    })
                })
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
            setInRegister(true);
            register().then(res => {
                setInvoice({
                    paymentRequest: res.invoice,
                    settled: false,
                })
                localStorage.setItem("macaroon", res.macaroon)
            })
        }}>Aquire a token
        </button>}
        {localStorage.getItem("macaroon") && <button onClick={() => {
            localStorage.removeItem("macaroon");
            localStorage.removeItem("preimage");
            setInvoice(undefined)
        }}>Reset login</button>}
        {localStorage.getItem("macaroon") && !localStorage.getItem("preimage") && !inRegister &&
        <button onClick={() => {
            updateTokenInvoice()
                .then(_invoice => {
                    if (_invoice.preimage) {
                        localStorage.setItem("preimage", _invoice.preimage!!)
                    }
                    setInvoice({
                        id: _invoice.id,
                        paymentRequest: _invoice.paymentRequest,
                        settled: _invoice.settled !== null
                    })
                })
        }}>Check for payment</button>}
        {inRegister && invoice && !invoice.settled && (<div>
            <QRCode value={invoice.paymentRequest}/>
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