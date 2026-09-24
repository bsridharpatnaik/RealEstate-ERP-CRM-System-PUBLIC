import React, { useEffect, useState } from "react";
import ReactMarkdown from "react-markdown";
import { API } from "../../axios";
import { apiEndpoints } from "../../endpoints";
import "./style.scss";

// Global Dashboard "Needs attention" (rule-based, no AI). Lines depend on the user's role and projects.
// No links: the list pages can't filter by these rules (e.g. "no progress for 60+ days"), so counts wouldn't match.
const DISMISS_KEY = "attentionBannerDismissed";

const isDismissed = () => {
  try { return sessionStorage.getItem(DISMISS_KEY) === "1"; } catch (e) { return false; }
};

const AttentionBanner = () => {
  const [lines, setLines] = useState(null);
  const [hidden, setHidden] = useState(isDismissed);

  useEffect(() => {
    if (hidden) return undefined;
    let alive = true;
    API.GET(apiEndpoints.smartSuggestionAttention).then((res) => alive && res.success && setLines(res.data || []));
    return () => { alive = false; };
  }, [hidden]);

  if (hidden || !lines || lines.length === 0) return null;
  const dismiss = () => {
    try { sessionStorage.setItem(DISMISS_KEY, "1"); } catch (e) { /* hide for this view only */ }
    setHidden(true);
  };

  return (
    <div className="attention-banner" role="region" aria-label="Needs attention">
      <div className="attention-banner-head">
        <span className="smart-suggestion-title">✨ Needs attention</span>
        <button type="button" className="attention-banner-close" onClick={dismiss} aria-label="Hide until next login">×</button>
      </div>
      <ul>
        {lines.map((l) => (
          <li key={l.kind}>
            <ReactMarkdown components={{ p: "span" }}>{l.text}</ReactMarkdown>
          </li>
        ))}
      </ul>
    </div>
  );
};

export default AttentionBanner;
