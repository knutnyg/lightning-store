import {useState} from "react";
import {createInvoice, Invoice, updateInvoice} from "./invoices";
import useInterval from "../hooks/useInterval";
import QRCode from "qrcode.react";

export const DonateView = () => {
    const [invoice, setInvoice] = useState<Invoice | undefined>(undefined);

    useInterval(() => {
        console.log('checking hook')
        if (invoice && invoice?.inProgress) {
            console.log('should update invoice')
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
    } else if (invoice.settled !== null) {
        component = <p>Thank you!😎</p>;
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