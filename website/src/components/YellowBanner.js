import React from 'react';
import Section from './Section';

export default function YellowBanner(props){ 
  return <Section className="section-yellow-banner">
		<div className="row row--align-center">
			<div className="col col--6">
				<h1>{props.title}</h1>
				<div className="description">
					{props.children}
				</div>
			</div>
			<div className="col col--6">
				<div className="image-wrapper">
					<div className="image" style={{ backgroundImage : `url(${props.image})` }}></div>
				</div>
			</div>
		</div>
	</Section>
}