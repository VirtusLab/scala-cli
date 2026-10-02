import type {ReactNode} from 'react';
import {useRef, useState} from 'react';
import Tabs from '@theme/Tabs';
import TabItem from '@theme/TabItem';
import {Check, Copy} from 'lucide-react';
import {currentOs} from './osUtils';
import {cn} from '../lib/utils';

function InstallCode({children}: {children: ReactNode}) {
  const codeRef = useRef<HTMLElement>(null);
  const [copied, setCopied] = useState(false);

  const onCopy = async () => {
    const text = codeRef.current?.innerText ?? '';
    if (!text) return;
    try {
      await navigator.clipboard.writeText(text);
    } catch {
      return;
    }
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1200);
  };

  return (
    <div className="sc-install-code-wrap">
      <code ref={codeRef} className="sc-install-code">
        {children}
      </code>
      <button
        type="button"
        className={cn('sc-code-copy', copied && 'sc-code-copy--done')}
        onClick={onCopy}
        aria-label={copied ? 'Copied' : 'Copy code'}
        title={copied ? 'Copied' : 'Copy'}
      >
        {copied ? (
          <Check size={16} strokeWidth={2} aria-hidden />
        ) : (
          <Copy size={16} strokeWidth={2} aria-hidden />
        )}
      </button>
    </div>
  );
}

export default function BasicInstall() {
  // Keep all OS install commands in SSR HTML for crawlers / LLMs.
  // currentOs() falls back to 'linux' when window is unavailable.
  return (
    <div className="install-tabs">
      <Tabs
        groupId="operating-systems"
        defaultValue={currentOs()}
        values={[
          {label: 'macOS', value: 'mac'},
          {label: 'Linux', value: 'linux'},
          {label: 'Windows', value: 'windows'},
          {label: 'GitHub Actions', value: 'gha'},
        ]}
      >
        <TabItem value="windows">
          <p>
            Install Scala CLI with{' '}
            <a
              className="no_monospace"
              href="https://learn.microsoft.com/en-us/windows/package-manager/winget/#install-winget"
            >
              WinGet
            </a>{' '}
            by running the following one-line command in your terminal:
          </p>
          <InstallCode>winget install virtuslab.scalacli</InstallCode>
        </TabItem>

        <TabItem value="linux">
          <p>Run the following one-line command in your terminal:</p>
          <InstallCode>
            curl -sSLf https://scala-cli.virtuslab.org/get | sh
          </InstallCode>
        </TabItem>

        <TabItem value="mac">
          <p>
            Install Scala CLI with{' '}
            <a className="no_monospace" href="https://brew.sh/">
              Homebrew
            </a>{' '}
            by running the following one-line command in your terminal:
          </p>
          <InstallCode>
            brew install Virtuslab/scala-cli/scala-cli
          </InstallCode>
        </TabItem>

        <TabItem value="gha">
          <p>
            Add the{' '}
            <a href="https://github.com/VirtusLab/scala-cli-setup">
              scala-cli-setup
            </a>{' '}
            action to your workflow:
          </p>
          <InstallCode>
            steps:
            <br />
            &nbsp;&nbsp;&nbsp;&nbsp;- uses: coursier/cache-action@v6
            <br />
            &nbsp;&nbsp;&nbsp;&nbsp;- uses: VirtusLab/scala-cli-setup@main
            <br />
          </InstallCode>
        </TabItem>
      </Tabs>
    </div>
  );
}
