import React, { Component } from 'react';
import { API } from '../../../axios';
import { apiEndpoints } from '../../../endpoints';
import { withSnackbar } from 'notistack';
import CircularProgress from '@material-ui/core/CircularProgress';
import BOQTracker from './index';

class BOQTrackerGlobal extends Component {
  state = {
    projects: [],
    loadingProjects: true,
    selectedTenantCode: '',
  };

  async componentDidMount() {
    const res = await API.GET(apiEndpoints.getTenants);
    if (res.success) {
      const projects = (res.data || [])
        .filter(t => t.inventory === true)
        .map(t => ({ name: t.tenantName || t.name || '', code: t.tenantCode }))
        .filter(t => t.name && t.code)
        .sort((a, b) => a.name.localeCompare(b.name));
      this.setState({ projects, loadingProjects: false });
    } else {
      this.props.enqueueSnackbar('Failed to load projects', { variant: 'error' });
      this.setState({ loadingProjects: false });
    }
  }

  render() {
    const { projects, loadingProjects, selectedTenantCode } = this.state;

    return (
      <div style={{ fontFamily: 'inherit' }}>
        {/* Project selector bar */}
        <div style={{ padding: '16px 28px', background: '#f7f8fa', borderBottom: '1px solid #e2e8f0', display: 'flex', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
          <span style={{ fontSize: 13, fontWeight: 600, color: '#4a5568' }}>Project:</span>
          {loadingProjects ? (
            <CircularProgress size={20} />
          ) : (
            <select
              value={selectedTenantCode}
              onChange={e => this.setState({ selectedTenantCode: e.target.value })}
              style={{ padding: '8px 14px', borderRadius: 6, border: '1px solid #d1d5db', fontSize: 13, background: '#fff', outline: 'none', cursor: 'pointer', minWidth: 240 }}
            >
              <option value="">— Select a project —</option>
              {projects.map(p => <option key={p.code} value={p.code}>{p.name}</option>)}
            </select>
          )}
        </div>

        {/* BOQ Tracker — reuses exact same component, tenantCode prop overrides tenant-id header */}
        {selectedTenantCode ? (
          <BOQTracker key={selectedTenantCode} tenantCode={selectedTenantCode} {...this.props} />
        ) : (
          <div style={{ padding: 64, textAlign: 'center', color: '#a0aec0', fontSize: 14 }}>
            Select a project above to view BOQ Tracker data.
          </div>
        )}
      </div>
    );
  }
}

export default withSnackbar(BOQTrackerGlobal);
