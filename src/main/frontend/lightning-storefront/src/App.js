import './App.css';
import {useState} from "react";
import QRCode from "qrcode.react"

function App() {
    const [invoice, setInvoice] = useState(undefined);

    return (
        <div className="Lightning Store">
            <header className="App-header">
                {invoice && <QRCode value={invoice.paymentRequest}/>}
                <p>
                    Welcome to the my store
                </p>
                <button onClick={() => {
                    fetch('http://localhost:8000/invoices', {
                        method: 'POST',
                        headers: {
                            'Access-Control-Allow-Origin': '*',
                            'Content-Type': 'application/json',
                            'Accept': 'application/json'
                        },
                        body: JSON.stringify({memo: "Chancellor on brink of second bailout for banks"})
                    })
                        .then(response => response.json())
                        .then(data => setInvoice(data))
                        .catch(err => console.log(err));
                }}>Donate 10 sats
                </button>
            </header>

        </div>
    );
}

export default App;
