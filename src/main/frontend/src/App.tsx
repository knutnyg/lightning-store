import './App.css';
import {DonateView} from "./invoice/Invoice";
import {LSATView} from './Register';


// export const baseUrl = "http://localhost:8081"
export const baseUrl = "https://store-api.nygaard.xyz"

function App() {

    return (
        <div className="main">
            <div className="content">
                <h1>Welcome to the my store</h1>
                <p>This site is a project for me to learn my way around programming for the lightning network and all
                    the possibilities it brings to web 3.0. I will attempt to explore concepts and techniques that
                    micropayments brings to the table.</p>
                <p>The lightning network is a global decentralised payment network leveraging the bitcoin block chain.
                    Trading some of the features (like offline wallets) for benefits like massively scalable and near
                    free instant transactions. This could pave the way for a brave new web 3.0 where micropayments
                    replaces advertisements and sale of your personal data as the main way to fund a service or a web site.</p>
                <p>To navigate this site you need your own lightning wallet. There are many alternatives, but as a
                    quick-start I suggest the custodian wallet BlueWallet.</p>
                <LSATView/>
                <DonateView/>
                <footer>
                    <span>I would love suggestions to what more I could add to my store! Take a look at the code on <a
                        href={"https://github.com/knutnyg/lightning-store/"}>github</a>.</span>
                    <span>Connect to my lightning node: 020deb273bd81cd6771ec3397403f2e74a3c22f8f4c052321c30e5c612cf538328@84.214.74.65:9735</span>
                </footer>
            </div>
        </div>
    );
}

export default App;
