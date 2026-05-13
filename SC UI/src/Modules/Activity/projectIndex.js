import React from 'react';
import { Slide } from '@material-ui/core';
import Common from './../../Shared/CommonIndex';
import ProjectList from './projectList';

class ProjectActivity extends Common {
  state = { list: true };

  render() {
    this.title = 'Activity Log';
    return (
      <div className="page">
        <div className="header-info">
          <div>{this.renderHeading()}</div>
        </div>
        <Slide direction="right" in={this.state.list} mountOnEnter unmountOnExit timeout={{ exit: 0 }}>
          <div><ProjectList /></div>
        </Slide>
      </div>
    );
  }
}

export default ProjectActivity;
