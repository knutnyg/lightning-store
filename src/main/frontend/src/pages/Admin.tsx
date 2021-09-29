import {FormEvent, useEffect, useState} from "react";
import {baseUrl} from "../App";

export interface PageProps {
    onChange: (title: string) => void;
}

export interface Image {
    objUrl: string,
    payload: Blob
}

export const Admin = (props: PageProps) => {

    const [image, setImage] = useState<undefined | Image>(undefined)
    const [name, setName] = useState<undefined | string>(undefined)
    const [price, setPrice] = useState<undefined | number>(1)

    useEffect(() => {
        props.onChange("Admin 🔐")
    })

    const handleSubmit = (event: FormEvent) => {
        event.preventDefault()
        toBase64(image?.payload!!)
            .then((payload) => {
                return fetch(`${baseUrl}/admin/product`, {
                    method: 'POST',
                    headers: {
                        'Access-Control-Allow-Origin': '*',
                        'Accept': 'application/json',
                        'Content-Type': 'application/json',
                        'Authorization': `LSAT ${localStorage.getItem('macaroon')}:${localStorage.getItem("preimage")}`,
                    },
                    body: JSON.stringify({
                        name: name,
                        mediaType: image?.payload.type,
                        data: payload,
                        price: 1,
                    })
                }).catch(err => {
                    console.log(err)
                    throw err
                })
            })
    }

    return (
        <div>
            {image && <img src={image.objUrl}/>}
            <form onSubmit={handleSubmit}>
                <div>
                    <label>name:</label><input type="text" name="name" onChange={(ev) => setName(ev.target.value)}/>
                    <label>mime-type</label><input type="text" defaultValue={image?.payload.type}
                                                   onChange={(ev) => setName(ev.target.value)}/>
                    <label>price:</label><input type="text" name="price" defaultValue={"1"}
                                                onChange={(ev) => setName(ev.target.value)}/>
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

const toBase64 = (file: Blob) => new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.readAsDataURL(file);
    reader.onload = () => resolve(reader.result);
    reader.onerror = error => reject(error);
});