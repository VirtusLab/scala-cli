import React from 'react';

import UseCase from "../components/UseCase"

const Index = (props) => {
  return <UseCase
    title="Scripting with Scala CLI"
    description="Page describing why Scala CLI is good for scripting with Scala."
    headline="Scripting using all the powers of the Scala ecosystem"
    image="gifs/scripting.gif"
    id="scripting"
    >
      <p>Scala-cli allows you to use Scala to create and enhance scripts with using all the goodies of Scala.</p>
        
      <p>Use dependencies, declare tests or even package your scripts into native applications!</p>
  </UseCase>;
};

export default Index;

// const Index = (props) => {
//   const { siteConfig } = useDocusaurusContext();
//   return (
//     <Layout title="Scripting with Scala CLI" description="Page describing why Scala CLI is good for scripting with Scala.">
//       <div className="container padding--sm content">
        
//         <HeaderSection image="img/fast-scala-cli.gif">
//           <h1>Scripting using all powers of Scala ecosystem</h1>
//           {/* TODO: better text */}
//           <p>Scala-cli allows you to use Scala to create and enhance scripts with using all the goodies of Scala.</p>
        
//           <p>Use dependencies, declare tests or even package your scripts into native applications!</p>
//         </HeaderSection>
      
//         <h1>Scripting with Scala CLI</h1>


//       </div>
//     </Layout>
//   );
// };
