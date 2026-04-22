import React, { useState, useEffect } from 'react';
import { Activity, AlertTriangle, PhoneCall, DollarSign, Database, Server, ShieldOff, Network, Layers } from 'lucide-react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, AreaChart, Area, CartesianGrid } from 'recharts';

const generatePhoneNumber = () => `9${Math.floor(Math.random() * 900000000 + 100000000)}`;

// Initial chart data
const initialRevenueData = [
  { name: 'National', value: 45000 },
  { name: 'Metro', value: 68000 },
  { name: 'Circle A', value: 38000 },
  { name: 'Circle B', value: 24000 },
  { name: 'Circle C', value: 18000 },
];

const initialVolData = [
  { time: '00:00', calls: 1200 },
  { time: '04:00', calls: 800 },
  { time: '08:00', calls: 5600 },
  { time: '12:00', calls: 8900 },
  { time: '16:00', calls: 7600 },
  { time: '20:00', calls: 9200 },
];

export default function App() {
  const [cdrs, setCdrs] = useState([]);
  const [frauds, setFrauds] = useState([]);
  const [metrics, setMetrics] = useState({
    totalCalls: 1245890,
    fraudAlerts: 42,
    activeTowers: 1845,
    revenue: 485900
  });

  const [revData, setRevData] = useState(initialRevenueData);
  const [volData, setVolData] = useState(initialVolData);
  const [topCallers, setTopCallers] = useState([]);

  // Fetch from Java Backend
  useEffect(() => {
    const fetchAnalytics = async () => {
      try {
        const response = await fetch('http://localhost:8080/api/dashboard/data');
        if (!response.ok) return;
        const data = await response.json();

        // Update states from Redis payload if available
        if (data.metrics) setMetrics(data.metrics);
        if (data.stream) {
          const mappedStream = data.stream.map(c => ({
            id: c.callerNumber + c.startTime,
            caller: c.callerNumber,
            receiver: c.receiverNumber,
            time: new Date(c.startTime).toLocaleTimeString('en-US', { hour12: false })
          }));
          setCdrs(mappedStream);
        }
        if (data.frauds) setFrauds(data.frauds);
        if (data.topCallers) setTopCallers(data.topCallers);
        if (data.revData && data.revData.length > 0) setRevData(data.revData);
        if (data.volData && data.volData.length > 0) setVolData(data.volData);

      } catch (err) {
        console.error("Could not fetch dashboard data from backend.", err);
      }
    };

    fetchAnalytics(); // Fetch immediately on mount
    const pollInterval = setInterval(fetchAnalytics, 2000); // And poll every 2 seconds

    return () => clearInterval(pollInterval);
  }, []);

  return (
    <div className="app-container">
      {/* Header */}
      <div className="top-bar">
        <h1><Activity color="var(--accent-blue)" /> Telecom Analytics Pipeline</h1>
        <div className="live-indicator">
          <div className="live-dot"></div> Live Connection: ClickHouse + Neo4j
        </div>
      </div>

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
            <div className="value">${metrics.revenue.toLocaleString(undefined, { minimumFractionDigits: 0, maximumFractionDigits: 0 })}</div>
          </div>
        </div>

        <div className="glass-panel metric-card">
          <div className="metric-icon-wrap purple">
            <Server size={28} />
          </div>
          <div className="metric-data">
            <h3>Active Cell Towers</h3>
            <div className="value">{metrics.activeTowers.toLocaleString()}</div>
          </div>
        </div>

        <div className="glass-panel metric-card" style={{ borderColor: 'rgba(239, 68, 68, 0.2)' }}>
          <div className="metric-icon-wrap red">
            <AlertTriangle size={28} />
          </div>
          <div className="metric-data">
            <h3>Fraud Alerts</h3>
            <div className="value" style={{ color: 'var(--accent-red)' }}>{metrics.fraudAlerts}</div>
          </div>
        </div>
      </div>

      {/* Main Content Areas */}
      <div className="main-content">

        {/* Left: Stream Feed */}
        <div className="glass-panel animate-enter" style={{ animationDelay: '0.1s' }}>
          <div className="panel-header">
            <Activity size={18} color="var(--text-muted)" /> Kafka Stream (cdr_raw)
          </div>
          <div className="panel-body">
            {cdrs.map((c) => (
              <div key={c.id} className="feed-item animate-enter">
                <div>
                  <span className="caller">{c.caller}</span> ➔ <span className="receiver">{c.receiver}</span>
                </div>
                <div className="time">{c.time}</div>
              </div>
            ))}
          </div>
        </div>

        {/* Center: OLAP Cubes */}
        <div className="glass-panel animate-enter" style={{ animationDelay: '0.2s', padding: '1.5rem', overflowY: 'auto' }}>
          <h2 style={{ fontSize: '1.1rem', marginBottom: '2rem', display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Database size={20} color="var(--accent-purple)" /> ClickHouse OLAP Rollups
          </h2>

          <div className="chart-container">
            <div className="chart-title">Revenue by Circle (Monthly Cube)</div>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={revData} margin={{ top: 0, right: 0, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" vertical={false} />
                <XAxis dataKey="name" stroke="rgba(255,255,255,0.3)" fontSize={12} tickLine={false} />
                <YAxis stroke="rgba(255,255,255,0.3)" fontSize={12} tickLine={false} axisLine={false} tickFormatter={(val) => `$${val / 1000}k`} />
                <Tooltip cursor={{ fill: 'rgba(255,255,255,0.02)' }} contentStyle={{ backgroundColor: '#161925', borderColor: 'rgba(255,255,255,0.1)', borderRadius: '8px' }} />
                <Bar dataKey="value" fill="var(--accent-purple)" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>

          <div className="chart-container" style={{ height: '220px', marginTop: '2rem' }}>
            <div className="chart-title">Network Trafffic Hierarchy (Time of Day)</div>
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={volData} margin={{ top: 0, right: 0, left: -10, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorCalls" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="var(--accent-blue)" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="var(--accent-blue)" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <XAxis dataKey="time" stroke="rgba(255,255,255,0.3)" fontSize={12} tickLine={false} />
                <YAxis stroke="rgba(255,255,255,0.3)" fontSize={12} tickLine={false} axisLine={false} />
                <Tooltip contentStyle={{ backgroundColor: '#161925', borderColor: 'rgba(255,255,255,0.1)', borderRadius: '8px' }} />
                <Area type="monotone" dataKey="calls" stroke="var(--accent-blue)" strokeWidth={3} fillOpacity={1} fill="url(#colorCalls)" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Right: Fraud Monitor */}
        <div className="glass-panel animate-enter" style={{ animationDelay: '0.3s' }}>
          <div className="panel-header" style={{ color: 'var(--accent-red)' }}>
            <ShieldOff size={18} /> Neo4j Graph Monitor
            {frauds.length > 0 && <span className="badge-alert" style={{ marginLeft: 'auto' }}>ACTIVE</span>}
          </div>
          <div className="panel-body">
            {frauds.length === 0 ? (
              <div style={{ color: 'var(--text-muted)', textAlign: 'center', marginTop: '4rem' }}>
                <Network size={48} opacity={0.2} style={{ margin: '0 auto 1rem' }} />
                <p>Graph analyzing continuous calls...</p>
                <p style={{ fontSize: '0.8rem', marginTop: '0.5rem' }}>Awaiting pattern match</p>
              </div>
            ) : frauds.map((f) => (
              <div key={f.id} className="fraud-item animate-enter">
                <div className="fraud-title">
                  {f.type}
                  <AlertTriangle size={14} />
                </div>
                <div className="fraud-detail highlight-node" style={{ marginBottom: '0.5rem' }}>
                  {f.details}
                </div>
                <div className="fraud-detail" style={{ color: 'rgba(255,255,255,0.5)' }}>
                  {f.desc}
                </div>
              </div>
            ))}
          </div>
        </div>

      </div>

      {/* Bottom Section: Detailed OLAP Drill-Down */}
      <div className="glass-panel animate-enter" style={{ animationDelay: '0.4s', padding: '1.5rem' }}>
        <div className="panel-header" style={{ borderBottom: 'none', padding: '0 0 1rem 0' }}>
          <Layers size={20} color="var(--accent-purple)" />
          Top 10 Callers Leaderboard (Live Aggregation)
        </div>
        <div className="olap-report-container">
          <table className="olap-table">
            <thead>
              <tr>
                <th>Rank</th>
                <th>Caller Network ID</th>
                <th className="numeric">Total Calls</th>
                <th className="numeric">Duration (Hrs)</th>
                <th className="numeric">Total Revenue</th>
              </tr>
            </thead>
            <tbody>
              {topCallers.map((c, index) => (
                <tr key={index}>
                  <td>#{index + 1}</td>
                  <td style={{ color: 'var(--accent-blue)', fontWeight: 500 }}>{c.caller}</td>
                  <td className="numeric">{c.calls.toLocaleString()}</td>
                  <td className="numeric">{(c.duration / 3600).toFixed(2)}</td>
                  <td className="numeric" style={{ color: 'var(--accent-green)' }}>
                    ${c.revenue.toLocaleString(undefined, { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </td>
                </tr>
              ))}
              {topCallers.length === 0 && (
                <tr>
                  <td colSpan="5" style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>
                    Waiting for data...
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
