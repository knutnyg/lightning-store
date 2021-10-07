import Carousel from "react-multi-carousel";
import {useEffect, useState} from "react";
import {GalleryImages, requestGalleryImages} from "../io/images";
import {baseUrl} from "../App";
import {PageProps} from "./Kunstig";
import {Link} from "react-router-dom";

export const Gallery = (pageProps: PageProps) => {
    const [loaded, setLoaded] = useState<Boolean>(false)
    const [images, setImages] = useState<GalleryImages[]>([])

    const DOMImages = images?.map((id, index) => {
        return <img key={index} className={"carousel-image"} alt={'t'}
                    src={`${baseUrl}/products/${id}/data`}/>
    })

    pageProps.onChange("Galleriet")

    useEffect(() => {
        if (!loaded) {
            requestGalleryImages()
                .then(images => {
                    setImages(images)
                    setLoaded(true)
                })
                .then(_ => {
                    console.log("updated images with:", images)
                })
                .catch(err => {
                    console.log("error", err)
                    setImages([])
                })
        }
    })

    return (<div className="page">
        <div className={"flex-container grow"}>
            <p>Voila og velkommen! Her henger all kunsten Kunstig har malt til nå.</p>
            <div className={"centered"}>
                <Carousel
                    swipeable={true}
                    draggable={false}
                    showDots={true}
                    responsive={{
                        superLargeDesktop: {
                            // the naming can be any, depends on you.
                            breakpoint: {max: 4000, min: 3000},
                            items: 1
                        },
                        desktop: {
                            breakpoint: {max: 3000, min: 1024},
                            items: 1
                        },
                        tablet: {
                            breakpoint: {max: 1024, min: 464},
                            items: 1
                        },
                        mobile: {
                            breakpoint: {max: 464, min: 0},
                            items: 1
                        }
                    }}
                    ssr={true} // means to render carousel on server-side.
                    infinite={true}
                    autoPlay={true}
                    autoPlaySpeed={30000}
                    keyBoardControl={true}
                    customTransition="all .5"
                    transitionDuration={1000}
                    containerClass="carousel-container"
                    removeArrowOnDeviceType={["tablet", "mobile"]}
                    dotListClass="custom-dot-list-style"
                    itemClass="carousel-item-padding-40-px"
                >
                    {DOMImages}
                </Carousel>
            </div>
        </div>
        <Link to="/workshop">Til verkstedet</Link>
    </div>)
}