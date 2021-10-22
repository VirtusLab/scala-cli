import React from 'react';

import UseCase from "../components/UseCase"

const Index = (props) => {
  return <UseCase
    title="Prototyping, experimenting, reproducing bugs with Scala CLI"
    description="Page describing why Scala CLI is good for prototyping / experimenting / reproducing bugs."
    headline="Move fast and break things but be in control of your build"
    image="prototyping.svg"
    id="prototyping"
    >
    <p>If you ever waste time prototyping, experimenting or reproducing a nasty bug by testing different environment then you intendted?</p>

    <p>With Scala CLI defining Scala or JVM versions, platform, compiler options and dependencies is setting an argument.</p>
  </UseCase>;
};

export default Index;
