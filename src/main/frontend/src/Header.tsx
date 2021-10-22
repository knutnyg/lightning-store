import {User} from "./hooks/useUser";

export interface HeaderProps {
    title: string,
    user: User | undefined
}

export const Header = (props: HeaderProps) => <header>
    <h1 className={props.title === 'Galleriet' ? 'gallery-title' : ''}>️{props.title}</h1>
    {props.user && <span className="user"/>}
    {!props.user && <span className="user"/>}
</header>