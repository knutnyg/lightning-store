import './App.css';
import {LSATView} from './pages/Register';
import {updateUser, useTitle, useUser} from "./hooks/useUser";
import {
    BrowserRouter as Router,
    Switch,
    Route,
} from "react-router-dom";
import {PaywallView} from "./pages/Blog";
import {Bitcoin} from "./pages/Bitcoin";
import {Lightning} from "./pages/Lightning";
import {Header} from "./Header";
import {Home} from './pages/Home';
import {useEffect} from "react";
import {Kunstig} from "./pages/Kunstig";
import {Admin} from "./pages/Admin";

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
                        <Route path="/kunstig"><Kunstig onChange={newtitle}/></Route>
                        <Route path="/admin"><Admin onChange={newtitle}/></Route>
                        <Route path="/"><Home onChange={newtitle}/></Route>
                    </Switch>
                </Router>
            </div>
        </div>
    );
}

export default App;
