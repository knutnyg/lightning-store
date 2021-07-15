import './App.css';
import {useState} from "react";
import QRCode from "qrcode.react"
import {createInvoice, Invoice, updateInvoice} from "./invoice/invoices";
import useInterval from "./hooks/useInterval";

function App() {
    const [invoice, setInvoice] = useState<Invoice | undefined>(undefined);

    useInterval(() => {
        console.log('checking hook')
        if (invoice && invoice?.inProgress) {
            console.log('should update invoice')
            updateInvoice(invoice)
                .then(_invoice => setInvoice(_invoice))
        }
    }, 500)

    return (
        <div className="Lightning Store">
            <header className="App-header">
                {invoice && <QRCode value={invoice.paymentRequest}/>}
                {invoice && invoice?.settled && <p>Please scan code with your device</p>}
                {!invoice ? <p>
                    Welcome to the my store
                </p> : <p>
                    Please scan QR code with your phone
                </p>}
                {!invoice && <button onClick={() => {
                    createInvoice()
                        .then(_invoice => setInvoice(_invoice))
                }}>Donate 10 sats
                </button>}
                {invoice?.settled && <p>Thank you!😎</p>}
            </header>
        </div>
    );
}

export default App;
