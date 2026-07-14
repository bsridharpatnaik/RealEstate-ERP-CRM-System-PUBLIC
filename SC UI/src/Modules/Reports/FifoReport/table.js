import React from 'react';
import CommonTable from '../../../Shared/Table';
import { messages } from '../../../messages';
import Tooltip from '@material-ui/core/Tooltip';

// Column definitions: header, key, width (px), align
const COLUMNS = [
  { header: 'Date',            key: 'outwardDate',       width: 95,  align: 'left'  },
  { header: 'Project',         key: 'tenantSchema',      width: 100, align: 'left'  },
  { header: 'Outward ID',      key: 'outwardId',         width: 85,  align: 'right' },
  { header: 'Product',         key: 'productName',       width: 140, align: 'left'  },
  { header: 'Unit',            key: 'measurementUnit',   width: 50,  align: 'center'},
  { header: 'Warehouse',       key: 'warehouseName',     width: 100, align: 'left'  },
  { header: 'Structure',       key: 'usageLocationName', width: 120, align: 'left'  },
  { header: 'Final Location',  key: 'usageAreaName',     width: 150, align: 'left'  },
  { header: 'Contractor',      key: 'contractorName',    width: 130, align: 'left'  },
  { header: 'Lot #',           key: 'batchLotNumber',    width: 80,  align: 'left'  },
  { header: 'Brand',           key: 'batchBrand',        width: 80,  align: 'left'  },
  { header: 'Recv. Date',      key: 'batchReceivedDate', width: 90,  align: 'left'  },
  { header: 'Expiry Date',     key: 'batchExpiryDate',   width: 90,  align: 'left'  },
  { header: 'Qty',             key: 'qtyConsumed',       width: 55,  align: 'right' },
  { header: 'Override Reason', key: 'overrideComment',   width: 140, align: 'left'  },
  { header: 'Performed By',    key: 'performedBy',       width: 110, align: 'left'  },
];

const TOTAL_WIDTH = COLUMNS.reduce((sum, c) => sum + c.width, 0);

const cellBase = {
  overflow: 'hidden',
  textOverflow: 'ellipsis',
  whiteSpace: 'nowrap',
  padding: '7px 8px',
  borderBottom: '1px solid #f0f0f0',
  fontSize: 13,
};

const thBase = {
  ...cellBase,
  background: '#f5f5f5',
  fontWeight: 600,
  fontSize: 12,
  borderBottom: '2px solid #ddd',
  color: '#333',
  position: 'sticky',
  top: 0,
  zIndex: 1,
};

function renderValue(key, row) {
  const val = row[key];
  if (val == null || val === '') return <span style={{ color: '#ccc' }}>—</span>;
  return val;
}

class FifoReportTable extends CommonTable {

  renderHeader() {
    return (
      <tr>
        {COLUMNS.map((col) => (
          <th key={col.key} style={{ ...thBase, width: col.width, textAlign: col.align }}>
            {col.header}
          </th>
        ))}
      </tr>
    );
  }

  renderBody() {
    const rows = this.state.rows || [];
    if (rows.length === 0) {
      return (
        <tr>
          <td
            colSpan={COLUMNS.length}
            style={{ textAlign: 'center', padding: 32, color: '#999', fontSize: 13 }}
          >
            {messages.common.noRecords}
          </td>
        </tr>
      );
    }
    return rows.map((row, index) => (
      <tr key={index} style={{ background: index % 2 === 0 ? '#ffffff' : '#fdf6ee' }}>
        {COLUMNS.map((col) => {
          const val = row[col.key];
          const display = (val == null || val === '')
            ? <span style={{ color: '#ccc' }}>—</span>
            : String(val);
          const isString = typeof val === 'string' && val.length > 0;
          return (
            <Tooltip
              key={col.key}
              title={isString ? val : ''}
              placement="top"
              disableHoverListener={!isString}
            >
              <td style={{ ...cellBase, width: col.width, textAlign: col.align }}>
                {display}
              </td>
            </Tooltip>
          );
        })}
      </tr>
    ));
  }

  render() {
    return (
      <div style={{ overflowX: 'auto', width: '100%', border: '1px solid #e8e8e8', borderRadius: 6 }}>
        <table
          style={{
            tableLayout: 'fixed',
            width: TOTAL_WIDTH,
            minWidth: '100%',
            borderCollapse: 'collapse',
          }}
        >
          <colgroup>
            {COLUMNS.map((col) => (
              <col key={col.key} style={{ width: col.width }} />
            ))}
          </colgroup>
          <thead>{this.renderHeader()}</thead>
          <tbody>{this.renderBody()}</tbody>
        </table>
      </div>
    );
  }
}

export default FifoReportTable;
