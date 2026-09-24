import React, { useEffect, useState } from "react";
import ReactMarkdown from "react-markdown";
import { API } from "../../axios";
import "./style.scss";

// Rule-based notes (no AI). Pass `url` to load the text from the server (indent / PO details), or `text`
// when the page builds it itself (Create PO rates). Stays hidden when there is nothing to say or loading fails.
const SmartSuggestion = ({ url, text: givenText, footer }) => {
  const [loadedText, setLoadedText] = useState(null);

  useEffect(() => {
    if (!url) return undefined;
    let alive = true;
    API.GET(url).then((res) => alive && res.success && setLoadedText(res.data.text));
    return () => { alive = false; };
  }, [url]);

  const text = url ? loadedText : givenText;
  if (!text) return null;
  return (
    <div className="smart-suggestion">
      <div className="smart-suggestion-title">✨ Smart suggestion</div>
      <ReactMarkdown>{text}</ReactMarkdown>
      {footer && <div className="smart-suggestion-meta">{footer}</div>}
    </div>
  );
};

export default SmartSuggestion;
