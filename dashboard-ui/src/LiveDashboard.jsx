import React, { useState, useEffect } from 'react';
import { Activity, AlertTriangle, PhoneCall, DollarSign, Database, Server, ShieldOff, Network, Layers } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, AreaChart, Area, CartesianGrid } from 'recharts';

export default function LiveDashboard() {
  const [cdrs, setCdrs] = useState([]);
  const [frauds, setFrauds] = useState([]);
  const [metrics, setMetrics] = useState({
    totalCalls: 0,
    fraudAlerts: 0,
    activeTowers: 1845,
    revenue: 0
  });

  const [revData, setRevData] = useState([]);
  const [volData, setVolData] = useState([]);
  const [topCallers, setTopCallers] = useState([]);

  useEffect(() => {
    const fetchAnalytics = async () => {
      try {
        const response = await fetch('http://localhost:8080/api/dashboard/data');
        if (!response.ok) return;
        const data = await response.json();

        if (data.metrics) setMetrics(data.metrics);
        if (data.stream) {
          setCdrs(data.stream.map(c => ({
            id: c.callerNumber + c.startTime + Math.random(),
            caller: c.callerNumber,
            receiver: c.receiverNumber,
            time: new Date(c.startTime).toLocaleTimeString()
          })));
        }
        if (data.frauds) setFrauds(data.frauds);
        if (data.topCallers) setTopCallers(data.topCallers);
        if (data.revData) setRevData(data.revData);
        if (data.volData) setVolData(data.volData);

      } catch (err) {
        console.error("Dashboard Fetch Error:", err);
      }
    };

    fetchAnalytics();
    const pollInterval = setInterval(fetchAnalytics, 2000);
    return () => clearInterval(pollInterval);
  }, []);

  return (
    <div className="dashboard-view">
      {/* Metrics Row */}
      <div className="header-row animate-enter">
        <div className="glass-panel metric-card">
          <div className="metric-icon-wrap blue">
            <PhoneCall size={28} />
          </div>
          <div className="metric-data">
            <h3>Total Processed</h3>
            <div className="value">{metrics.totalCalls.toLocaleString()}</div>
          </div>
        </div>

        <div className="glass-panel metric-card">
          <div className="metric-icon-wrap green">
            <DollarSign size={28} />
          </div>
          <div className="metric-data">
            <h3>Est. Revenue</h3>
            <div className="value">${metrics.revenue.toLocaleString()}</div>
          </div>
        </div>

        <div className="glass-panel metric-card">
          <div className="metric-icon-wrap purple">
            <Server size={28} />
          </div>
          <div className="metric-data">
            <h3>Active Cell Towers</h3>
            <div className="value">{metrics.activeTowers}</div>
          </div>
        </div>

        <div className="glass-panel metric-card danger">
          <div className="metric-icon-wrap red">
            <AlertTriangle size={28} />
          </div>
          <div className="metric-data">
            <h3>Fraud Alerts</h3>
            <div className="value">{metrics.fraudAlerts}</div>
          </div>
        </div>
      </div>

      {/* Main Content */}
      <div className="main-content">
        {/* Stream */}
        <div className="glass-panel stream-panel">
          <div className="panel-header"><Activity size={18} /> Kafka Live Feed</div>
          <div className="panel-body">
            {cdrs.map(c => (
              <div key={c.id} className="feed-item">
                <span>{c.caller} ➔ {c.receiver}</span>
                <span className="time">{c.time}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Charts */}
        <div className="glass-panel chart-panel">
          <div className="panel-header"><Database size={18} /> OLTB Snapshot (Postgres)</div>
          <div className="chart-container">
            <ResponsiveContainer width="100%" height={200}>
              <BarChart data={revData}>
                <XAxis dataKey="name" stroke="rgba(255,255,255,0.3)" />
                <YAxis stroke="rgba(255,255,255,0.3)" />
                <Tooltip />
                <Bar dataKey="value" fill="var(--accent-purple)" />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Fraud */}
        <div className="glass-panel fraud-panel">
          <div className="panel-header danger"><ShieldOff size={18} /> Graph Monitor (Neo4j)</div>
          <div className="panel-body">
            {frauds.map((f, i) => (
              <div key={i} className="fraud-item">
                <div className="fraud-title">{f.type}</div>
                <div className="fraud-detail">{f.details}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
