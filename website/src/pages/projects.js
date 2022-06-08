import React from 'react';

import UseCase from "../components/UseCase"

const Index = (props) => {
  return <UseCase
    title="Single-module projects with Scala CLI"
    description="Page describing why Scala CLI is good for maintaining single-module projects."
    headline="Fight with your bugs, not with your buildtool"
    image="gifs/projects.gif"
    id="projects"
    >
    <p>Scala CLI provides all the functionality to easily maintain single module projects like cli apps or simple microservices.</p>
  </UseCase>;
};

export default Index;
