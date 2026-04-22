import React, { useState } from 'react';
import { Activity, TrendingUp, Cpu } from 'lucide-react';
import LiveDashboard from './LiveDashboard';
import Analytics from './Analytics';

export default function App() {
  const [activeTab, setActiveTab] = useState('live');

  return (
    <div className="app-layout">
      {/* Sidebar Navigation */}
      <nav className="sidebar">
        <div className="logo">
          <Cpu size={32} color="var(--accent-blue)" />
          <span>TeleCrunch 2.0</span>
        </div>
        
        <div className="nav-items">
          <button 
            className={`nav-btn ${activeTab === 'live' ? 'active' : ''}`}
            onClick={() => setActiveTab('live')}
          >
            <Activity size={20} />
            <span>Live Stream</span>
          </button>
          
          <button 
            className={`nav-btn ${activeTab === 'analytics' ? 'active' : ''}`}
            onClick={() => setActiveTab('analytics')}
          >
            <TrendingUp size={20} />
            <span>OLAP Insights</span>
          </button>
        </div>

        <div className="system-status">
          <div className="status-item">
            <div className="status-dot green"></div>
            <span>Kafka: OK</span>
          </div>
          <div className="status-item">
            <div className="status-dot green"></div>
            <span>Postgres: OK</span>
          </div>
          <div className="status-item">
            <div className="status-dot blue"></div>
            <span>ClickHouse: OK</span>
          </div>
        </div>
      </nav>

      {/* Main Content Area */}
      <main className="content-area">
        {activeTab === 'live' ? <LiveDashboard /> : <Analytics />}
      </main>
    </div>
  );
}
