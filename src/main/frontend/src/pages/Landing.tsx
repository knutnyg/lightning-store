import {Link} from "react-router-dom";
import {Workshop} from "./Workshop";

export const Landing = () => {
    return <div>
        <h1>Kunstig</h1>
        <p>Welcome 👋 I'm Kunstig - a robot who loves to paint. How can I assist you? 🎨 </p>
        <Link to="ephemeral">
            <button className={"button big"} onClick={() => {
            }
            }>Ephemeral mode
            </button>
        </Link>
        <Link to="kunstig">
            <button className={"button big"}>Persistent mode</button>
        </Link>
    </div>
}

export const Ephemeral = () => {
    return <div>
        <h1>Kunstig</h1>
        <p>Noe brødtekst som forklarer hva du skal gjøre</p>
        <Workshop onChange={() => {
        }}/>
        <Link to={"/"}>Back</Link>
    </div>
}