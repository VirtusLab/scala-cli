import React from 'react';

export default function SectionAbout(props){ 
  return <div className="section-about__wrapper row">
        <div className="col col--1 big-title pre-title">
            &gt;_
        </div>
        <div className="col col--3 big-title">
            <span className="pre-title-mobile">&gt;_</span> {props.title}
        </div>
        <div className="col col--8 description">
            {props.children}
        </div>
</div>
}