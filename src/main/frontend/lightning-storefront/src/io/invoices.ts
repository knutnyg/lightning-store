interface InvoiceRaw {
    id: string
    paymentRequest: string,
    settled: string,
    memo: string
}

export interface Invoice extends InvoiceRaw {
    inProgress: boolean,
}

export const updateInvoice = (invoice: Invoice): Promise<Invoice> => {
    return fetch(`http://localhost:8000/invoices/${invoice.id}`, {
        method: 'GET',
        headers: {
            'Access-Control-Allow-Origin': '*',
            'Accept': 'application/json'
        },
    })
        .then(response => (response.json() as Promise<InvoiceRaw>))
        .then((raw) => {
            return {
                ...raw,
                inProgress: !raw.settled
            }
        })
        .catch(err => {
            console.log(err)
            return Promise.reject()
        });
}

export const createInvoice = (): Promise<Invoice> => {
    return fetch('http://localhost:8000/invoices', {
        method: 'POST',
        headers: {
            'Access-Control-Allow-Origin': '*',
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        },
        body: JSON.stringify({memo: "Chancellor on brink of second bailout for banks"})
    })
        .then(response => (response.json() as Promise<InvoiceRaw>))
        .then(raw =>
            ({...raw, inProgress: true})
        )
        .catch(err => {
            console.log(err)
            return Promise.reject()
        });
}