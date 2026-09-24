import React from "react";
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";

// Admin: per-user AI chat usage, daily question limits, and who asked what.
class AiUsage extends React.Component {
  state = { report: null, usernames: [], edits: {}, newUser: "", newLimit: "", error: null, saving: false, history: null, historyUser: "" };

  componentDidMount() {
    this.load();
    this.loadHistory("");
    API.GET(apiEndpoints.getUsers).then((res) => res.success && this.setState({ usernames: res.data.usernames || [] }));
  }

  load = async () => {
    const res = await API.GET(apiEndpoints.aiUsageReport);
    if (res.success) this.setState({ report: res.data, edits: {}, error: null });
    else this.setState({ error: res.errorMessage || "Failed to load AI usage" });
  };

  // dailyRequests "" = remove the user's own limit (use default)
  save = async (username, dailyRequests) => {
    this.setState({ saving: true });
    const res = await API.PUT(apiEndpoints.aiUsageLimits, { username, dailyRequests: dailyRequests === "" ? null : Number(dailyRequests) });
    this.setState({ saving: false });
    if (res.success) this.setState({ report: res.data, edits: {}, newUser: "", newLimit: "", error: null });
    else this.setState({ error: res.errorMessage || "Failed to save limit" });
  };

  loadHistory = async (historyUser) => {
    this.setState({ historyUser, history: null });
    const res = await API.GET(apiEndpoints.aiUsageHistory(historyUser));
    this.setState({ history: res.success ? res.data : [] });
    if (!res.success) this.setState({ error: res.errorMessage || "Failed to load AI history" });
  };

  edit = (key, value) => this.setState((s) => ({ edits: { ...s.edits, [key]: value } }));

