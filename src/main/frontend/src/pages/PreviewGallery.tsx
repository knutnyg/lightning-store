import {useEffect, useState} from "react";
import {GalleryImages, requestGalleryImages} from "../io/images";
import {baseUrl} from "../App";

export const PreviewGallery = () => {
    const [images, setImages] = useState<GalleryImages[]>([])

    const DOMImages = images
        .map((id, index) => {
            return <img key={index} alt={'t'}
                        src={`${baseUrl}/products/${id}/data`}/>
        })

    // For local development:
    // const DOMImages = [0, 1, 2, 3, 4, 5, 6, 7].map((index) =>
    //     <img key={index} className={"carousel-image centered"} alt={'t'}
    //          src={`https://hips.hearstapps.com/hmg-prod.s3.amazonaws.com/images/dog-puppy-on-garden-royalty-free-image-1586966191.jpg`}/>
    // )

    const refreshImages = () => {
        requestGalleryImages('/minigallery')
            .then(images => {
                setImages(images)
            })
            .then(_ => {
                console.log("updated images with:", images)
            })
            .catch(err => {
                console.log("error", err)
                setImages([])
            })
    }

    useEffect(() => {
        refreshImages()
    }, [])

    return (
        <div className="minigallery">
            {DOMImages}
        </div>
    )
}