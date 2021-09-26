import {FormEvent, useEffect, useState} from "react";
import {baseUrl} from "../App";

export interface PageProps {
    onChange: (title: string) => void;
}

export interface Image {
    objUrl: string,
    payload: File
}

export const Admin = (props: PageProps) => {

    const [image, setImage] = useState<undefined | Image>(undefined)
    const [id, setId] = useState<undefined | string>(undefined)

    useEffect(() => {
        props.onChange("Admin 🔐")
    })

    const handleSubmit = (event: FormEvent) => {
        event.preventDefault()
        return fetch(`${baseUrl}/admin/product/${id}/upload`, {
            method: 'POST',
            headers: {
                'Access-Control-Allow-Origin': '*',
                'Accept': 'application/json',
                'Authorization': `LSAT ${localStorage.getItem('macaroon')}:${localStorage.getItem("preimage")}`,
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