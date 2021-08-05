import reportWebVitals from "./reportWebVitals";
import {useState} from "react";
import {updateInvoice, updateTokenInvoice} from "./invoice/invoices";
import {baseUrl} from "./App";
import QRCode from "qrcode.react";
import useInterval from "./hooks/useInterval";

interface Invoice {
    paymentRequest: string,
    settled: boolean
}

export const LSATView = () => {
    const [invoice, setInvoice] = useState<Invoice | undefined>(undefined);
    const [inRegister, setInRegister] = useState<Boolean>(false);

    useInterval(() => {
        if (invoice && !invoice.settled) {
            updateTokenInvoice()
                .then(_invoice => setInvoice({
                    paymentRequest: _invoice.paymentRequest,
                    settled: _invoice.settled !== null
                }))
        }
    }, 1000)

    return <div>
        <h2>Teqnique #1: Authentication</h2>
        <p>Lots of sites requires you to authenticate yourself. Most use a combination of username, email and passwords.
            One drawback with this method is that we spread personal details all over the world - even though these are
            not required to consume whatever service we register for. Another problem with the Internet today is all the
            robot traffic and spam we encounter every day. We have developed tools like CAPCHA to prove that we are
            humans, but the underlying problem still exists: Micropayments are troublesome making free signups the only
            option</p>
        <h3>Enter Lightning Service Authentication Token(LSAT)</h3>
        <p>Have you ever noticed how service providers charge your creditcard 1$ to validate it when you sign up for a
            subscription? Wouldn't it be amazing to do the same whenever someone creates an account for your
            service? These tokens can both be used for authentication and for paid APIs. One way to prevent spam and to
            shoo away the trolls
            is to require a tiny payment for every action. To an actual user these add up change but for automated
            robots they add up to real $$. To use my site you need to request a token from me. This is a one time
            authentication requiring you to make a micropayment in exchange for a valid token. Leveraging clever
            cryptografy the payment receipt is bound to the token making server side validation a purly mathematical
            task</p>
        <button onClick={() => {
            setInRegister(true);
            register().then(res => {
                setInvoice({
                    paymentRequest: res.invoice,
                    settled: false,
                })
                localStorage.setItem("macaroon", res.macaroon)
            })
        }}>Aquire a token
        </button>
        {inRegister && invoice && !invoice.settled && (<div>
            <QRCode value={invoice.paymentRequest}/>
            <p>Please scan QR code with your favorite lightning wallet and pay the invoice</p>
        </div>)}
        {inRegister && invoice && invoice.settled && (<p>
            Now you're golden 🤝
        </p>)}
    </div>
}

export const lookupPreimage = (): Promise<Response> => {
    return fetch(`${baseUrl}/register`, {
            method: 'GET',
            headers: {
                'Access-Control-Allow-Origin': '*',
                'Authorization': `LSAT ${localStorage.getItem('macaroon')}`
            }
        }
    )
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
        // Parse header
        .then(response => {
            if (response.status == 402) {
                console.log("payment is required")
                console.log(response.headers)
                console.log()

                const wwwchallenge = response.headers.get('WWW-Authenticate')!
                const type = wwwchallenge.split(' ')[0]
                const macaroon = wwwchallenge.split(' ')[1].slice(0, -1).split('=')[1].slice(1, -1)
                const invoice = wwwchallenge.split(' ')[2].split('=')[1].slice(1, -1)


                console.log(wwwchallenge)
                console.log('type', type)
                console.log('macaroon', macaroon)
                console.log('invoice', invoice)

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