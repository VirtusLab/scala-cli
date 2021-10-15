import React from 'react';

export default function SectionAbout(props){
  const id = props.title.toLowerCase().split(" ").join("-")
  const link = <a href={"#" + id} >&gt;_</a> 
  return <div className="section-about__wrapper row" id={id}>
        <div className="col col--1 big-title pre-title">{link}</div>
        <div className="col col--3 big-title">
            <span className="pre-title-mobile">{link}</span> {props.title}
        </div>
        <div className="col col--8 description">
            {props.children}
        </div>
</div>
}