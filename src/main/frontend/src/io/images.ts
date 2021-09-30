import {baseUrl} from "../App";
import {Invoice} from "../invoice/invoices";

export const requestFreshlyPaintedPicture = () => {
    console.log("User requested to buy a new image")
    return fetch(`${baseUrl}/products/image`, {
        method: 'POST',
        headers: {
            'Access-Control-Allow-Origin': '*',
            'Accept': 'application/json',
            'Authorization': `LSAT ${localStorage.getItem('macaroon')}:${localStorage.getItem("preimage")}`
        },
    })
        .then(response => (response.json() as Promise<Invoice>))
}