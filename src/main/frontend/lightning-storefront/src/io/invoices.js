export const updateInvoice = (invoice, setInvoice) => {
    fetch(`http://localhost:8000/invoices/${invoice.id}`, {
        method: 'GET',
        headers: {
            'Access-Control-Allow-Origin': '*',
            'Accept': 'application/json'
        },
    })
        .then(response => response.json())
        .then(data => processInvoice(data, setInvoice))
        .catch(err => console.log(err));
}

export const createInvoice = (setInvoice) => {
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
        .then(invoice => processInvoice(invoice, setInvoice))
        .catch(err => console.log(err));
}

const processInvoice = (invoice, setInvoice) => {
    console.log('processing invoice')
    if (invoice.settled) {
        console.log('invoice settled!')
    }
    setInvoice({...invoice, inFlow: !invoice.settled})
}