import {useEffect, useState} from "react";
import 'react-multi-carousel/lib/styles.css';
import useInterval from "../hooks/useInterval";
import {User} from "../hooks/useUser";
import {TicketBooth} from "./TicketBooth";
import {Gallery} from "./Gallery";

export interface PageProps {
    onChange: (title: string) => void;
}

export interface PageWithUserProps extends PageProps {
    updateUser: () => void;
    user?: User
}

enum State {
    INITIAL, TICKET_REQUIRED, GALLERY
}

interface PageState {
    state: State
}

const initialState = {
    state: State.INITIAL
}

export const Kunstig = (props: PageWithUserProps) => {
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