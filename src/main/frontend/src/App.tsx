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

export const baseUrl = process.env.NODE_ENV === 'production'
    ? 'https://store-api.nygaard.xyz' : 'http://localhost:8081';

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
                        <Route path="/s/lsat"><LSATView onChange={newtitle}/></Route>
                        <Route path="/s/blog-paywall"><PaywallView onChange={newtitle}/></Route>
                        <Route path="/s/bitcoin-network"><Bitcoin onChange={newtitle}/></Route>
                        <Route path="/s/lightning-network"><Lightning onChange={newtitle}/></Route>
                        <Route path="/s/ai-painting"><Kunstig onChange={newtitle}/></Route>
                        <Route path="/"><Home onChange={newtitle}/></Route>
                    </Switch>
                </Router>
            </div>
        </div>
    );
}


export default App;
