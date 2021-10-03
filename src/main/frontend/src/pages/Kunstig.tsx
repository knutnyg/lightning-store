import {useEffect, useState} from "react";
import pic1 from "./resources/AI-picture-1.png"
import pic2 from "./resources/AI-picture-2.png"
import pic3 from "./resources/AI-picture-3.png"
import pic4 from "./resources/AI-picture-4.png"
import {AccessState} from "./Blog";
import {InvoiceView} from "../invoice/Invoice";
import 'react-multi-carousel/lib/styles.css';
import {baseUrl} from "../App";
import useInterval from "../hooks/useInterval";
import {handleRegister, Register, updateTokenInvoice} from "./Register";
import {User} from "../hooks/useUser";
import Carousel from "react-multi-carousel";
import {Image} from "./Admin";
import {updateInvoice} from "../invoice/invoices";
import {GalleryImages, requestFreshlyPaintedPicture, requestGalleryImages} from "../io/images";

export interface PageProps {
    onChange: (title: string) => void;
    updateUser: () => void;
    user?: User
}

interface CustomImage {
    id?: string
    image?: Image
}

interface PageState {
    state: AccessState
    tokenInvoice?: InvoiceKunstig,
    imageInvoice?: InvoiceKunstig
    register?: Register,
    customImage?: CustomImage,
    imageFetchInFlight: boolean,
    images?: GalleryImages []
}

interface InvoiceKunstig {
    paymentRequest: string,
    settled: boolean,
    id: string,
}

