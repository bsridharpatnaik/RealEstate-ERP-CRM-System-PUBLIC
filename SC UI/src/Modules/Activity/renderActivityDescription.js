import React from 'react';

const thStyle = {
  padding: '3px 8px',
  textAlign: 'left',
  fontWeight: 600,
  borderBottom: '1px solid #ddd',
  background: '#f5f5f5',
  fontSize: '11px',
};

const tdStyle = {
  padding: '3px 8px',
  borderBottom: '1px solid #f0f0f0',
  fontSize: '12px',
};

/**
 * Renders an activity log description.
 * If description is a JSON object with an `items` array, renders a mini-table.
 * Falls back to plain text for old-format entries.
 */
const INWARD_TYPE_STYLES = {
  'From PO':        { color: '#1565c0', background: '#e3f2fd' },
  'Sample Inward':  { color: '#6a1b9a', background: '#f3e5f5' },
  'Direct Inward':  { color: '#2e7d32', background: '#e8f5e9' },
  'Opening Stock':  { color: '#e65100', background: '#fff3e0' },
};

export function renderActivityDescription(description) {
  if (!description) return null;
  try {
    const parsed = JSON.parse(description);
    const { summary, items, inwardType } = parsed;

    const typeBadge = inwardType ? (() => {
      const s = INWARD_TYPE_STYLES[inwardType] || { color: '#555', background: '#f0f0f0' };
      return (
        <span style={{
          ...s,
          display: 'inline-block',
          padding: '1px 8px',
          borderRadius: '10px',
          fontSize: '11px',
          fontWeight: 600,
          marginBottom: '4px',
        }}>{inwardType}</span>
      );
    })() : null;

    if (!items || items.length === 0) {
      return (
        <div>
          {typeBadge}
          {typeBadge && <br />}
          <span>{summary || description}</span>
        </div>
      );
    }

    const isChange = items[0].hasOwnProperty('oldQty');

    return (
      <div>
        {typeBadge && <div style={{ marginBottom: '4px' }}>{typeBadge}</div>}
        <div style={{ fontSize: '12px', color: '#555', marginBottom: '4px' }}>{summary}</div>
        <table style={{ borderCollapse: 'collapse', width: '100%', minWidth: '200px' }}>
          <thead>
            <tr>
              <th style={thStyle}>Product</th>
              {isChange ? (
                <>
                  <th style={{ ...thStyle, color: '#c62828' }}>Old Qty</th>
                  <th style={{ ...thStyle, color: '#2e7d32' }}>New Qty</th>
                </>
              ) : (
                <th style={thStyle}>Qty</th>
              )}
            </tr>
          </thead>
          <tbody>
            {items.map((item, i) => (
              <tr key={i}>
                <td style={tdStyle}>{item.product}</td>
                {isChange ? (
                  <>
                    <td style={{ ...tdStyle, color: '#c62828' }}>{item.oldQty}</td>
                    <td style={{ ...tdStyle, color: '#2e7d32' }}>{item.newQty}</td>
                  </>
                ) : (
                  <td style={tdStyle}>{item.qty}</td>
                )}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  } catch (e) {
    // Not JSON — old format or plain text
    return <span>{description}</span>;
  }
}
