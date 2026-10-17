import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { Analyzer } from './pages/Analyzer'
import { Portfolio } from './pages/Portfolio'
import './App.css'

function App() {
  return (
    <BrowserRouter>
      <div className="app-shell">
        <Routes>
          <Route path="/" element={<Portfolio />} />
          <Route path="/analyze" element={<Analyzer />} />
        </Routes>
        <footer className="site-footer">
          <span>Dependency Migration Intelligence — deterministic MVP</span>
        </footer>
      </div>
    </BrowserRouter>
  )
}

export default App
