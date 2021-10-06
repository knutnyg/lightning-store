export const Gallery = () => <div></div>

// import {requestFreshlyPaintedPicture} from "../io/images";
// import {baseUrl} from "../App";
// import {AccessState} from "./Blog";
// import {useState} from "react";
//
// const Workshop = () => {
//     const [state, setState] = useState<AccessState>(AccessState.INITIAL)
//
//     const buyImage = () => {
//         requestFreshlyPaintedPicture()
//             .then(invoice => {
//                 console.log("User got invoice:", invoice.id)
//                 setState({
//                     ...state,
//                     imageInvoice: {
//                         id: invoice.id,
//                         paymentRequest: invoice.paymentRequest,
//                         settled: !!invoice.settled
//                     },
//                     customImage: {
//                         id: invoice.memo
//                     },
//                     imageFetchInFlight: false
//                 })
//                 return invoice
//             })
//             .catch(err => {
//                 console.log(err)
//                 return Promise.reject()
//             });
//     }
//     const getImage = (id: string) => {
//         console.log('Fetching image:', id)
//         fetch(`${baseUrl}/products/${id}/data`, {
//             method: 'GET',
//             headers: {
//                 'Access-Control-Allow-Origin': '*',
//                 'Accept': 'application/json',
//                 'Authorization': `LSAT ${localStorage.getItem('macaroon')}:${localStorage.getItem("preimage")}`
//             },
//         })
//             .then(res => res.blob())
//             .then(blob => {
//                 setState({
//                     ...state,
//                     customImage: {
//                         id: id,
//                         image: {
//                             payload: blob,
//                             objUrl: URL.createObjectURL(blob)
//                         },
//                     },
//                     imageInvoice: undefined,
//                     imageFetchInFlight: false
//                 })
//             })
//             .catch(err => console.log(err))
//     }
// }
//
//
