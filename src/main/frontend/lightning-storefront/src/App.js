import './App.css';

const getInvoice = () => {
    console.log("calling")
    fetch('http://localhost:8000/invoices/8f9ade7c-6006-4869-b785-95322f954057', {
        method: 'Get',
        mode: 'cors',
        headers: {
            'Access-Control-Allow-Origin': '*'
        }

    })
        .then(response => response.json())
        .then(data => state.invoice = data)
        .catch(err => console.log(err));
}

let state = {
    invoice: undefined
}

function App() {
    return (
        <div className="Lightning Store">
            <header className="App-header">
                {state.invoice && <p>{state.invoice.paymentRequest}</p>}
                <p>
                    Welcome to the my store
                </p>
                <button onClick={getInvoice}>Donate</button>
            </header>

        </div>
    );
}

export default App;
