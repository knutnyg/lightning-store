import './App.css';
import {LSATView} from './pages/Register';
import {updateUser, useTitle, useUser} from "./hooks/useUser";
import useInterval from "./hooks/useInterval";
import {
    BrowserRouter as Router,
    Switch,
    Route,
    Link
} from "react-router-dom";
import {PaywallView} from "./pages/Blog";
import {Bitcoin} from "./pages/Bitcoin";
import {Lightning} from "./pages/Lightning";
import {Header} from "./Header";
import {Home} from './pages/Home';


// export const baseUrl = "http://localhost:8081"
export const baseUrl = "https://store-api.nygaard.xyz"

function App() {
    const [user, setUser] = useUser()
    const [title, setTitle] = useTitle()
    useInterval(() => {
        if (!user) {
            updateUser()
                .then(_user => setUser(_user))
                .catch(err => console.log(err))
        }
    }, 3000)

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
                        <Route path="/"><Home onChange={newtitle}/></Route>
                    </Switch>
                    <Link to="/">Back</Link>
                </Router>
                <footer>
                    <span>Take a look at the code on <a
                        href={"https://github.com/knutnyg/lightning-store/"}>github</a>. </span>
                    <span>Connect to my <a
                        href={"https://1ml.com/node/020deb273bd81cd6771ec3397403f2e74a3c22f8f4c052321c30e5c612cf538328"}>lightning node</a></span>
                </footer>
            </div>

        </div>
    );
}


export default App;
