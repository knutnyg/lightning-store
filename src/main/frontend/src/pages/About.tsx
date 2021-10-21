import {useEffect} from "react";
import {Link} from "react-router-dom";


interface Props {
    onChange: (title: string) => void;
}

export const About = (props: Props) => {

    useEffect(() => {
        props.onChange("Om Galleriet")
    })

    return (<div className="page">
        <p>Dette er historien om to hobbyprosjekter som gjennom tilfeldigheter har funnet
            hverandre. Hege har laget
            laget en AI-modell som har lært seg å male og Knut har laget en digital butikk som foreløpig mangler
            inventar.</p>
        <p>Ut av dette ble konseptet Galleriet skapt: Et sted hvor vi kan oppleve middelmådig kunst sammen, finansiert
            med internettpenger. Galleriet vårt er levende og skapes i løpet av fagdagen med din hjelp.</p>
        <p>Galleriet er bygget ved hjelp av betalingsnettverket Lightning og kunstneren er et
            Generative Adversarial
            Network. Stikk innom, lær om digitale betalinger og gjør din første mikrotransaksjon i dag!</p>
        <p>Hilsen Hege Haavaldsen & Knut Nygaard</p>
        <Link to="/kunstig">Tilbake</Link>
    </div>)
}

