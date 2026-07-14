import React, { Component } from 'react';
import LowStockReport from '../Reports/LowStock/index';
import DeadStockReport from './DeadStockReport';
import ExpiredStockReport from './ExpiredStockReport';

const TABS = [
  { key: 'lowStock',     label: 'Low Stock',     color: '#f39c12' },
  { key: 'deadStock',    label: 'Dead Stock',     color: '#b71c1c' },
  { key: 'expiredStock', label: 'Expired Stock',  color: '#e65100' },
];

class GlobalStockReports extends Component {
  state = {
    activeTab: 'lowStock',
  };

  render() {
    const { activeTab } = this.state;
    return (
      <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
        <div style={{ display: 'flex', borderBottom: '2px solid #e0e0e0', backgroundColor: 'white', padding: '0 16px' }}>
          {TABS.map(tab => (
            <button
              key={tab.key}
              onClick={() => this.setState({ activeTab: tab.key })}
              style={{
                padding: '12px 20px',
                border: 'none',
                cursor: 'pointer',
                fontSize: '13px',
                fontWeight: 600,
                backgroundColor: 'transparent',
                borderBottom: activeTab === tab.key ? `2px solid ${tab.color}` : '2px solid transparent',
                color: activeTab === tab.key ? tab.color : '#666',
                marginBottom: '-2px',
              }}
            >
              {tab.label}
            </button>
          ))}
        </div>
        <div style={{ flex: 1, overflow: 'auto' }}>
          {activeTab === 'lowStock'     && <LowStockReport {...this.props} />}
          {activeTab === 'deadStock'    && <DeadStockReport {...this.props} />}
          {activeTab === 'expiredStock' && <ExpiredStockReport {...this.props} />}
        </div>
      </div>
    );
  }
}

export default GlobalStockReports;
