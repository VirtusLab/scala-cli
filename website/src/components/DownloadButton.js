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
      class="button button--danger button--outline"
      onClick={this.handleClick}
      >
        {this.props.desc}
      </button>
    );
  }
}

export default DownloadButton;