import React from 'react';
import Section from './Section';
import ThemedImage from '@theme/ThemedImage';

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
          <ThemedImage
            className="image"
            alt={props.image}
            sources={{
              light: `img/${props.image}`,
              dark: `img/dark/${props.image}`,
            }}
            />
				</div>
			</div>
		</div>
	</Section>
}