import {useEffect, useState} from "react";
import {Link} from "react-router-dom";
import pic1 from "./resources/AI-picture-1.png"
import pic2 from "./resources/AI-picture-2.png"
import pic3 from "./resources/AI-picture-3.png"
import pic4 from "./resources/AI-picture-4.png"
import {AccessState} from "./Blog";
import {InvoiceView} from "../invoice/Invoice";
import 'react-multi-carousel/lib/styles.css';
import {baseUrl} from "../App";
import useInterval from "../hooks/useInterval";
import {updateTokenInvoice} from "./Register";
import {User} from "../hooks/useUser";

export interface PageProps {
    onChange: (title: string) => void;
    updateUser: () => void;
    user?: User
}

interface ImageBlob {
    id: string,
    payload: string | undefined
}

// 1. lookup register

export interface MyState {
    state: AccessState
    invoice?: InvoiceKunstig,
    register?: Register,
}

interface InvoiceKunstig {
    paymentRequest: string,
    settled: boolean,
    id: string,
    preimage?: string
}

interface Register {
    paymentRequest: string,
}

export const Kunstig = (props: PageProps) => {
    const [state, setState] = useState<MyState>({
        invoice: undefined,
        state: AccessState.INITIAL
    })

    let images

    const handleRegister = (): Promise<void> => {
        return fetch(`${baseUrl}/register`, {
                method: 'PUT',
                headers: {
                    'Access-Control-Allow-Origin': '*',
                },
            }
        ).then(response => {
            if (response.status === 402) {
                const wwwchallenge = response.headers.get('WWW-Authenticate')!
                const type = wwwchallenge.split(' ')[0]
                const macaroon = wwwchallenge.split(' ')[1].slice(0, -1).split('=')[1].slice(1, -1)
                const invoice = wwwchallenge.split(' ')[2].split('=')[1].slice(1, -1)

                setState({
                    ...state,
                    register: {
                        paymentRequest: invoice,
                    },
                    state: AccessState.PENDING_REGISTER
                })
                localStorage.setItem("macaroon", macaroon)
            }
        })
            .catch(err => {
                console.log(err)
                return Promise.reject()
            });
    }

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
        // createOrderInvoice("ec533145-47fa-464e-8cf0-fd36e3709ad3")
        //     .then(invoice => setState({...state, invoice: invoice, access: AccessState.PAYMENT_PENDING}))
    }
    //


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
        {state.state === AccessState.ACCESS && <div>
            <p>ACCESS 🎉</p>
            <img src={`${baseUrl}/products/a1afc48b-23bc-4297-872a-5e7884d6975a/data`}/>
        </div>
        }


        {/*<Carousel*/}
        {/*    swipeable={false}*/}
        {/*    draggable={false}*/}
        {/*    showDots={true}*/}
        {/*    responsive={responsive}*/}
        {/*    ssr={true} // means to render carousel on server-side.*/}
        {/*    infinite={true}*/}
        {/*    autoPlay={true}*/}
        {/*    autoPlaySpeed={7000}*/}
        {/*    keyBoardControl={true}*/}
        {/*    customTransition="all .5"*/}
        {/*    transitionDuration={500}*/}
        {/*    containerClass="carousel-container"*/}
        {/*    removeArrowOnDeviceType={["tablet", "mobile"]}*/}
        {/*    dotListClass="custom-dot-list-style"*/}
        {/*    itemClass="carousel-item-padding-40-px"*/}
        {/*>*/}

        {/*</Carousel>*/}

        {/*<p>Next steps:</p>*/}
        {/*<ul>*/}
        {/*    <li>Buy the rest of the image set</li>*/}
        {/*    <li>Order freshly generated art</li>*/}
        {/*</ul>*/}

        <Link to="/">Back</Link>
    </div>
}