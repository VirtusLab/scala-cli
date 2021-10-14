import React from 'react';
import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';
import BrowserOnly from '@docusaurus/BrowserOnly';

const currentOs = () => {
  const isCurrentOsEqual = (osShortName) => window.navigator.userAgent.indexOf(osShortName) !== -1
  if(isCurrentOsEqual("Win")) return "win"
  if(isCurrentOsEqual("Mac")) return "mac"
  else return "linux"
}

export default function BasicInstall(props){
  return  <BrowserOnly>{() =>
      <div>
        <Tabs 
          groupId="operating-systems"
          defaultValue={currentOs()}
          values={[
          {label: 'Windows', value: 'win'},
          {label: 'macOS', value: 'mac'},
          {label: 'Linux', value: 'linux'},
        ]}>
          
          <TabItem value="win">
            <a className="no_monospace" href="https://github.com/Virtuslab/scala-cli/releases/latest/download/scala-cli-x86_64-pc-win32.msi">
              Download Scala CLI for Windows
            </a>
          </TabItem>

          <TabItem value="linux" >
            <p>Run the following one-line command in your terminal</p>
            <code>
              curl -sSLf https://virtuslab.github.io/scala-cli-packages/scala-setup.sh | sh
            </code>
          </TabItem>

          <TabItem value="mac">
            <p>Run the following one-line command in your terminal</p>
            <code>
              brew install Virtuslab/scala-cli/scala-cli
            </code>
          </TabItem>

        </Tabs>
      </div>          
  }</BrowserOnly>
}