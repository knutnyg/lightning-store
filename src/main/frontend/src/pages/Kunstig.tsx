import {useEffect, useState} from "react";
import {Link} from "react-router-dom";
import pic1 from "./resources/AI-picture-1.png"
import pic2 from "./resources/AI-picture-2.png"
import pic3 from "./resources/AI-picture-3.png"
import pic4 from "./resources/AI-picture-4.png"
import {AccessState, State} from "./Blog";
import {createOrderInvoice, fetchProduct} from "../product/products";
import useInterval from "../hooks/useInterval";
import {updateInvoice} from "../invoice/invoices";
import {InvoiceView} from "../invoice/Invoice";
import Carousel from 'react-multi-carousel';
import 'react-multi-carousel/lib/styles.css';
import {baseUrl} from "../App";

export interface PageProps {
    onChange: (title: string) => void;
}

interface ImageBlob {
    id: string,
    payload: string | undefined
}

export const Kunstig = (props: PageProps) => {
    const [state, setState] = useState<State>({
        invoice: undefined,
        product: undefined,
        access: AccessState.INITIAL
    })

    let images

    useEffect(() => {
        props.onChange("Can a machine make art? 🎨")
        if (state.access === AccessState.INITIAL) {
            fetchProduct("ec533145-47fa-464e-8cf0-fd36e3709ad3")
                .then(product => setState({...state, product: product, access: AccessState.ACCESS}))
                .catch(res => setState({...state, access: AccessState.PAYMENT_REQUIRED}))
        }

        if (state.access === AccessState.ACCESS) {
            images = (<div>
                <img src={`baseUrl/products/a1afc48b-23bc-4297-872a-5e7884d6975a`}/>
            </div>)
        }
    })

    const buyAccess = () => {
        createOrderInvoice("ec533145-47fa-464e-8cf0-fd36e3709ad3")
            .then(invoice => setState({...state, invoice: invoice, access: AccessState.PAYMENT_PENDING}))
    }

    useInterval(() => {
        if (state.access === AccessState.PAYMENT_PENDING) {
            if (state.invoice && !state.invoice?.settled) {
                updateInvoice(state.invoice)
                    .then(invoice => {
                            setState({...state, invoice: invoice})
                        }
                    )
            }
            if (state.invoice?.settled) {
                fetchProduct("ec533145-47fa-464e-8cf0-fd36e3709ad3")
                    .then(product => {
                        setState({...state, product: product, access: AccessState.ACCESS, invoice: undefined})
                    })
                    .catch(_ => setState({...state, access: AccessState.PAYMENT_REQUIRED, invoice: undefined}))
            }
        }
    }, 1000)

    const responsive = {
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
    };

    return <div className="page">
        <p>
            These pieces are created by Kunstig, an AI that has taught itself to paint on its own. Kunstig is heavily
            inspired by Edward Munch but you can also see that he takes from the abstract world of art as well. Kunstig
            can produce an infinite amount of unique artworks, meaning there will never exist two identical pieces.
        </p>
        <h2>Heres a few samples:</h2>
        <div>
            <Carousel
                swipeable={false}
                draggable={false}
                showDots={true}
                responsive={responsive}
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
                <img className={"ai-image"} src={pic1}/>
                <img className={"ai-image"} src={pic2}/>
                <img className={"ai-image"} src={pic3}/>
                <img className={"ai-image"} src={pic4}/>
            </Carousel>
        </div>

        <div>
            <button onClick={buyAccess}>Purchase a ticket</button>
            {state.access === AccessState.PAYMENT_PENDING && state.invoice &&
            <InvoiceView paymentReq={state.invoice.paymentRequest}/>}
        </div>

        <img src={`${baseUrl}/products/a1afc48b-23bc-4297-872a-5e7884d6975a/data`}/>


        <p>Next steps:</p>
        <ul>
            <li>Buy the rest of the image set</li>
            <li>Order freshly generated art</li>
        </ul>

        <Link to="/">Back</Link>
    </div>
}