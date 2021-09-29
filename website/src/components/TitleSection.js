import React from 'react';

export default function TitleSection(props){ 
  return <div className="row">
    <div className="install-section col col--6 col--offset-2 text--left">
      {props.children}
    </div>
    <div className="install-section col col--4 text--center">
      {props.right}
    </div>
  </div> 
}