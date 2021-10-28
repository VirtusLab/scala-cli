import React from 'react';
import CodeBlock from '@theme/CodeBlock';

export const RunnableCommand = ({command, commandLanguage, output, outputLanguage, title}) => {
    const languageToClass = (language, defaultClass) => language ? "language-" + language : defaultClass

    const _command = typeof command == "string" ? command.trim() : command
    return (
        <div className="runnable-command">
            <CodeBlock className={languageToClass(commandLanguage, "language-bash")} title={title}>{_command}</CodeBlock>
            <CodeBlock className={"output " + languageToClass(outputLanguage, "language-scala")}>
                {output}
            </CodeBlock>
        </div>
    )
}