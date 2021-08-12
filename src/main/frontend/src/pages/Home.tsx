import {Link} from "react-router-dom";
import {useEffect} from "react";
import {PageProps} from "./Blog";


export const Home = (props: PageProps) => {
    useEffect(() => {
        props.onChange("Lightning blog ⚡️")
    })

    return <div><p>This site is a project for me to learn my way around programming for the lightning
        network and all
        the possibilities it brings to web 3.0. I will attempt to explore concepts and
        techniques that
        micropayments bring to the table.</p>
        <p>To navigate this site you need to have lightning enabled bitcoin wallet and authenticate
            yourself through paying a lightning invoice. Read more and authenticate yourself in
            the <Link to="/s/LSAT">LSAT section</Link></p>
        <h2>Table of content:</h2>
        <h3>Concepts</h3>
        <ul>
            <li><Link to="/s/bitcoin-network">Bitcoin Network</Link></li>
            <li><Link to="/s/lightning-network">Lightning Network</Link></li>
            <li><Link to="/s/lsat">LSAT</Link></li>
        </ul>
        <h3>Techniques</h3>
        <ul>
            <li><Link to="/s/blog-paywall">Paywalling content</Link></li>
        </ul>
    </div>
}