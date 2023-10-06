import React from 'react';
import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';
import BrowserOnly from '@docusaurus/BrowserOnly';
import {currentOs} from "./osUtils";

export default function BasicInstall(props){
  return  <BrowserOnly>{() =>
      <div>
        <Tabs 
          groupId="operating-systems"
          defaultValue={currentOs()}
          values={[
          {label: 'Windows', value: 'windows'},
          {label: 'macOS', value: 'mac'},
          {label: 'Linux', value: 'linux'},
          {label: 'GitHub Actions', value: 'gha'}
        ]}>
          
          <TabItem value="windows">
            <a className="no_monospace" href="https://github.com/Virtuslab/scala-cli/releases/latest/download/scala-cli-x86_64-pc-win32.msi">
              Download Scala CLI for Windows
            </a>
          </TabItem>

          <TabItem value="linux" >
            <p>Run the following one-line command in your terminal:</p>
            <code>
              curl -sSLf https://scala-cli.virtuslab.org/get | sh
            </code>
          </TabItem>

          <TabItem value="mac">
            <p>Run the following one-line command in your terminal:</p>
            <code>
              brew install Virtuslab/scala-cli/scala-cli
            </code>
          </TabItem>

          <TabItem value="gha">
            <p>Add the <a href="https://github.com/VirtusLab/scala-cli-setup">scala-cli-setup</a> action to your workflow:</p>
            <code>
              steps:<br/>
              &nbsp;&nbsp;&nbsp;&nbsp;- uses: coursier/cache-action@v6<br/>
              &nbsp;&nbsp;&nbsp;&nbsp;- uses: VirtusLab/scala-cli-setup@main<br/>
            </code>
          </TabItem>

        </Tabs>
      </div>          
  }</BrowserOnly>
}
