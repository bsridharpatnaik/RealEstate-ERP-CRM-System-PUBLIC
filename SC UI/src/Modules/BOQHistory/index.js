import React from 'react';
import { Slide } from '@material-ui/core';
import Common from './../../Shared/CommonIndex';
import List from './list';
import { messages } from './../../messages';

class BOQHistory extends Common {
  state = { list: true };

  render() {
    this.title = this.props.pageTitle || 'BOQ History';
    const breadcrumbTitle = this.props.breadcrumbTitle || messages.common.boq;
    return (
      <div className="page">
        <div className="header-info">
          <div>
            {this.renderHeading()}
            {this.renderBreadcrums(breadcrumbTitle)}
          </div>
        </div>
        <Slide direction="right" in={this.state.list} mountOnEnter unmountOnExit timeout={{ exit: 0 }}>
          <div><List /></div>
        </Slide>
      </div>
    );
  }
}

export default BOQHistory;
