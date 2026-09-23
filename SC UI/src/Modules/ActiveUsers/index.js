import React from "react";
import { API } from "./../../axios";
import { apiEndpoints } from "./../../endpoints";

const POLL_MS = 3000;
const MAX_FEED = 2000; // cap client-side feed to keep the DOM light

// tail -f style live view of user activity (fed by the in-memory buffer in the gateway)
class ActiveUsers extends React.Component {
  state = {
    entries: [],
    activeUsers: [],
    lastSeq: 0,
    enabled: true,
    activeMinutes: 15,
    filter: "",
    autoScroll: true,
    paused: false,
    error: null,
    loading: true,
  };

  feedRef = React.createRef();

  componentDidMount() {
    this.fetch();
    this.timer = setInterval(() => {
      if (!this.state.paused) this.fetch();
    }, POLL_MS);
  }

  componentWillUnmount() {
    clearInterval(this.timer);
  }

  componentDidUpdate(_prevProps, prevState) {
    if (this.state.autoScroll && prevState.entries !== this.state.entries && this.feedRef.current) {
      this.feedRef.current.scrollTop = this.feedRef.current.scrollHeight;
    }
  }

  fetch = async () => {
    try {
      const res = await API.GET(
        apiEndpoints.liveActivity(this.state.lastSeq, this.state.activeMinutes)
      );
      if (!res.success) {
        this.setState({ error: "Failed to load activity", loading: false });
        return;
      }
      const data = res.data || {};
      const incoming = data.entries || [];
      this.setState((prev) => {
        const merged = incoming.length ? [...prev.entries, ...incoming] : prev.entries;
        const trimmed = merged.length > MAX_FEED ? merged.slice(merged.length - MAX_FEED) : merged;
        return {
          entries: trimmed,
          activeUsers: data.activeUsers || [],
          lastSeq: data.lastSeq != null ? data.lastSeq : prev.lastSeq,
          enabled: data.enabled !== false,
          error: null,
          loading: false,
        };
      });
    } catch (e) {
      this.setState({ error: "Failed to load activity", loading: false });
    }
  };

  clearFeed = () => this.setState({ entries: [] });

