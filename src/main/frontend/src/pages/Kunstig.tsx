import {useEffect, useState} from "react";
import {Link} from "react-router-dom";
import pic1 from "./resources/AI-picture-1.png"
import pic2 from "./resources/AI-picture-2.png"
import pic3 from "./resources/AI-picture-3.png"
import pic4 from "./resources/AI-picture-4.png"
import {AccessState, State} from "./Blog";
import {fetchProduct} from "../product/products";
import useInterval from "../hooks/useInterval";
import {updateInvoice} from "../invoice/invoices";

export interface PageProps {
    onChange: (title: string) => void;
}

export const Kunstig = (props: PageProps) => {
    const [state, setState] = useState<State>({
        invoice: undefined,
        product: undefined,
        access: AccessState.INITIAL
    })


    useEffect(() => {
        props.onChange("Can a machine make art? 🎨")
        if (state.access === AccessState.INITIAL) {
            fetchProduct("261dd820-cfc4-4c3e-a2c8-59d41eb44dfc")
                .then(product => setState({...state, product: product, access: AccessState.ACCESS}))
                .catch(res => setState({...state, access: AccessState.PAYMENT_REQUIRED}))
        }
    })

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
                fetchProduct("awd")
                    .then(product => {
                        setState({...state, product: product, access: AccessState.ACCESS, invoice: undefined})
                    })
                    .catch(_ => setState({...state, access: AccessState.PAYMENT_REQUIRED, invoice: undefined}))
            }
        }
    }, 1000)

    return <div className="page">
        <p>
            These pieces are created by Kunstig, an AI that has taught itself to paint on its own. Kunstig is heavily
            inspired by Edward Munch but you can also see that he takes from the abstract world of art as well. Kunstig
            can produce an infinite amount of unique artworks, meaning there will never exist two identical pieces.
        </p>
        <h2>Heres a few samples:</h2>
        <div className="gallery">
            <img className={"ai-image"} src={pic1}/>
            <img className={"ai-image"} src={pic2}/>
            <img className={"ai-image"} src={pic3}/>
            <img className={"ai-image"} src={pic4}/>
        </div>

        <div>
            <button>Unlock more</button>
        </div>

        <p>Next steps:</p>
        <ul>
            <li>Buy the rest of the image set</li>
            <li>Order freshly generated art</li>
        </ul>

        <Link to="/">Back</Link>
    </div>
}