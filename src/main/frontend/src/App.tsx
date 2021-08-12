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
import {PaywallView} from "./Blog";


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


export default App;
