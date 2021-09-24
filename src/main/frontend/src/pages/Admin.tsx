import {FormEvent, useEffect, useState} from "react";
import {baseUrl} from "../App";

export interface PageProps {
    onChange: (title: string) => void;
}

interface Image {
    objUrl: string,
    payload: File
}

export const Admin = (props: PageProps) => {

    const [image, setImage] = useState<undefined | Image>(undefined)
    const [type, setType] = useState<undefined | string>("image")
    const [id, setId] = useState<undefined | string>(undefined)

    useEffect(() => {
        props.onChange("Admin 🔐")
    })

    const handleSubmit = (event: FormEvent) => {
        event.preventDefault()
        console.log("sending")
        console.log("id", id)
        return fetch(`${baseUrl}/admin/product/${id}/upload`, {
            method: 'POST',
            headers: {
                'Access-Control-Allow-Origin': '*',
                'Accept': 'application/json',
                'Authorization': `LSAT ${localStorage.getItem('macaroon')}:${localStorage.getItem("preimage")}`,
                'uploaded-media-type': `${type}`
            },
            body: image?.payload
        }).catch(err => {
            console.log(err)
            throw err
        })
    }

    return (
        <div>
            {image && <img src={image.objUrl}/>}
            <form onSubmit={handleSubmit}>
                <input type="radio" name="type" value="image" onChange={(ev) => setType("image")}/>Image
                <input type="radio" name="type" value="other" onChange={(ev) => setType("image")}/>Other
                <input type="text" name="id" onChange={(ev) => setId(ev.target.value)}/>
                <div>
                    <h1>Select Image</h1>
                    <input type="file" name="media"
                           onChange={(ev) => setImage({
                               objUrl: URL.createObjectURL(ev.target.files?.[0]),
                               payload: ev.target.files?.[0]!
                           })
                           }/>
                </div>
                <input type="submit"/>
            </form>
        </div>
    )
}