import {Image} from "./Admin";
import {GalleryImages, requestFreshlyPaintedPicture, requestGalleryImages} from "../io/images";
import {useEffect, useState} from "react";
import {baseUrl} from "../App";
import useInterval from "../hooks/useInterval";
import {User} from "../hooks/useUser";
import {Invoice, updateInvoice} from "../invoice/invoices";
import {InvoiceView} from "../invoice/Invoice";
import Loader from "react-loader-spinner";
import {MiniGallery} from "./MiniGallery";
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
    const [images, setImages] = useState<GalleryImages[]>([])
    const buyImage = () => {
        requestFreshlyPaintedPicture()
            .then(invoice => {
                console.log("User got invoice:", invoice.id)
                setState({
                    ...state,
                    state: State.IN_PAYMENT,
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

    useEffect(() => {
        refreshMinigallery()
    }, [])

    const refreshMinigallery = () => {
        requestGalleryImages('/minigallery')
            .then(images => {
                setImages(images)
            })
            .then(_ => {
                console.log("updated images with:", images)
            })
            .catch(err => {
                console.log("error", err)
                setImages([])
            })
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

    const reset = () => {
        setState(initialState)
        window.scrollTo(0, 0);
        refreshMinigallery()
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
                    console.log("after invoice paid", state)
                    if (invoice.settled) {
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

    const shopDescription = "Kjøp et kunstverk ved å scanne denne QR-koden med mobilen din, det koster 1 satoshi (mindre enn 0,005 NOK) 🤑"

    return (<div className="page">
        <div className={"flex-container grow"}>
            {(state.state === State.INITIAL || state.state === State.IN_PAYMENT) &&
            <p>
                Velkommen👋 Her kan du be Kunstig (vår AI-modell) male et bilde til galleriet vårt 👩‍🎨
            </p>
            }
            {state.state === State.INITIAL && <button className="button block-xxl" onClick={buyImage}>Kjøp</button>}
            {state.state === State.IN_PAYMENT && <InvoiceView paymentReq={state.imageInvoice?.paymentRequest!!} description={shopDescription}/>}
            {state.state === State.FETCHING_IMAGE && <div className={"flex-container"}>
                <p>Kunstig jobber iherdig med å male et bilde til deg.</p>
                <div className="centered">
                    <Loader
                        type="BallTriangle"
                        color="#00BFFF"
                        height={100}
                        width={100}
                    />
                </div>

            </div>}
            {state.state === State.IMAGE_READY && <div className="flex-container">
                <div className="block">
                    <p className="pagetext">Kunstig har malt dette til deg ❤️</p>
                    <p className="pagetext">Kunstverket er lagt til i galleriet, tusen takk for bidraget 🙌</p>
                </div>
                <img className={"centered block"} src={state.customImage?.image?.objUrl} alt={'your special image'}/>
                <button onClick={reset} className="button">Prøv igjen!</button>
            </div>}
            <MiniGallery images={images}/>
            <Link to="/about">Om galleriet</Link>
        </div>
    </div>)
}

