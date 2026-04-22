import React, { useState, useEffect } from 'react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, AreaChart, Area, CartesianGrid, PieChart, Pie, Cell } from 'recharts';
import { TrendingUp, Globe, PieChart as PieIcon, Clock, Filter, Download } from 'lucide-react';

const COLORS = ['#6366f1', '#a855f7', '#ec4899', '#f43f5e'];

export default function Analytics() {
  const [hourlyData, setHourlyData] = useState([]);
  const [regionalData, setRegionalData] = useState([]);
  const [callTypeData, setCallTypeData] = useState([]);
  const [loading, setLoading] = useState(true);
  const [errorMsg, setErrorMsg] = useState(null);

  useEffect(() => {
    const fetchOlapData = async () => {
      try {
        setErrorMsg(null);
        const [hourlyRes, regionalRes, typeRes] = await Promise.all([
          fetch('http://localhost:8080/api/olap/hourly-trend'),
          fetch('http://localhost:8080/api/olap/regional-distribution'),
          fetch('http://localhost:8080/api/olap/call-type-analytics')
        ]);

        if (!hourlyRes.ok) throw new Error("Backend connection failed (500)");

        const hourly = await hourlyRes.json();
        const regional = await regionalRes.json();
        const types = await typeRes.json();
        
        console.log("DEBUG: OLAP Data Received:", { hourly, regional, types });

        // Safety Check: Ensure we received arrays
        if (!Array.isArray(hourly) || !Array.isArray(regional) || !Array.isArray(types)) {
            console.error("DEBUG: One or more data sources is NOT an array. Backend error?");
            setLoading(false);
            return;
        }

        // Robust mapping
        setHourlyData(hourly.map(d => ({ 
          hour: `${d.HOUR || d.hour || 0}:00`, 
          revenue: d.TOTAL_REVENUE || d.total_revenue || 0, 
          calls: d.CALL_COUNT || d.call_count || 0 
        })));
        
        setRegionalData(regional.map(d => ({ 
          name: d.REGION || d.region, 
          value: d.REVENUE || d.revenue || 0, 
          calls: d.CALLS || d.calls || 0, 
          avg: Math.round(d.AVG_DURATION || d.avg_duration || 0) 
        })));

        setCallTypeData(types.map(d => ({ 
          name: d.CALL_TYPE || d.call_type, 
          value: d.TOTAL_CALLS || d.total_calls || 0 
        })));
        setLoading(false);
      } catch (err) {
        console.error("OLAP Fetch Error:", err);
        setErrorMsg("Failed to connect to OLAP Warehouse. Check if ClickHouse is up!");
        setLoading(false);
      }
    };

    fetchOlapData();
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
        <div className="header-actions">
          <button className="btn-secondary"><Download size={16} /> Export CSV</button>
          <button className="btn-primary"><Filter size={16} /> Filter Date</button>
        </div>
      </div>

      <div className="analytics-grid">
        {/* Row 1: Hourly Trends */}
        <div className="glass-panel wide-panel">
          <div className="panel-header">
            <Clock size={18} /> Hourly Revenue & Traffic Density (24H Rollup)
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
                <XAxis dataKey="hour" stroke="rgba(255,255,255,0.3)" />
                <YAxis yAxisId="left" stroke="rgba(255,255,255,0.3)" />
                <YAxis yAxisId="right" orientation="right" stroke="rgba(255,255,255,0.3)" />
                <Tooltip contentStyle={{ backgroundColor: '#161925', borderColor: 'rgba(255,255,255,0.1)' }} />
                <Area yAxisId="left" type="monotone" dataKey="revenue" stroke="var(--accent-purple)" fill="url(#olapColor)" strokeWidth={3} />
                <Area yAxisId="right" type="monotone" dataKey="calls" stroke="var(--accent-blue)" fill="transparent" strokeDasharray="5 5" />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Row 2: Regional & Call Types */}
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
            {regionalData.map((d, i) => (
              <div key={i} className="stat-row">
                <span>{d.name}</span>
                <span className="font-mono">${d.value.toLocaleString()}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="glass-panel">
          <div className="panel-header">
            <PieIcon size={18} /> Protocol Mix
          </div>
          <div className="chart-wrapper">
            <ResponsiveContainer width="100%" height={250}>
              <PieChart>
                <Pie
                  data={callTypeData}
                  innerRadius={60}
                  outerRadius={80}
                  paddingAngle={5}
                  dataKey="value"
                >
                  {callTypeData.map((entry, index) => (
                    <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                  ))}
                </Pie>
                <Tooltip contentStyle={{ backgroundColor: '#161925', borderColor: 'rgba(255,255,255,0.1)' }} />
              </PieChart>
            </ResponsiveContainer>
            <div className="pie-legend">
              {callTypeData.map((d, i) => (
                <div key={i} className="legend-item">
                  <div className="dot" style={{ backgroundColor: COLORS[i % COLORS.length] }}></div>
                  <span>{d.name}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
