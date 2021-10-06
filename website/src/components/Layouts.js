import React from 'react';

export function HeaderSection(props){ 
  return <div className="row headerSection padding--lg margin--lg">
  <div className="col col--6 text--left">
    {props.children}
  </div>
  <div className="col col--6">
    {props.image ? <img src={props.image}/> : ""}
  </div>
</div>
}

export function TitledSection(props){ 
  return <div className="row titledSection padding--lg margin--lg">
    <div className="col col--3">
      <h1>{props.title}</h1>
    </div>
    <div className="col col--9 text--left">
      {props.children}
    </div>
</div>
}

