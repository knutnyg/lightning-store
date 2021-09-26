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
import {fetchProduct} from "../product/products";

export interface PageProps {
    onChange: (title: string) => void;
    updateUser: () => void;
    user?: User
}

interface CustomImage {
    id?: string
    paymentRequest?: string,
    image?: Image,
    pending: boolean,
}

interface PageState {
    state: AccessState
    invoice?: InvoiceKunstig,
    register?: Register,
    customImage?: CustomImage
}

interface InvoiceKunstig {
    paymentRequest: string,
    settled: boolean,
    id: string,
    preimage?: string
}

export const Kunstig = (props: PageProps) => {
    const [state, setState] = useState<PageState>({
        state: AccessState.INITIAL
    })

    useInterval(() => {
        if (state.state === AccessState.PENDING_REGISTER) {
            updateTokenInvoice()
                .then(invoice => {
                    setState({
                        ...state, invoice: {
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
        }
        if (state.customImage?.pending) {
            fetchProduct(state.customImage?.id!!)
                .then((product) => setState({
                    ...state,
                    customImage: {...state.customImage, pending: false, id: product?.payload}
                }))
        }
    }, 1000)
    useEffect(() => {
        if (!props.user) {
            props.updateUser()
        }
        if (props.user && state.state === AccessState.INITIAL) {
            setState({...state, state: AccessState.ACCESS})
        }
        props.onChange("Can a machine make art? 🎨")
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
        setState({...state, customImage: {...state.customImage, pending: true}})
    }

    return <div className="page">
        <p>
            These pieces are created by Kunstig, an AI that has taught itself to paint on its own. Kunstig is heavily
            inspired by Edward Munch but you can also see that he takes from the abstract world of art as well. Kunstig
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
        {state.state === AccessState.ACCESS && props.user && <div>
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
                <img className={"carousel-image"}
                     src={`${baseUrl}/products/a1afc48b-23bc-4297-872a-5e7884d6975a/data`}/>
            </Carousel>
            <div>
                <p>Kunstig can also draw paintings just for you!</p>
                <button onClick={buyImage}>Buy a custom box fresh image</button>
                {state.customImage?.pending && <InvoiceView paymentReq={state.customImage?.paymentRequest!!}/>}
                {state.customImage?.id &&
                <img src={`${baseUrl}/products/${state.customImage.id}/data`} alt={'my special image'}/>}
            </div>

        </div>
        }
    </div>
}