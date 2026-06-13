import React, { Component } from 'react';
import LowStockReport from '../Reports/LowStock/index';
import DeadStockReport from './DeadStockReport';

class GlobalStockReports extends Component {
  state = {
    activeTab: 'lowStock',
  };

  render() {
    const { activeTab } = this.state;
    return (
      <div style={{ height: '100%', display: 'flex', flexDirection: 'column' }}>
        <div style={{ display: 'flex', borderBottom: '2px solid #e0e0e0', backgroundColor: 'white', padding: '0 16px' }}>
          <button
            onClick={() => this.setState({ activeTab: 'lowStock' })}
            style={{
              padding: '12px 20px',
              border: 'none',
              cursor: 'pointer',
              fontSize: '13px',
              fontWeight: 600,
              backgroundColor: 'transparent',
              borderBottom: activeTab === 'lowStock' ? '2px solid #f39c12' : '2px solid transparent',
              color: activeTab === 'lowStock' ? '#f39c12' : '#666',
              marginBottom: '-2px',
            }}
          >
            Low Stock
          </button>
          <button
            onClick={() => this.setState({ activeTab: 'deadStock' })}
            style={{
              padding: '12px 20px',
              border: 'none',
              cursor: 'pointer',
              fontSize: '13px',
              fontWeight: 600,
              backgroundColor: 'transparent',
              borderBottom: activeTab === 'deadStock' ? '2px solid #b71c1c' : '2px solid transparent',
              color: activeTab === 'deadStock' ? '#b71c1c' : '#666',
              marginBottom: '-2px',
            }}
          >
            Dead Stock
          </button>
        </div>
        <div style={{ flex: 1, overflow: 'auto' }}>
          {activeTab === 'lowStock' && <LowStockReport {...this.props} />}
          {activeTab === 'deadStock' && <DeadStockReport {...this.props} />}
        </div>
      </div>
    );
  }
}

export default GlobalStockReports;
