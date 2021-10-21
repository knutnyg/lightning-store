import './App.scss';
import {LSATView} from './pages/Register';
import {updateUser, useTitle, useUser} from "./hooks/useUser";
import {
    BrowserRouter as Router,
    Switch,
    Route, Link,
} from "react-router-dom";
import {PaywallView} from "./pages/Blog";
import {Bitcoin} from "./pages/Bitcoin";
import {Lightning} from "./pages/Lightning";
import {Header} from "./Header";
import {useEffect} from "react";
import {Admin} from "./pages/Admin";
import {Kunstig} from "./pages/Kunstig";
import {WorkshopWrapper} from "./pages/WorkshopWrapper";
import {About} from "./pages/About";

const resolveBaseUrl = (host: string): string => {
    switch (host) {
        // switch port if react dev-server
        case 'http://localhost:8080':
        case 'http://localhost:3000':
            return `http://localhost:8081/api`
        // use current host
        default:
            return `${host}/api`
    }
}

export const baseUrl = resolveBaseUrl(window.location.origin)

function App() {
    const [user, setUser] = useUser()
    const [title, setTitle] = useTitle()

    useEffect(() => {
        if (!user) {
            updateUser()
                .then(_user => setUser(_user))
                .catch(err => console.log(err))
        }
    })

    const newtitle = (title: string) => {
        setTitle(title)
    }

    const callbackUpdateUser = () => {
        updateUser()
            .then(_user => {
                setUser(_user);
            })
            .catch(err => console.log(err))
    }

    return (
        <div className="main">
            <Header title={title} user={user}/>
            <div className="content">
                <Router>
                    <Switch>
                        <Route path="/lsat"><LSATView onChange={newtitle}/></Route>
                        <Route path="/blog-paywall"><PaywallView onChange={newtitle}/></Route>
                        <Route path="/bitcoin-network"><Bitcoin onChange={newtitle}/></Route>
                        <Route path="/lightning-network"><Lightning onChange={newtitle}/></Route>
                        <Route path="/about"><About onChange={newtitle}/></Route>
                        <Route path="/gallery"><Kunstig onChange={newtitle} updateUser={callbackUpdateUser}
                                                        user={user}/></Route>
                        <Route path="/kunstig"><WorkshopWrapper onChange={newtitle} updateUser={callbackUpdateUser}
                                                                user={user}/></Route>
                        <Route path="/admin"><Admin onChange={newtitle}/></Route>
                        <Route path="/"><WorkshopWrapper onChange={newtitle} updateUser={callbackUpdateUser}
                                                         user={user}/></Route>
                    </Switch>
                </Router>
            </div>
        </div>
    );
}

export default App;
