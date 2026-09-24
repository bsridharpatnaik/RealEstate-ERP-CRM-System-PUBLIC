import React, { useEffect, useRef, useState } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { Fab, IconButton, TextField, Tooltip, CircularProgress } from "@material-ui/core";
import CloseIcon from "@material-ui/icons/Close";
import SendIcon from "@material-ui/icons/Send";
import AddCommentIcon from "@material-ui/icons/AddComment";
import FullscreenIcon from "@material-ui/icons/Fullscreen";
import FullscreenExitIcon from "@material-ui/icons/FullscreenExit";
import {
  ResponsiveContainer, BarChart, Bar, LineChart, Line, PieChart, Pie, Cell,
  XAxis, YAxis, Tooltip as ChartTooltip, Legend, CartesianGrid,
} from "recharts";
import { API } from "../../axios";
import { apiEndpoints } from "../../endpoints";
import { getToken, getRole } from "../../helper";
import "./style.scss";

// Router uses forceRefresh (full reload on every navigation) — keep the chat in sessionStorage.
const STORE_KEY = "aiAssistantChat";
// Chat is for admins + purchase managers. The server enforces the same roles.
const CHAT_ROLES = ["admin", "purchase-manager"];
export const isAiUser = () => !!getToken() && CHAT_ROLES.includes((getRole() || "").toLowerCase());
const COLORS = ["#2a78d6", "#e8743b", "#19a979", "#945ecf", "#d64e4e", "#13a4b4", "#bf8b16", "#6c757d"];

const loadChat = () => {
  try {
    return JSON.parse(sessionStorage.getItem(STORE_KEY)) || { messages: [], sessionId: null };
  } catch (e) {
    return { messages: [], sessionId: null };
  }
};

// ```chart {"type":"bar|line|pie","title","xKey","series":[{"key","label"}],"data":[...]} ```
const AiChart = ({ json }) => {
  let spec;
  try {
    spec = JSON.parse(json);
  } catch (e) {
    return <pre>{json}</pre>;
  }
  const { type = "bar", title, xKey, series = [], data = [] } = spec;
  let chart;
  if (type === "pie") {
    const s = series[0] || {};
    chart = (
      <PieChart>
        <Pie data={data} dataKey={s.key} nameKey={xKey} outerRadius={80} label>
          {data.map((_, i) => <Cell key={i} fill={COLORS[i % COLORS.length]} />)}
        </Pie>
        <ChartTooltip /><Legend />
      </PieChart>
    );
  } else {
    const Chart = type === "line" ? LineChart : BarChart;
    chart = (
      <Chart data={data}>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey={xKey} tick={{ fontSize: 11 }} />
        <YAxis tick={{ fontSize: 11 }} tickFormatter={(v) => Number(v).toLocaleString("en-IN")} />
        <ChartTooltip formatter={(v) => Number(v).toLocaleString("en-IN")} />
        {series.length > 1 && <Legend />}
        {series.map((s, i) =>
          type === "line"
            ? <Line key={s.key} dataKey={s.key} name={s.label} stroke={COLORS[i % COLORS.length]} dot={false} />
            : <Bar key={s.key} dataKey={s.key} name={s.label} fill={COLORS[i % COLORS.length]} />
        )}
      </Chart>
    );
  }
  return (
    <div className="ai-chart">
      {title && <div className="ai-chart-title">{title}</div>}
      <ResponsiveContainer width="100%" height={220}>{chart}</ResponsiveContainer>
    </div>
  );
};

// Fenced blocks: ```chart → chart, ```sql → collapsed "View query", anything else → plain code.
const PreBlock = ({ children }) => {
  const code = React.Children.toArray(children)[0];
  const lang = ((code && code.props && code.props.className) || "").replace("language-", "");
  const text = String((code && code.props && code.props.children) || "").replace(/\n$/, "");
  if (lang === "chart") return <AiChart json={text} />;
  if (lang === "sql") {
    return (
      <details className="ai-sql">
        <summary>View query</summary>
        <pre>{text}</pre>
      </details>
    );
  }
  return <pre>{text}</pre>;
};

