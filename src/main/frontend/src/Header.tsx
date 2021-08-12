import {User} from "./hooks/useUser";

export interface HeaderProps {
    title: string,
    user: User | null
}

export const Header = (props: HeaderProps) => <header>
    <h1>️{props.title}</h1>
    {props.user && <span className="user">Balance: {props.user?.balance}</span>}
    {!props.user && <span className="user">Not logged in</span>}
</header>