export const Kunstig = (props: PageProps) => {
    const [state, setState] = useState<PageState>({
        state: AccessState.INITIAL,
        imageFetchInFlight: false,
        images: undefined
    })

    useInterval(() => {
        console.log(state)
            if (state.state === AccessState.PENDING_REGISTER) {
                updateTokenInvoice()
                    .then(invoice => {
                        setState({
                            ...state, tokenInvoice: {
                                paymentRequest: invoice.paymentRequest,
                                settled: invoice.settled,
                                id: invoice.id!!
                            },
                            state: invoice.settled ? AccessState.ACCESS : AccessState.PENDING_REGISTER
                        })
                        if (invoice.settled) {
                            localStorage.setItem("preimage", invoice.preimage!!)
                            props.updateUser()
                        }
                    })
            } else if (state.imageInvoice) {
                updateInvoice(state.imageInvoice?.id!!)
                    .then(invoice => {
                            if (!!invoice.settled) {
                                setState({
                                    ...state,
                                    imageInvoice: {
                                        ...state.imageInvoice!!,
                                        settled: true,
                                    },
                                })
                            }
                            return invoice
                        }
                    )
                    .then(res => {
                        if (res.settled) {
                            setState({
                                ...state,
                                imageFetchInFlight: true,
                                imageInvoice: undefined
                            })
                            getImage(res.memo)
                        }
                    })
            }
        }, 4000
    )
    useEffect(() => {
        if (!props.user) {
            props.updateUser()
        }
        if (props.user && state.state === AccessState.INITIAL) {
            setState({...state, state: AccessState.ACCESS})
        }
        props.onChange("Can a machine make art? 🎨")
        if (state.images === undefined) {
            requestGalleryImages()
                .then(images => setState({...state, images: images}))
                .then(_ => {
                    console.log("updated images with:", state.images)
                })
                .catch(_ => {
                    setState({...state, images: []})
                })
        }
    })

    const buyAccess = () => {
        handleRegister()
            .then((register) => {
                setState({
                    ...state,
                    register: register,
                    state: AccessState.PENDING_REGISTER
                })
                localStorage.setItem("macaroon", register.macaroon)
            })
            .catch((error) => {
                console.log(error)
            })
    }

    const buyImage = () => {
        requestFreshlyPaintedPicture()
            .then(invoice => {
                console.log("User got invoice:", invoice.id)
                setState({
                    ...state,
                    imageInvoice: {
                        id: invoice.id,
                        paymentRequest: invoice.paymentRequest,
                        settled: !!invoice.settled
                    },
                    customImage: {
                        id: invoice.memo
                    },
                    imageFetchInFlight: false
                })
                return invoice
            })
            .catch(err => {
                console.log(err)
                return Promise.reject()
            });
    }

    const getImage = (id: string) => {
        console.log('Fetching image:', id)
        fetch(`${baseUrl}/products/${id}/data`, {
            method: 'GET',
            headers: {
                'Access-Control-Allow-Origin': '*',
                'Accept': 'application/json',
                'Authorization': `LSAT ${localStorage.getItem('macaroon')}:${localStorage.getItem("preimage")}`
            },
        })
            .then(res => res.blob())
            .then(blob => {
                setState({
                    ...state,
                    customImage: {
                        id: id,
                        image: {
                            payload: blob,
                            objUrl: URL.createObjectURL(blob)
                        },
                    },
                    imageInvoice: undefined,
                    imageFetchInFlight: false
                })
            })
            .catch(err => console.log(err))
    }

    const images = state.images?.map((id, index) => {
        return <img key={index} className={"carousel-image"} alt={'t'}
                    src={`${baseUrl}/products/${id}/data`}/>
    }) ?? null

    return <div className="page">
        <p>
            These pieces are created by Kunstig, an AI that has taught itself to paint on its own. Kunstig is
            heavily
            inspired by Edward Munch but you can also see that he takes from the abstract world of art as well.
            Kunstig
            can produce an infinite amount of unique artworks, meaning there will never exist two identical pieces.
        </p>
        <h2>Heres a few samples:</h2>
        <div className={"gallery-sample"}>
            <img className={"ai-image"} src={pic1}/>
            <img className={"ai-image"} src={pic2}/>
            <img className={"ai-image"} src={pic3}/>
            <img className={"ai-image"} src={pic4}/>
        </div>
        {state.state !== AccessState.ACCESS &&
        <div>
            {state.state === AccessState.INITIAL && <div>
                <p>To enter the gallery you must purchase a ticket</p>
                <button onClick={buyAccess}>Purchase a ticket</button>
            </div>}
            {state.state === AccessState.PENDING_REGISTER && state.register &&
            <InvoiceView paymentReq={state.register.paymentRequest}/>}
        </div>}
        {state.state === AccessState.ACCESS && props.user && state.images && state.images.length > 0 && <div>
            <Carousel
                swipeable={false}
                draggable={false}
                showDots={true}
                responsive={{
                    superLargeDesktop: {
                        // the naming can be any, depends on you.
                        breakpoint: {max: 4000, min: 3000},
                        items: 1
                    },
                    desktop: {
                        breakpoint: {max: 3000, min: 1024},
                        items: 1
                    },
                    tablet: {
                        breakpoint: {max: 1024, min: 464},
                        items: 1
                    },
                    mobile: {
                        breakpoint: {max: 464, min: 0},
                        items: 1
                    }
                }}
                ssr={true} // means to render carousel on server-side.
                infinite={true}
                autoPlay={true}
                autoPlaySpeed={7000}
                keyBoardControl={true}
                customTransition="all .5"
                transitionDuration={500}
                containerClass="carousel-container"
                removeArrowOnDeviceType={["tablet", "mobile"]}
                dotListClass="custom-dot-list-style"
                itemClass="carousel-item-padding-40-px"
            >
                {images}
            </Carousel>
            <div>
                <p>Kunstig can also draw paintings just for you!</p>
                <button onClick={buyImage}>Buy a custom box fresh image</button>
                {state.imageInvoice &&
                <InvoiceView paymentReq={state.imageInvoice.paymentRequest!!}/>}
                {state.customImage?.id && state.customImage?.image &&
                <img src={state.customImage?.image.objUrl} alt={'my special image'}/>}
            </div>

        </div>
        }
    </div>
}