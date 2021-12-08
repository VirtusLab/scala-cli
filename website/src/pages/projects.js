import React from 'react';

import UseCase from "../components/UseCase"

const Index = (props) => {
  return <UseCase
    title="Single-module projects with Scala CLI"
    description="Page describing why Scala CLI is good for maintainig a single-module projects."
    headline="Fight with your bugs not with with you buildtool"
    image="gifs/projects.gif"
    id="projects"
    >
    <p>Scala CLI provides all tools to maintain with easy single module projects like cli apps or single microservice.</p>
  </UseCase>;
};

export default Index;
