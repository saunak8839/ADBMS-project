import React, { useState, useEffect } from 'react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, AreaChart, Area, CartesianGrid, PieChart, Pie, Cell } from 'recharts';
import { TrendingUp, Globe, PieChart as PieIcon, Clock, ShieldCheck, Zap } from 'lucide-react';

const COLORS = ['#10b981', '#ef4444', '#f59e0b', '#3b82f6'];

export default function Analytics() {
  const [hourlyData, setHourlyData] = useState([]);
  const [regionalData, setRegionalData] = useState([]);
  const [callHealthData, setCallHealthData] = useState([]);
  const [topSpenders, setTopSpenders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState(null);

  useEffect(() => {
    const fetchOlapData = async () => {
      try {
        setErrorMsg(null);
        const [hourlyRes, regionalRes, healthRes, spendersRes] = await Promise.all([
          fetch('http://localhost:8080/api/olap/hourly-trend'),
          fetch('http://localhost:8080/api/olap/regional-distribution'),
          fetch('http://localhost:8080/api/olap/call-status-analytics'),
          fetch('http://localhost:8080/api/olap/high-value-callers')
        ]);

        if (!hourlyRes.ok) throw new Error("Backend connection failed (500)");

        const hourly = await hourlyRes.json();
        const regional = await regionalRes.json();
        const health = await healthRes.json();
        const spenders = await spendersRes.json();
        
        // Robust mapping
        setHourlyData(hourly.map(d => {
          const t = d.TIME || d.time || 0;
          const h = Math.floor(t / 100);
          const m = t % 100;
          return { 
            time: `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}`, 
            revenue: d.TOTAL_REVENUE || d.total_revenue || 0, 
            calls: d.CALL_COUNT || d.call_count || 0 
          };
        }));
        
        setRegionalData(regional.map(d => ({ 
          name: d.REGION || d.region, 
          value: d.REVENUE || d.revenue || 0, 
          calls: d.CALLS || d.calls || 0 
        })));

        setCallHealthData(health.map(d => ({ 
          name: d.CALL_STATUS || d.call_status, 
          value: d.TOTAL_CALLS || d.total_calls || 0 
        })));

        setTopSpenders(spenders.map(d => ({
          phone: d.PHONE_NUMBER || d.phone_number,
          calls: d.TOTAL_CALLS || d.total_calls,
          spent: d.TOTAL_SPENT || d.total_spent
        })));

        setLoading(false);
      } catch (err) {
        console.error("OLAP Fetch Error:", err);
        setErrorMsg("Failed to connect to OLAP Warehouse. Check if ClickHouse is up!");
        setLoading(false);
      }
    };

    fetchOlapData();
    const interval = setInterval(fetchOlapData, 60000); // Auto-refresh every minute
    return () => clearInterval(interval);
  }, []);

  if (loading) return (
    <div className="analytics-loading-screen">
      <div className="spinner"></div>
      <p>Initializing ClickHouse OLAP Engine...</p>
    </div>
  );

  if (errorMsg) return (
    <div className="analytics-error-screen">
      <div className="error-icon">⚠️</div>
      <h3>Analytics Offline</h3>
      <p>{errorMsg}</p>
      <button onClick={() => window.location.reload()} className="refresh-btn">Try Reconnecting</button>
    </div>
  );

  return (
    <div className="analytics-page animate-enter">
      <div className="page-header">
        <div>
          <h2><TrendingUp size={24} color="var(--accent-purple)" /> Historical Insights Warehouse</h2>
          <p className="text-muted">High-speed aggregations powered by ClickHouse Star Schema</p>
        </div>
      </div>

      <div className="analytics-grid">
        {/* Row 1: Minute Trends */}
        <div className="glass-panel wide-panel">
          <div className="panel-header">
            <Clock size={18} /> Minute-by-Minute Revenue & Traffic (Today)
          </div>
          <div className="chart-wrapper">
            <ResponsiveContainer width="100%" height={300}>
              <AreaChart data={hourlyData}>
                <defs>
                  <linearGradient id="olapColor" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="var(--accent-purple)" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="var(--accent-purple)" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" stroke="rgba(255,255,255,0.05)" vertical={false} />
                <XAxis 
                  dataKey="time" 
                  stroke="rgba(255,255,255,0.3)" 
                  minTickGap={60} // Prevents label crowding
                />
                <YAxis yAxisId="left" stroke="rgba(255,255,255,0.3)" />
                <YAxis yAxisId="right" orientation="right" stroke="rgba(255,255,255,0.3)" />
                <Tooltip contentStyle={{ backgroundColor: '#161925', borderColor: 'rgba(255,255,255,0.1)' }} />
                <Area yAxisId="left" type="monotone" dataKey="revenue" stroke="var(--accent-purple)" fill="url(#olapColor)" strokeWidth={3} />
                <Area yAxisId="right" type="monotone" dataKey="calls" stroke="var(--accent-blue)" fill="transparent" strokeDasharray="5 5" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Row 2: Regional Distribution */}
        <div className="glass-panel">
          <div className="panel-header">
            <Globe size={18} /> Regional Distribution
          </div>
          <div className="chart-wrapper">
            <ResponsiveContainer width="100%" height={250}>
              <BarChart data={regionalData} layout="vertical">
                <XAxis type="number" stroke="rgba(255,255,255,0.3)" hide />
                <YAxis dataKey="name" type="category" stroke="rgba(255,255,255,0.7)" />
                <Tooltip cursor={{ fill: 'rgba(255,255,255,0.05)' }} contentStyle={{ backgroundColor: '#161925', borderColor: 'rgba(255,255,255,0.1)' }} />
                <Bar dataKey="value" fill="var(--accent-blue)" radius={[0, 4, 4, 0]} barSize={20} />
              </BarChart>
            </ResponsiveContainer>
          </div>
          <div className="regional-stats">
            {regionalData.slice(0, 4).map((d, i) => (
              <div key={i} className="stat-row">
                <span>{d.name}</span>
                <span className="font-mono">${d.value.toLocaleString()}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Row 3: Call Health (Replacing Protocol Mix) */}
        <div className="glass-panel">
          <div className="panel-header">
            <ShieldCheck size={18} /> Service Health (Success Rate)
          </div>
          <div className="chart-wrapper">
            <ResponsiveContainer width="100%" height={250}>
              <PieChart>
                <Pie
                  data={callHealthData}
                  innerRadius={60}
                  outerRadius={80}
                  paddingAngle={5}
                  dataKey="value"
                >
                  {callHealthData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={entry.name === 'SUCCESS' ? '#10b981' : '#ef4444'} />
                  ))}
                </Pie>
                <Tooltip contentStyle={{ backgroundColor: '#161925', borderColor: 'rgba(255,255,255,0.1)' }} />
              </PieChart>
            </ResponsiveContainer>
            <div className="pie-legend">
              {callHealthData.map((d, i) => (
                <div key={i} className="legend-item">
                  <div className="dot" style={{ backgroundColor: d.name === 'SUCCESS' ? '#10b981' : '#ef4444' }}></div>
                  <span>{d.name}: {d.value}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Row 4: Top High-Value Callers (New) */}
        <div className="glass-panel wide-panel">
          <div className="panel-header">
            <Zap size={18} /> Top High-Value Subscribers (Today)
          </div>
          <div className="panel-body">
            <table className="olap-table">
              <thead>
                <tr>
                  <th>Phone Number</th>
                  <th className="numeric">Calls</th>
                  <th className="numeric">Revenue Generated</th>
                </tr>
              </thead>
              <tbody>
                {topSpenders.map((s, i) => (
                  <tr key={i}>
                    <td>{s.phone}</td>
                    <td className="numeric">{s.calls}</td>
                    <td className="numeric font-mono text-green">${s.spent.toLocaleString()}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  );
}
