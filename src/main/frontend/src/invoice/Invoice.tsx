import {useState} from "react";
import QRCode from "qrcode.react";

export interface InvoiceViewProps {
    paymentReq: string,
    description: string
}

export const InvoiceView = (props: InvoiceViewProps) => {
    const [success, setSuccess] = useState<boolean>(false)
    return <div className={"invoice-view"}>
            <QRCode className={"centered qr-code"} value={`lightning:${props.paymentReq}`} onClick={() => {
                navigator.clipboard.writeText(props.paymentReq)
                    .then(r => {
                        setSuccess(true)
                        setTimeout(() => {
                            setSuccess(false)
                        }, 2000)
                    })
            }}/>
        {success && <span>Kopiert!</span>}
        <p>{props.description}</p>
    </div>
}
