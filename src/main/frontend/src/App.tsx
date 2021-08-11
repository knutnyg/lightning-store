import './App.css';
import {LSATView} from './Register';
import {updateUser, useUser} from "./hooks/useUser";
import useInterval from "./hooks/useInterval";
import {
    BrowserRouter as Router,
    Switch,
    Route,
    Link
} from "react-router-dom";
import {Invoice, InvoiceRaw, updateInvoice} from "./invoice/invoices";
import {useEffect, useState} from "react";
import QRCode from "qrcode.react";


export const baseUrl = "http://localhost:8081"

// export const baseUrl = "https://store-api.nygaard.xyz"

function App() {
    const [user, setUser] = useUser()
    useInterval(() => {
        if (!user) {
            updateUser()
                .then(_user => setUser(_user))
        }
    }, 3000)

    return (
        <div className="main">
            <header>
                <h1>Store</h1>
                {user && <span className="user">Balance: ${user?.balance}</span>}
                {!user && <span className="user">Not logged in</span>}
            </header>
            <div className="content">
                <Router>
                    <Switch>
                        <Route path="/lsat"><LSATView/></Route>
                        <Route path="/blog-paywall"><PaywallView/></Route>
                        <Route path="/bitcoin-network"><h1>Bitcoin Network</h1></Route>
                        <Route path="/lightning-network"><h1>Lightning Network</h1>
                            <p>The lightning network is a global
                                decentralized payment network leveraging the bitcoin block chain.
                                Trading some of the features (like offline wallets) for benefits like massively scalable
                                and near free instant transactions. This could pave the way for a brave new web 3.0
                                where micropayments replace advertisements and sale of your personal data as the main
                                way to fund a service or a web site.</p>
                        </Route>
                        {/*<Route path="/about">{about()}</Route>*/}
                        {/*<Route path="/about">{about()}</Route>*/}
                        <Route path="/">
                            <p>This site is a project for me to learn my way around programming for the lightning
                                network and all
                                the possibilities it brings to web 3.0. I will attempt to explore concepts and
                                techniques that
                                micropayments bring to the table.</p>
                            <p>To navigate this site you need to have lightning enabled bitcoin wallet and authenticate
                                yourself through paying a lightning invoice. Read more and authenticate yourself in
                                the <Link to="./LSAT">LSAT section</Link></p>
                            <h2>Table of content:</h2>
                            <h3>Concepts</h3>
                            <ul>
                                <li><Link to="./bitcoin-network">Bitcoin Network</Link></li>
                                <li><Link to="./lightning-network">Lightning Network</Link></li>
                                <li><Link to="./lsat">LSAT</Link></li>
                            </ul>
                            <h3>Techniques</h3>
                            <ul>
                                <li><Link to="./blog-paywall">Paywalling content</Link></li>
                            </ul>
                        </Route>
                    </Switch>
                    <Link to="./">Back</Link>
                </Router>
            </div>

            <footer>
                    <span>I would love suggestions to what more I could add to my store! Take a look at the code on <a
                        href={"https://github.com/knutnyg/lightning-store/"}>github</a>. </span>
                <span>Connect to my lightning node: 020deb273bd81cd6771ec3397403f2e74a3c22f8f4c052321c30e5c612cf538328@84.214.74.65:9735</span>
            </footer>
        </div>
    );
}

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

    return <div>
        <h2>Introducing paywalled content</h2>
        <p>Tired of finding articles behind paywalls requiring a monthly subscription on a news site you visit once a
            year? To read the rest of this article you need to buy it, however in the world of micropayments that does
            not need to be a cumbersome experience. Simply scan the QR-code and pay 50 satoshis for access</p>
        {blog && <div dangerouslySetInnerHTML={{__html: blog.payload}}/>}
        {!blog && <button onClick={createOrder}>Hit me</button>}
        {invoice &&
        <div className={"invoice-view"}>
            <QRCode value={invoice.paymentRequest}/>
            <p>Please scan QR code with your phone</p>
        </div>}


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

export default App;
