import {useEffect} from "react";
import {PageProps} from "./Blog";

export const Lightning = (props:PageProps) => {

    useEffect(() => {
        props.onChange("Lightning")
    })
    return <div>
        <p>The lightning network is a global
            decentralized payment network leveraging the bitcoin block chain.
            Trading some of the features (like offline wallets) for benefits like massively scalable
            and near free instant transactions. This could pave the way for a brave new web 3.0
            where micropayments replace advertisements and sale of your personal data as the main
            way to fund a service or a web site.</p></div>
}