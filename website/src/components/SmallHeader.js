import React from 'react';

export default function SmallHeader(props){ 
  return <div className="section__header">
		<h2>{props.title}</h2>
		<div className="section__header-description">
			{props.children}
		</div>
	</div>
}