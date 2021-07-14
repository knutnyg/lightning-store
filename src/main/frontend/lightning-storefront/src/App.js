import './App.css';
import {useState} from "react";
import QRCode from "qrcode.react"
import {createInvoice, updateInvoice} from "./io/invoices";

const poll = () => {

}

function App() {
    const [invoice, setInvoice] = useState(undefined);
    const [status, setStatus] = useState({
        inFlow: false,
        settled: false,
    });


    console.log(invoice)
    console.log(status)

    return (
        <div className="Lightning Store">
            <header className="App-header">
                {invoice && <QRCode value={invoice.paymentRequest}/>}
                {invoice && invoice?.settled === false && <p>Please scan code with your device</p>}
                <p>
                    Welcome to the my store
                </p>
                <button onClick={() => {
                    createInvoice(setInvoice)
                    setStatus({...status, inFlow: true})
                }}>Donate 10 sats
                </button>
                {invoice && <button onClick={() => {
                    updateInvoice(invoice.id, setInvoice)
                }}>Update
                </button>}
                {invoice?.settled && <p>Thank you!😎</p>}
            </header>
        </div>
    );
}

export default App;
