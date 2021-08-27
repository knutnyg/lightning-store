import {useEffect} from "react";
import {Link} from "react-router-dom";
import pic1 from "./resources/AI-picture-1.png"
import pic2 from "./resources/AI-picture-2.png"
import pic3 from "./resources/AI-picture-3.png"
import pic4 from "./resources/AI-picture-4.png"

export interface PageProps {
    onChange: (title: string) => void;
}

export const MachinePaints = (props: PageProps) => {

    useEffect(() => {
        props.onChange("Can a machine make art? 🎨")
    })

    return <div className="page">
        <p>My colleage has a project where she has a neural network drawing images based on input.</p>
        <p>She can use my site as a gallery</p>
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