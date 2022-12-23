import React from 'react';

import UseCase from "../components/UseCase"

const Index = (props) => {
  return <UseCase
    title="Prototyping, experimenting, reproducing bugs with Scala CLI"
    description="Page describing why Scala CLI is good for prototyping / experimenting / reproducing bugs."
    headline="Move fast and break things but be in control of your build"
    image="gifs/prototyping.gif"
    id="prototyping"
    >
    <p>Have you ever wasted time prototyping, experimenting or reproducing a nasty bug by testing in a different environment than you intended?</p>

    <p>With Scala CLI you can explicitly define Scala or JVM versions, platform, compiler options and dependencies by setting them as arguments.</p>
  </UseCase>;
};

export default Index;
