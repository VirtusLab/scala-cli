import React from 'react';
import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';
import BrowserOnly from '@docusaurus/BrowserOnly';

const windowsPlatforms = ['Win32', 'Win64', 'Windows', 'WinCE']

function defaultOS(){ return window ? (window.navigator.userAgent.indexOf("Win") != -1 ? "win" : "mac/linux") : "mac/linux" }

export default function BasicInstall(props){
  return  <BrowserOnly>{() =>
      <div>
        <Tabs 
          groupId="operating-systems"
          defaultValue={defaultOS()}
          values={[
          {label: 'Windows', value: 'win'},
          {label: 'macOS/Linux', value: 'mac/linux'},
        ]}>
          
          <TabItem value="win">
            <a className="no_monospace" href="https://github.com/Virtuslab/scala-cli/releases/download/nightly/scala-cli-x86_64-pc-win32.msi">
              Download Scala CLI for Windows
            </a>
          </TabItem>

          <TabItem value="mac/linux">
            <p>Run the following one-line command in your terminal</p>
            <code> bash
              curl -sSLf https://virtuslab.github.io/scala-cli-packages/scala-setup.sh | sh
            </code>
          </TabItem>

        </Tabs>
      </div>          
  }</BrowserOnly>
}