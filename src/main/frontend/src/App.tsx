import './App.scss';
import {LSATView} from './pages/Register';
import {updateUser, useTitle, useUser} from "./hooks/useUser";
import {BrowserRouter, Route, Routes,} from "react-router-dom";
import {PaywallView} from "./pages/Blog";
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
                <BrowserRouter>
                    <Routes>
                        <Route path="/lsat" element={<LSATView onChange={newtitle}/>}/>
                        <Route path="/blog-paywall" element={<PaywallView onChange={newtitle}/>}/>
                        <Route path="/about" element={<About onChange={newtitle}/>}/>
                        <Route path="/gallery" element={<Kunstig onChange={newtitle} updateUser={callbackUpdateUser}
                                                                 user={user}/>}/>
                        <Route path="/kunstig"
                               element={<WorkshopWrapper onChange={newtitle} updateUser={callbackUpdateUser}
                                                         user={user}/>}/>
                        <Route path="/admin" element={<Admin onChange={newtitle}/>}/>
                        <Route path="/" element={<WorkshopWrapper onChange={newtitle} updateUser={callbackUpdateUser}
                                                                  user={user}/>}/>
                    </Routes>
                </BrowserRouter>
            </div>
        </div>
    );
}

export default App;
