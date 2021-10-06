import React from 'react';

export default function BigHeader(props){ 
  return <div className={"col col--" + props.colsize }>
		<h1 className={"section-title" + (props.promptsign ? " with-before" : "")}>
            {props.title}
        </h1>
	</div>
}