  render() {
    const { report, usernames, edits, newUser, newLimit, error, saving, history, historyUser } = this.state;
    if (!report) return <div style={{ padding: 20 }}>{error || "Loading…"}</div>;
    const def = edits["*"] !== undefined ? edits["*"] : String(report.defaultDailyLimit);

    return (
      <div style={{ padding: 20 }}>
        <h2 style={{ margin: "0 0 4px" }}>AI Usage</h2>
        <div style={{ color: "#777", fontSize: 13, marginBottom: 16 }}>
          AI chat questions per user. Limits reset at midnight.
        </div>
        {error && <div style={{ color: "#c0392b", fontSize: 13, marginBottom: 8 }}>{error}</div>}

        <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap", marginBottom: 16 }}>
          <label style={{ fontSize: 13, fontWeight: 600 }}>Default limit per user per day:</label>
          <input type="number" min="0" value={def} onChange={(e) => this.edit("*", e.target.value)} style={input} />
          <button style={btn} disabled={saving || def === "" || def === String(report.defaultDailyLimit)} onClick={() => this.save("*", def)}>
            Save
          </button>
        </div>

        <div style={{ overflowX: "auto" }}>
          <table style={{ borderCollapse: "collapse", fontSize: 13, minWidth: 640 }}>
            <thead>
              <tr>
                {["User", "Questions today", "Daily limit", "Last 30 days", "Cost (30 days)"].map((h) => (
                  <th key={h} style={th}>{h}</th>
                ))}
              </tr>
            </thead>
            <tbody>
              {report.users.map((u) => {
                const limit = u.dailyLimit == null ? "" : String(u.dailyLimit);
                const value = edits[u.username] !== undefined ? edits[u.username] : limit;
                const today = u.chatToday;
                const effective = u.dailyLimit == null ? report.defaultDailyLimit : u.dailyLimit;
                return (
                  <tr key={u.username} style={today >= effective ? { background: "#fdecea" } : null}>
                    <td style={td}>
                      <button style={link} title="Show this user's history" onClick={() => this.loadHistory(u.username)}>{u.username}</button>
                    </td>
                    <td style={td}>{u.chatToday}</td>
                    <td style={td}>
                      <input type="number" min="0" value={value} placeholder={`default (${report.defaultDailyLimit})`}
                        onChange={(e) => this.edit(u.username, e.target.value)} style={{ ...input, width: 110 }} />
                      {value !== limit && (
                        <button style={{ ...btn, marginLeft: 6 }} disabled={saving} onClick={() => this.save(u.username, value)}>Save</button>
                      )}
                    </td>
                    <td style={td}>{u.requests30d}</td>
                    <td style={td}>${Number(u.cost30dUsd).toFixed(2)}</td>
                  </tr>
                );
              })}
              {report.users.length === 0 && (
                <tr><td style={td} colSpan={5}>No AI usage yet.</td></tr>
              )}
            </tbody>
          </table>
        </div>

        <div style={{ display: "flex", alignItems: "center", gap: 8, flexWrap: "wrap", marginTop: 16 }}>
          <label style={{ fontSize: 13, fontWeight: 600 }}>Set limit for user:</label>
          <input list="ai-usage-users" placeholder="Username" value={newUser} onChange={(e) => this.setState({ newUser: e.target.value })} style={{ ...input, width: 160 }} />
          <datalist id="ai-usage-users">
            {usernames.map((n) => <option key={n} value={n} />)}
          </datalist>
          <input type="number" min="0" placeholder="Requests/day" value={newLimit} onChange={(e) => this.setState({ newLimit: e.target.value })} style={input} />
          <button style={btn} disabled={saving || !newUser.trim() || newLimit === ""} onClick={() => this.save(newUser.trim(), newLimit)}>
            Add
          </button>
          <span style={{ fontSize: 12, color: "#777" }}>0 blocks the user.</span>
        </div>

        <h3 style={{ margin: "28px 0 8px" }}>History (last 30 days)</h3>
        <div style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 10 }}>
          <label style={{ fontSize: 13 }}>User:</label>
          <select value={historyUser} onChange={(e) => this.loadHistory(e.target.value)} style={{ ...input, width: "auto" }}>
            <option value="">All users</option>
            {report.users.map((u) => <option key={u.username} value={u.username}>{u.username}</option>)}
          </select>
          <span style={{ fontSize: 12, color: "#777" }}>Click an entry to see the AI's answer.</span>
        </div>
        {!history && <div style={{ fontSize: 13 }}>Loading…</div>}
        {history && history.length === 0 && <div style={{ fontSize: 13, color: "#777" }}>No AI activity in the last 30 days.</div>}
        {history && history.map((h, i) => (
          <details key={i} style={{ borderBottom: "1px solid #eee", padding: "6px 0", fontSize: 13 }}>
            <summary style={{ cursor: "pointer", listStylePosition: "outside" }}>
              <span style={{ color: "#777" }}>{h.time}</span>{" · "}
              <b>{h.username}</b>{" · "}
              {h.status !== "OK" && <span style={{ ...tag, background: "#fdecea" }}>{h.status.toLowerCase()}</span>}{" "}
              {h.text}
            </summary>
            <div style={{ whiteSpace: "pre-wrap", background: "#fafafa", border: "1px solid #eee", borderRadius: 4, padding: 10, marginTop: 6 }}>
              {h.answer || "(no answer)"}
            </div>
          </details>
        ))}
      </div>
    );
  }
}

const input = { padding: "5px 8px", border: "1px solid #ccc", borderRadius: 4, fontSize: 13, width: 90 };
const btn = { padding: "5px 12px", border: "1px solid #ccc", borderRadius: 4, background: "#fff", cursor: "pointer", fontSize: 13 };
const th = { textAlign: "left", padding: "8px 10px", borderBottom: "2px solid #ddd", whiteSpace: "nowrap" };
const link = { background: "none", border: "none", padding: 0, color: "#2a78d6", cursor: "pointer", fontSize: 13, textDecoration: "underline" };
const tag = { display: "inline-block", background: "#f0f0f0", borderRadius: 10, padding: "0 8px", fontSize: 12 };
const td = { padding: "6px 10px", borderBottom: "1px solid #eee", whiteSpace: "nowrap" };

export default AiUsage;
