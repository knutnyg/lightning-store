import {useEffect} from "react";
import {Link} from "react-router-dom";
import pic1 from "./resources/AI-picture-1.png"
import pic2 from "./resources/AI-picture-2.png"
import pic3 from "./resources/AI-picture-3.png"
import pic4 from "./resources/AI-picture-4.png"

export interface PageProps {
    onChange: (title: string) => void;
}

export const Kunstig = (props: PageProps) => {

    useEffect(() => {
        props.onChange("Can a machine make art? 🎨")
    })

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

        <p>Next steps:</p>
        <ul>
            <li>Buy the rest of the image set</li>
            <li>Order freshly generated art</li>
        </ul>

        <Link to="/">Back</Link>
    </div>
}