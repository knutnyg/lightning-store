import {GalleryImages} from "../io/images";
import {baseUrl} from "../App";


interface Props {
    images: GalleryImages[]
}

export const MiniGallery = (props:Props) => {

    const DOMImages = props.images
        .map((id, index) => {
            return <img key={index} alt={'t'}
                        src={`${baseUrl}/products/${id}/data`}/>
        })

    // For local development:
    // const DOMImages = [0, 1, 2, 3, 4, 5, 6, 7].map((index) =>
    //     <img key={index} className={"carousel-image centered"} alt={'t'}
    //          src={`https://hips.hearstapps.com/hmg-prod.s3.amazonaws.com/images/dog-puppy-on-garden-royalty-free-image-1586966191.jpg`}/>
    // )

    return (
        <div className="minigallery">
            <p className="block">Her er Kunstigs siste verk</p>
            {DOMImages}
        </div>
    )
}