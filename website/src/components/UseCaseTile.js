import React from 'react';

export default function UseCaseTile(props) { 
    const isLink = props.slug ? true : false;

    if( isLink ) {
        return <a href={"/" + props.slug } className="col col--4 use-box-wrapper">
        <div className="use-box">
  
          <div className="icon-wrapper">
            <img src={"img/ico-" + props.slug + ".png"} alt={props.slug + " icon"} />
          </div>
  
          <h3>
            {props.title}
          </h3>
  
          <p>
            {props.description}
          </p>
  
          <div className="read-more-wrap">
            <div className="read-more with-before">
              Read more
            </div>
          </div>
  
        </div>
      </a>
    } else {
        return <div className="col col--4 use-box-wrapper">
        <div className="use-box your-case">

          <div className="icon-wrapper">
            <img className="light-theme" src="img/ico-yours.png" alt="your use case icon" />
            <img className="dark-theme" src="img/ico-yours-dark.png" alt="your use case icon" />
          </div>

          <h3>
            {props.title}
          </h3>

          <p>
            {props.description}
          </p>

        </div>
      </div>
    }
}