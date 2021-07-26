import './App.css';
import {DonateView} from "./invoice/Invoice";
import {RegisterView} from './Register';

function App() {

    return (
        <div className="main">
            <h1>Welcome to the my store</h1>
            <RegisterView/>
            <DonateView/>
        </div>
    );
}

export default App;
