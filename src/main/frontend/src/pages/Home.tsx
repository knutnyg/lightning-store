import {Link} from "react-router-dom";
import {useEffect} from "react";
import {PageProps} from "./Blog";


export const Home = (props: PageProps) => {
    useEffect(() => {
        props.onChange("⚡️ Galleriet ð")
    })

    return <div className={"page"}>
        <p>Welcome to my Lightning project! This is a ⚡️ powered site where I experiment with and all
            the possibilities programmable money brings to the web. My goal is to make an educational and inspiring site
            explaining and showcasing different concepts and techniques using the Lightning Network.</p>

        <p>To navigate this site you need to have a lightning enabled bitcoin wallet on your phone or computer. I
            recommend <a href="https://bluewallet.io">BlueWallet</a> to get started.</p>

        <p>Lets start by setting you up with an account. Not a normal boring account. We live in the future and use a
            new technique
            named <Link
                to="/lsat">LSAT</Link>.</p>
        <Link className={"centered"} to="/lsat">Read more and sign up here️</Link>

        <p>All my code is freely available at <a
            href={"https://github.com/knutnyg/lightning-store/"}>github</a> and my <a
            href={"https://1ml.com/node/020deb273bd81cd6771ec3397403f2e74a3c22f8f4c052321c30e5c612cf538328"}>lightning
            node</a> is public and would love new connections! 🤝🤠</p>


        {/*<h2>Table of content:</h2>*/}
        {/*<h3>Concepts</h3>*/}
        {/*<ul>*/}
        {/*    <li><Link to="/s/lsat">LSAT</Link></li>*/}
        {/*    <li><Link to="/s/bitcoin-network">Bitcoin Network</Link></li>*/}
        {/*    <li><Link to="/s/lightning-network">Lightning Network</Link></li>*/}
        {/*</ul>*/}
        {/*<h3>Techniques</h3>*/}
        {/*<ul>*/}
        {/*    <li><Link to="/s/blog-paywall">Paywalling content</Link></li>*/}
        {/*</ul>*/}
    </div>
}