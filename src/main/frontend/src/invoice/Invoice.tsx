import {useState} from "react";
import {SubmitHandler, useForm} from "react-hook-form";
import {createInvoice, Invoice, updateInvoice} from "./invoices";
import useInterval from "../hooks/useInterval";
import QRCode from "qrcode.react";

type FormData = {
    amount: number;
    memo: string;
};

export const DonateView = () => {
    const [invoice, setInvoice] = useState<Invoice | undefined>(undefined);
    const {register, handleSubmit, watch, formState: {errors}} = useForm<FormData>();
    const onSubmit: SubmitHandler<FormData> = (data) => {
        createInvoice(data.amount, data.memo)
            .then(_invoice => setInvoice(_invoice))
    }

    useInterval(() => {
        if (invoice && invoice?.inProgress) {
            updateInvoice(invoice)
                .then(_invoice => setInvoice(_invoice))
        }
    }, 500)

    let component;

    if (invoice === undefined) {
        component = <div>
            <form onSubmit={handleSubmit(onSubmit)}>
                <div className={"form-row"}>
                    <label>Amount</label>
                    <input className={"amount"} defaultValue={10} {...register("amount", {required: true, min: 0})} />
                </div>
                <div className={"form-row"}>
                    <label>Memo</label>
                    <textarea className={"memo"} defaultValue={""} {...register("memo", {
                        required: false,
                        maxLength: 150
                    })} />
                </div>
                <button onSubmit={handleSubmit(onSubmit)}>Get Invoice</button>
            </form>
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