  render() {
    const { entries, activeUsers, enabled, activeMinutes, filter, autoScroll, paused, error, loading } = this.state;
    const f = filter.trim().toLowerCase();
    const shown = f
      ? entries.filter(
          (e) =>
            (e.username || "").toLowerCase().includes(f) ||
            (e.url || "").toLowerCase().includes(f) ||
            (e.tenant || "").toLowerCase().includes(f)
        )
      : entries;

    return (
      <div style={{ padding: 20 }}>
        <h2 style={{ margin: "0 0 4px" }}>Live Activity</h2>
        <div style={{ color: "#777", fontSize: 13, marginBottom: 16 }}>
          Who is active right now — in-memory feed, resets on server restart, keeps last {activeMinutes}+ min.
        </div>

        {!enabled && (
          <div style={{ background: "#fff3cd", border: "1px solid #ffe69c", padding: "8px 12px", borderRadius: 4, marginBottom: 16, fontSize: 13 }}>
            Activity tracking is disabled (<code>activity.tracking.enabled=false</code>). No data will appear until it is enabled.
          </div>
        )}

        {/* Active users summary */}
        <div style={{ marginBottom: 16 }}>
          <div style={{ fontWeight: 600, marginBottom: 8 }}>
            {activeUsers.length} user{activeUsers.length === 1 ? "" : "s"} active (last {activeMinutes} min)
          </div>
          <div style={{ display: "flex", flexWrap: "wrap", gap: 8 }}>
            {activeUsers.map((u) => (
              <span
                key={u.username}
                title={`Last activity: ${u.lastActivity}`}
                style={{
                  display: "inline-flex", alignItems: "center", gap: 6,
                  background: "#e8f0fe", color: "#1a3d7c", border: "1px solid #c5d6f5",
                  borderRadius: 14, padding: "4px 10px", fontSize: 12.5,
                }}
              >
                <span style={{ width: 8, height: 8, borderRadius: "50%", background: u.secondsAgo <= 90 ? "#2e9e44" : "#c9a227" }} />
                {u.username}
                <span style={{ color: "#6b7fa3" }}>· {u.minutesAgo <= 0 ? "just now" : `${u.minutesAgo}m ago`}</span>
              </span>
            ))}
            {activeUsers.length === 0 && !loading && (
              <span style={{ color: "#999", fontSize: 13 }}>No one active in the window.</span>
            )}
          </div>
        </div>

        {/* Controls */}
        <div style={{ display: "flex", alignItems: "center", gap: 12, flexWrap: "wrap", marginBottom: 10 }}>
          <label style={{ fontSize: 13 }}>
            Active window:{" "}
            <select
              value={activeMinutes}
              onChange={(e) => this.setState({ activeMinutes: Number(e.target.value) }, this.fetch)}
            >
              <option value={5}>5 min</option>
              <option value={15}>15 min</option>
              <option value={30}>30 min</option>
              <option value={60}>60 min</option>
            </select>
          </label>
          <input
            type="text"
            placeholder="Filter user / url / project"
            value={filter}
            onChange={(e) => this.setState({ filter: e.target.value })}
            style={{ padding: "5px 8px", border: "1px solid #ccc", borderRadius: 4, fontSize: 13, minWidth: 220 }}
          />
          <label style={{ fontSize: 13 }}>
            <input type="checkbox" checked={autoScroll} onChange={(e) => this.setState({ autoScroll: e.target.checked })} /> Auto-scroll
          </label>
          <button onClick={() => this.setState({ paused: !paused })} style={btn}>
            {paused ? "Resume" : "Pause"}
          </button>
          <button onClick={this.clearFeed} style={btn}>Clear</button>
          <span style={{ fontSize: 12, color: paused ? "#c0392b" : "#2e9e44" }}>
            {paused ? "paused" : "live"} · {shown.length} lines
          </span>
        </div>

        {error && <div style={{ color: "#c0392b", fontSize: 13, marginBottom: 8 }}>{error}</div>}

        {/* Feed */}
        <div
          ref={this.feedRef}
          style={{
            background: "#0f1419", color: "#d6deeb", fontFamily: "'SF Mono', Menlo, Consolas, monospace",
            fontSize: 12.5, lineHeight: 1.6, padding: 12, borderRadius: 6, height: "60vh",
            overflowY: "auto", whiteSpace: "nowrap",
          }}
        >
          {shown.length === 0 && (
            <div style={{ color: "#5b6b7f" }}>{loading ? "Loading…" : "Waiting for activity…"}</div>
          )}
          {shown.map((e) => (
            <div key={e.seq} style={{ overflow: "hidden", textOverflow: "ellipsis" }}>
              <span style={{ color: "#5b6b7f" }}>{e.timestamp}</span>{" "}
              <span style={{ color: "#7fd1b9", fontWeight: 600 }}>{e.username}</span>{" "}
              {e.tenant ? <span style={{ color: "#c792ea" }}>[{e.tenant}] </span> : null}
              <span style={{ color: methodColor(e.method) }}>{e.method}</span>{" "}
              <span style={{ color: "#a6accd" }}>{e.url}</span>
            </div>
          ))}
        </div>
      </div>
    );
  }
}

const btn = {
  padding: "5px 12px", border: "1px solid #ccc", borderRadius: 4,
  background: "#fff", cursor: "pointer", fontSize: 13,
};

function methodColor(m) {
  switch ((m || "").toUpperCase()) {
    case "GET": return "#82aaff";
    case "POST": return "#c3e88d";
    case "PUT": return "#ffcb6b";
    case "DELETE": return "#f07178";
    default: return "#a6accd";
  }
}

export default ActiveUsers;
