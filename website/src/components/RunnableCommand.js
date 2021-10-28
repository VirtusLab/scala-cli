import React from 'react';
import CodeBlock from '@theme/CodeBlock';

export const RunnableCommand = ({command, output, title, language}) => {
    const languageStyle = language ? "language-" + language : "language-bash"
    return (
        <div className="runnable-command">
            <CodeBlock className={languageStyle} title={title}>{command}</CodeBlock>
            <pre className="output">{output}</pre>
        </div>
    )
}