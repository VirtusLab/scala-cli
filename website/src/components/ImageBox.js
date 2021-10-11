import React from 'react';
import ThemedImage from '@theme/ThemedImage';


export default function ImageBox(props){ 
  	return <div className={"section-image-box__row row "}>
				
		<div className="section-image-box__row-text col col--1 left-margin-stub"/>
		<div className={"section-image-box__row-text col col--5 " }>
			<div className="section-image-box__row-text-wrapper">
				<h3>{props.title}</h3>
				<div className="content">
					{props.children}
				</div>
			</div>
		</div>

		<div className = {"section-image-box__row-image col col--6 " }>
			<div className="section-image-box__row-image-wrapper">
				{!props.image ? "" : <div className="green_border">
					<ThemedImage
					alt={props.image}
					sources={{
						light: `/img/${props.image}`,
						dark: `/img/dark/${props.image}`,
					}}
					/>
				</div>}
			</div>
		</div>
		<div className="section-image-box__row-text col col--1 right-margin-stub"/>
	</div>
}