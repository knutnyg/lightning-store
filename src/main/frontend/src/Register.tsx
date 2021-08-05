import {useState} from "react";
import {baseUrl} from "./App";
import QRCode from "qrcode.react";
import useInterval from "./hooks/useInterval";

interface Invoice {
    id?: string,
    paymentRequest: string,
    settled: boolean,
    preimage?: string
}

export const updateTokenInvoice = (): Promise<Invoice> => {
    return fetch(`${baseUrl}/register`, {
        method: 'GET',
        headers: {
            'Access-Control-Allow-Origin': '*',
            'Accept': 'application/json',
            'Authorization': `LSAT ${localStorage.getItem('macaroon')}`
        },
    })
        .then(response => (response.json() as Promise<Invoice>))
        .then((raw) => {
            return raw
        })
        .catch(err => {
            console.log(err)
            return Promise.reject()
        });
}

export const LSATView = () => {
    const [invoice, setInvoice] = useState<Invoice | undefined>(undefined);
    const [inRegister, setInRegister] = useState<Boolean>(false);

    useInterval(() => {
        if (invoice && !invoice.settled) {
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
        }
    }, 1000)

    return <div className="lsat-view">
        <h2>Teqnique #1: Authentication</h2>
        <p>Lots of sites requires you to authenticate yourself. Most use a combination of username, email and passwords.
            One drawback with this method is that we spread personal details all over the world - even though these are
            not required to consume whatever service we register for. Another problem with the Internet today is all the
            robot traffic and spam we encounter every day. We have developed tools like CAPTCHA to prove that we are
            humans in response to the robots, but the underlying problem still exists: Micropayments are troublesome
            making free signups the only option.</p>
        <h3>Enter Lightning Service Authentication Token(LSAT)</h3>
        <p>Have you ever noticed how service providers charge your credit card 1$ to validate it when you sign up for a
            subscription? Wouldn't it be amazing to do the same whenever someone creates an account for your
            service or web site? The LSAT is a token that can both be used for authentication and paid APIs and its only
            valid after an successful micropayment. One way to prevent spam and to shoo away the trolls is to
            require a tiny payment for every
            action. To an actual user these payments adds up to change while robots add up to real $$ and become less
            viable. To use my
            site you need to request a token from me. This is a one time authentication requiring you to make a
            micropayment in exchange for a valid token. Leveraging clever cryptography the payment receipt is bound to
            the token making server side validation a purly mathematical task.</p>
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