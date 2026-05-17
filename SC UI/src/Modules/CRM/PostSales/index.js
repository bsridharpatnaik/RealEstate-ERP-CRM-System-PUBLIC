import React from 'react';
import Common from '../../../Shared/CommonIndex';
import { messages } from "./../../../messages";
import List from './list';

class PostSales extends Common {
    title = messages.common.postsales;
    render() {
        return (
          <div className="page">
            <div className="header-info">
              <div>
                {this.renderHeading()}
                {this.renderBreadcrums(messages.common.crm)}
              </div>
            </div>
              <List
                isLoading={(bIsLoading) => this.setState({ isLoading: bIsLoading })}
                setOptions={(options) => (this.dropdowns = options)}
              />
          </div>
        );
      }
}


export default PostSales;