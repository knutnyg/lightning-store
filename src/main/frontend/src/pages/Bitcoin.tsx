import {useEffect} from "react";
import {PageProps} from "./Blog";

export const Bitcoin = (props: PageProps) => {
    useEffect(() => {
        props.onChange("Bitcoin ❤️")
    })

    return <h2>Bitcoin Network</h2>
}