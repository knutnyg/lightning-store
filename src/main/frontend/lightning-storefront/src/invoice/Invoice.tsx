import {useState} from "react";
import {createInvoice, Invoice, updateInvoice} from "./invoices";
import useInterval from "../hooks/useInterval";
import QRCode from "qrcode.react";

export const DonateView = () => {
    const [invoice, setInvoice] = useState<Invoice | undefined>(undefined);

    useInterval(() => {
        if (invoice && invoice?.inProgress) {
            updateInvoice(invoice)
                .then(_invoice => setInvoice(_invoice))
        }
    }, 500)

    let component;

    if (invoice === undefined) {
        component = <div>
            <button onClick={() => {
                createInvoice()
                    .then(_invoice => setInvoice(_invoice))
            }}>Donate 10 sats
            </button>
        </div>
    } else if (invoice.settled) {
        component = <div className={"invoice-view"}>
            <p>Thank you!😎</p>
            <button onClick={() => {
                setInvoice(undefined)
            }}>Again!! 🤠
            </button>
        </div>;
    } else {
        component = <div className={"invoice-view"}>
            <QRCode value={invoice!.paymentRequest}/>
            <p>Please scan QR code with your phone</p>
        </div>;
    }

    return (
        <div className={"invoice-view"}>
            {component}
        </div>
    )

}