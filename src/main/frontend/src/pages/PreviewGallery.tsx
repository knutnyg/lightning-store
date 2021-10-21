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

    return (<div className="flex-container">
        {DOMImages}
    </div>)
}