import reportWebVitals from "./reportWebVitals";

export const RegisterView = () => {
    const test = () => {
    }
    return <div>
        <ul>
            <li>1. push button</li>
            <li>2. pay invoice</li>
            <li>3. receive api-key (LSAT)</li>
        </ul>
        <button onClick={register}>Register</button>
        <button onClick={test}>Test token</button>
    </div>
}

export const register = (): Promise<any> => {
    return fetch('http://localhost:8000/register', {
            method: 'PUT',
            headers: {
                'Access-Control-Allow-Origin': '*'
            }
        }
    )
        // Parse header
        .then(response => {
            if (response.status == 402) {
                console.log("payment is required")
                console.log(response.headers)
                console.log()

                const wwwchallenge = response.headers.get('WWW-Authenticate')!
                const type = wwwchallenge.split(' ')[0]
                const macaroon = wwwchallenge.split(' ')[1].slice(0, -1).split('=')[1].slice(1, -1)
                const invoice = wwwchallenge.split(' ')[2].split('=')[1].slice(1, -1)

                console.log(wwwchallenge)
                console.log('type', type)
                console.log('macaroon', macaroon)
                console.log('invoice', invoice)
            }
            return response
        })

        .catch(err => {
            console.log(err)
            return Promise.reject()
        });
}