import React from 'react';

class DownloadButton extends React.Component {
  
  constructor(props) {
    super(props);

    this.handleClick = this.handleClick.bind(this);
  }

  handleClick(e) {
      window.location.href=this.props.href;
  }

  render() {
    return (
      <button 
      style={{height: '55px', width : '190px', fontSize: 15,  borderRadius: '3px', color: '#fff', background: '#DC332D', borderColor: '#DC332D', fontWeight: 500 }}
      onClick={this.handleClick}
      >
        {this.props.desc}
      </button>
    );
  }
}

export default DownloadButton;