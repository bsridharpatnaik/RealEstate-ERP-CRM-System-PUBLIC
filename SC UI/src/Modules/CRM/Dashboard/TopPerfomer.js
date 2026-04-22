import React, { Component } from "react";
import { API } from "./../../../axios";
import { apiEndpoints } from "./../../../endpoints";
import Skeleton from "@material-ui/lab/Skeleton";

const height = 35;
class TopPerformer extends Component {

    state = {isTopPerformerLoaded: false};
    data = {};
    async componentDidMount() {
        this.setState({isTopPerformerLoaded: false})
        const response = await API.GET(apiEndpoints.gettopPerformerDB);
        if(response.success){
            this.data = response.data;
            this.setState({isTopPerformerLoaded: true})
        }
    }

    renderLoader() {
        return (
          <React.Fragment>
            <Skeleton variant="rect" height={height} />
            <Skeleton variant="rect" height={height} />
            <Skeleton variant="rect" height={height} />
            <Skeleton variant="rect" height={height} />
            <Skeleton variant="rect" height={height} />
          </React.Fragment>
        );
      }


      renderData() {
        return (
          <div className="topPerformer-details">
                <span className="name">{this.data.username}</span>
                <div className="top">
                    <div class="left">                        
                        <div className="header">Lead Generated</div>
                        <div className="leadgenerated count">{this.data.leadsGenerated > 9 ? this.data.leadsGenerated : '0' + this.data.leadsGenerated}</div>
                    </div>
                    <div class="right">
                        <div className="header">Property Visit</div>
                        <div className="propertyvisit count">{this.data.propertyVisits > 9 ? this.data.propertyVisits : '0' + this.data.propertyVisits}</div>
                    </div>
                </div>
                <div className="bottom">
                    <div class="left">
                        <div className="header">Negotiation</div>
                        <div className="negotiation count">{this.data.inNegotiation > 9 ? this.data.inNegotiation : '0' + this.data.inNegotiation}</div>
                    </div>
                    <div class="right">
                        <div className="header">Deal Close</div>
                        <div className="dealclose count">{this.data.dealsClosed > 9 ? this.data.dealsClosed : '0' + this.data.dealsClosed}</div>
                    </div>
                </div>
          </div>
        );
      }
      
      
    render() {
        return (
            <div className="notification">
                <div className="dashboard-heading performer">
                    Top Performer
                </div>                
                {this.state.isTopPerformerLoaded ? this.renderData() : this.renderLoader()}
            </div>
        )
    }
}

export default TopPerformer;