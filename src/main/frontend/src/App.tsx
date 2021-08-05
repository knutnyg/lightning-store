import './App.css';
import {DonateView} from "./invoice/Invoice";
import {RegisterView} from './Register';

function App() {

    return (
        <div className="main">
            <h1>Welcome to the my store</h1>
            <RegisterView/>
            <DonateView/>
            <footer>
                <span>I would love suggestions to what more I could add to my store! Take a look at the code on <a href={"https://github.com/knutnyg/lightning-store/"}>github</a>. </span>
                <span>Connect to my lightning node: 020deb273bd81cd6771ec3397403f2e74a3c22f8f4c052321c30e5c612cf538328@84.214.74.65:9735</span>
            </footer>
        </div>
    );
}

export default App;
