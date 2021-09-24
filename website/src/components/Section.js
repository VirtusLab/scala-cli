import React from 'react';

export default function Section(props){ 
  return <div className="row">
  <div className="col col--7 col--offset-1 text--left">
    <div className="text--center padding--lg"/>
    {props.children}
  </div>
  <div className="col col--4">
    {props.image ? <img src={props.image}/> : ""}
  </div>
</div>
}