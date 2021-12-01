import React from 'react';

import UseCase from "../components/UseCase"

const Index = (props) => {
  return <UseCase
    title="Education with Scala CLI"
    description="Page describing why Scala CLI is good within educational purposes, mainly learning Scala."
    headline="Learn a language not a build tool"
    image="gifs/education.gif"
    id="education"
    >
    <p>Scala-cli is deigned in a way so you can focus on learning, not struggle with installation or build tool.</p>
  </UseCase>;
};

export default Index;
