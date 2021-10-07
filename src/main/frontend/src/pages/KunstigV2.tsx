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
}

export interface PageWithUserProps extends PageProps {
    updateUser: () => void;
    user?: User
}

interface CustomImage {
    id?: string
    image?: Image
}

enum State {
    INITIAL, TICKET_REQUIRED, GALLERY
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
    state: State.INITIAL
}

const galleryState = {
    state: State.GALLERY
}

export const KunstigV2 = (props: PageWithUserProps) => {
    const [state, setState] = useState<PageState>(initialState)

    useEffect(() => {
        if (props.user && state.state !== State.GALLERY) {
            setState({state: State.GALLERY})
        } else {
            if (state.state === State.INITIAL) {
                setState({state: State.TICKET_REQUIRED})
            }
        }
    })

    useInterval(() => {
        props.updateUser()
    }, 10000)


    return <div className="page grow">
        {state.state === State.TICKET_REQUIRED &&
        <TicketBooth onChange={props.onChange} updateUser={props.updateUser}/>}
        {state.state === State.GALLERY && <Gallery onChange={props.onChange}/>}
    </div>
}