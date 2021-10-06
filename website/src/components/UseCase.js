import React from 'react';
import Section from './Section';
import YellowBanner from './YellowBanner';
import allFeatures from './features';
import Layout from '@theme/Layout';
import BigHeader from './BigHeader';

export default function UseCase(props){ 
  return <Layout title={props.title} description={props.description} key={props.title}>
    <div className="container content">
      <YellowBanner image={props.image} title={props.headline}>
        {props.children}
      </YellowBanner>
    
      
      <BigHeader title={props.title} colsize="12" promptsign={true}></BigHeader>


      <Section className="section-image-box">
        {allFeatures().filter(f => f.props[props.id])}
      </Section>
    </div>
  </Layout>
}