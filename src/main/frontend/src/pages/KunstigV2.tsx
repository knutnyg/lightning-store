import {useEffect, useState} from "react";
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
import {Link} from "react-router-dom";
import {TicketBooth} from "./TicketBooth";
import {Workshop} from "./Workshop";
import {Gallery} from "./Gallery";

export interface PageProps {
    onChange: (title: string) => void;
    updateUser: () => void;
    user?: User
}

interface CustomImage {
    id?: string
    image?: Image
}

enum State {
    INITIAL, TICKET_REQUIRED, GALLERY, WORKSHOP
}

interface PageState {
    state: State
}

interface InvoiceKunstig {
    paymentRequest: string,
    settled: boolean,
    id: string,
}

const initialState = {
    state: State.TICKET_REQUIRED
}

const galleryState = {
    state: State.GALLERY
}

const workshopState = {
    state: State.WORKSHOP
}

export const KunstigV2 = (props: PageProps) => {
    const [state, setState] = useState<PageState>(workshopState)

    useInterval(() => {

    }, 4000)
    useEffect(() => {
        if (props.user) {
            setState({state: State.WORKSHOP})
        }
    })


    return <div className="page grow">
        <div className="grow">
            {state.state === State.TICKET_REQUIRED && <TicketBooth onChange={props.onChange} updateUser={props.updateUser}/>}
            {state.state === State.GALLERY && <Gallery/>}
            {state.state === State.WORKSHOP && <Workshop onChange={props.onChange}/>}
        </div>
        <Link to="/kunstig/about">Om galleriet</Link>
    </div>
}