const AiAssistant = () => {
  const [open, setOpen] = useState(false);
  const [chat, setChat] = useState(loadChat);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [usage, setUsage] = useState(null);
  const [maximized, setMaximized] = useState(false);
  const listRef = useRef(null);

  useEffect(() => {
    sessionStorage.setItem(STORE_KEY, JSON.stringify(chat));
    // scrollTop, not scrollIntoView: scrollIntoView also scrolled the overflow:hidden panel on mobile
    if (listRef.current) listRef.current.scrollTop = listRef.current.scrollHeight;
  }, [chat, open]);

  useEffect(() => {
    if (!open) return;
    API.GET(apiEndpoints.aiAssistantUsage).then((res) => res.success && setUsage(res.data));
  }, [open]);

  if (!isAiUser()) return null;

  const limitReached = !!usage && usage.usedToday >= usage.dailyLimit;

  const addMessage = (msg, sessionId) =>
    setChat((c) => ({ messages: [...c.messages, msg], sessionId: sessionId === undefined ? c.sessionId : sessionId }));

  const send = async () => {
    const question = input.trim();
    if (!question || loading) return;
    setInput("");
    addMessage({ role: "user", text: question });
    setLoading(true);
    const res = await API.POST(apiEndpoints.aiAssistantAsk, { question, sessionId: chat.sessionId });
    setLoading(false);
    if (res.success) {
      addMessage({ role: "assistant", text: res.data.answer }, res.data.sessionId);
      setUsage({ usedToday: res.data.usedToday, dailyLimit: res.data.dailyLimit });
    } else {
      addMessage({ role: "error", text: res.errorMessage || "Something went wrong. Please try again." });
      if (res.status === 429) API.GET(apiEndpoints.aiAssistantUsage).then((u) => u.success && setUsage(u.data));
    }
  };

  const onKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      send();
    }
  };

  if (!open) {
    return (
      <Tooltip title="Ask AI about your ERP data" placement="left">
        <Fab variant="extended" className="ai-fab" onClick={() => setOpen(true)} aria-label="Open Ask AI">
          <span className="ai-fab-spark" aria-hidden="true">✨</span> Ask AI
        </Fab>
      </Tooltip>
    );
  }

  return (
    <div className={`ai-panel${maximized ? " ai-maximized" : ""}`} role="dialog" aria-label="AI assistant">
      <div className="ai-header">
        <span className="ai-title">Ask AI <small>beta</small></span>
        <Tooltip title="New chat">
          <IconButton size="small" onClick={() => setChat({ messages: [], sessionId: null })} aria-label="New chat">
            <AddCommentIcon fontSize="small" />
          </IconButton>
        </Tooltip>
        <Tooltip title={maximized ? "Restore" : "Maximize"}>
          <IconButton size="small" onClick={() => setMaximized(!maximized)} aria-label={maximized ? "Restore" : "Maximize"}>
            {maximized ? <FullscreenExitIcon fontSize="small" /> : <FullscreenIcon fontSize="small" />}
          </IconButton>
        </Tooltip>
        <IconButton size="small" onClick={() => setOpen(false)} aria-label="Close">
          <CloseIcon fontSize="small" />
        </IconButton>
      </div>

      <div className="ai-messages" ref={listRef}>
        {chat.messages.length === 0 && (
          <div className="ai-empty">
            Ask about your data or how the app works, e.g.
            <ul>
              <li>Purchase orders created this week</li>
              <li>Current cement stock in Bhaav Bhumi</li>
              <li>PO value by month for the last 6 months as a chart</li>
              <li>How does short-closing a PO work?</li>
            </ul>
          </div>
        )}
        {chat.messages.map((m, i) => (
          <div key={i} className={`ai-msg ai-${m.role}`}>
            {m.role === "assistant" ? (
              <ReactMarkdown remarkPlugins={[remarkGfm]} components={{ pre: PreBlock }}>
                {m.text}
              </ReactMarkdown>
            ) : (
              m.text
            )}
          </div>
        ))}
        {loading && (
          <div className="ai-msg ai-assistant ai-thinking">
            <CircularProgress size={14} /> Thinking… (usually 10–30 seconds)
          </div>
        )}
      </div>

      <div className="ai-input">
        <TextField
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={onKeyDown}
          placeholder={limitReached ? "Daily AI limit reached — resets at midnight" : "Ask a question…"}
          multiline
          rowsMax={4}
          fullWidth
          variant="outlined"
          size="small"
          inputProps={{ maxLength: 2000, "aria-label": "Question" }}
          disabled={loading || limitReached}
        />
        <IconButton color="primary" onClick={send} disabled={loading || limitReached || !input.trim()} aria-label="Send">
          <SendIcon />
        </IconButton>
      </div>
      {usage && (
        <div className="ai-usage">
          Today: {usage.usedToday} of {usage.dailyLimit} questions
        </div>
      )}
    </div>
  );
};

export default AiAssistant;
