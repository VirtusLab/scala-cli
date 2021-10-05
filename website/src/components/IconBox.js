import React from 'react';

export default function IconBox(props){ 
  return <div className="section-features__item col col--4">
		<div className="section-features__item-wrapper">
			<div className="icon">
				{!props.icon ? "" : <img src={props.icon} alt={props.title} />}
			</div>
			<div className="title">{props.title}</div>
			<div className="desc">{props.children}</div>
		</div>
    </div>
}