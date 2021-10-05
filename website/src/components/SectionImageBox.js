import React from 'react';

import Section from "../components/Section"
import ImageBox from "../components/ImageBox"
import SmallHeader from "../components/SmallHeader"

export default function SectionImageBox(props){ 
  	return <Section className="section-image-box">

        <SmallHeader title="Still undecided?">
            Here come our <span>main features</span>
        </SmallHeader>

        <ImageBox title="Scala versions, dependencies and JVMs" image="img/envs.gif" imgpos="right">
            <p>
                Scala CLI is built on top of coursier. This allows us to manage Scala versions, dependencies and JVMs so you can test your code in different environments by changing single option.
            </p>
            <p>
                Scala CLI ships with all its dependencies. No need to fluff with installing JVM or setting up PATH.
            </p>
        </ImageBox>

        <ImageBox title="Scala versions, dependencies and JVMs" image="img/envs.gif" imgpos="left">
            <p>
                Scala CLI is built on top of coursier. This allows us to manage Scala versions, dependencies and JVMs so you can test your code in different environments by changing single option.
            </p>
            <p>
                Scala CLI ships with all its dependencies. No need to fluff with installing JVM or setting up PATH.
            </p>
        </ImageBox>

        <ImageBox title="Scala versions, dependencies and JVMs" image="img/envs.gif" imgpos="right">
            <p>
                Scala CLI is built on top of coursier. This allows us to manage Scala versions, dependencies and JVMs so you can test your code in different environments by changing single option.
            </p>
            <p>
                Scala CLI ships with all its dependencies. No need to fluff with installing JVM or setting up PATH.
            </p>
        </ImageBox>

    </Section>
}