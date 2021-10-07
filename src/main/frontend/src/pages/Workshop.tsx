import {Image} from "./Admin";
import {requestFreshlyPaintedPicture} from "../io/images";
import {useEffect, useState} from "react";
import {baseUrl} from "../App";
import useInterval from "../hooks/useInterval";
import {User} from "../hooks/useUser";
import {Invoice, updateInvoice} from "../invoice/invoices";
import {InvoiceView} from "../invoice/Invoice";
import Loader from "react-loader-spinner";
import {Link} from "react-router-dom";

enum State {
    INITIAL, IN_PAYMENT, PAYMENT_COMPLETE, FETCHING_IMAGE, IMAGE_READY
}

interface CustomImage {
    id?: string
    image?: Image
}

interface PageState {
    state: State
    imageInvoice?: Invoice
    customImage?: CustomImage,
}

const initialState = {
    state: State.INITIAL,
    imageInvoice: undefined,
    customImage: undefined,
}


const buyingState = {
    state: State.IN_PAYMENT,
    imageInvoice: {
        id: 'id',
        paymentRequest: 'req',
        settled: undefined,
        memo: 'memo',
        inProgress: true,
    },
    customImage: {
        id: 'memo'
    },
}

const pendingState = {
    state: State.IN_PAYMENT,
    imageInvoice: {
        id: 'id',
        paymentRequest: 'req',
        settled: '123',
        memo: 'memo',
        inProgress: false,
    },
    customImage: {
        id: 'memo'
    }
}

const imageState = {
    state: State.IMAGE_READY,
    imageInvoice: undefined,
    customImage: {
        id: 'id',
        image: {
            objUrl: 'url',
            payload: new Blob()
        }
    },
    imageFetchInFlight: false
}

interface Props {
    onChange: (title: string) => void;
    user?: User
}

export const Workshop = (props: Props) => {
    const [state, setState] = useState<PageState>(initialState)
    const buyImage = () => {
        requestFreshlyPaintedPicture()
            .then(invoice => {
                console.log("User got invoice:", invoice.id)
                setState({
                    ...state,
                    imageInvoice: invoice,
                    customImage: {
                        id: invoice.memo
                    },
                })
                return invoice
            })
            .catch(err => {
                console.log(err)
            });
    }
    const getImage = (id: string) => {
        console.log('Fetching image:', id)
        return fetch(`${baseUrl}/products/${id}/data`, {
            method: 'GET',
            headers: {
                'Access-Control-Allow-Origin': '*',
                'Accept': 'application/json',
                'Authorization': `LSAT ${localStorage.getItem('macaroon')}:${localStorage.getItem("preimage")}`
            },
        })
            .then(res => res.blob())
    }

    useEffect(() => {
        props.onChange("Verkstedet")
    })
    useInterval(() => {
        if (state.state === State.IN_PAYMENT) {
            updateInvoice(state.imageInvoice?.id!!)
                .then(invoice => {
                        if (invoice.settled) {
                            setState({
                                ...state,
                                state: State.PAYMENT_COMPLETE,
                            })
                        }
                        return invoice
                    }
                )
                .then(invoice => {
                    if (state.state === State.PAYMENT_COMPLETE) {
                        setState({
                            state: State.FETCHING_IMAGE,
                            imageInvoice: undefined
                        })
                        getImage(invoice.memo)
                            .then(blob => {
                                setState({
                                    state: State.IMAGE_READY,
                                    customImage: {
                                        id: invoice.memo,
                                        image: {
                                            payload: blob,
                                            objUrl: URL.createObjectURL(blob)
                                        },
                                    }
                                })
                            })
                            .catch(err => console.log(err))
                    }
                })
                .catch(err => console.log(err))
        }
    }, 2000)

    return (<div className={"flex-container"}>
        {state.state !== State.FETCHING_IMAGE &&
        <p>Her kan du generere og donere nye kunstverk til Galleriet. De males av Kunstig på direkten mot en liten
            gave. </p>}
        {state.state === State.INITIAL && <button onClick={buyImage}>Kjøp et nytt bilde</button>}
        {state.state === State.IN_PAYMENT && <InvoiceView paymentReq={state.imageInvoice?.paymentRequest!!}/>}
        {state.state === State.FETCHING_IMAGE && <div className={"flex-container"}>
            <p>Vennligst vent et øyeblikk mens Kunstig maler et bilde til deg.</p>
            <div className="centered">
                <Loader
                    type="BallTriangle"
                    color="#00BFFF"
                    height={100}
                    width={100}
                />
            </div>

        </div>}
        {state.state === State.IMAGE_READY && <div>
            <p>Her er bildet ditt:</p>
            <img src={state.customImage?.image?.objUrl} alt={'your special image'}/>
        </div>}
        <Link to="/kunstig">Tilbake</Link>
    </div>)
}

