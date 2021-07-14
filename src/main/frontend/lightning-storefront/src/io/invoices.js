export const updateInvoice = (id, handler) => {
    fetch(`http://localhost:8000/invoices/${id}`, {
        method: 'GET',
        headers: {
            'Access-Control-Allow-Origin': '*',
            'Accept': 'application/json'
        },
    })
        .then(response => response.json())
        .then(data => handler(data))
        .catch(err => console.log(err));
}

export const createInvoice = (handler) => {
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
        .then(data => handler(data))
        .catch(err => console.log(err));
}