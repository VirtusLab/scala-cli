import React from 'react';
import ReactPlayer from 'react-player'

export function ChainedSnippets({children}){
    return (
        <div className="runnable-command">
            {children}
        </div>
    )
}

export function GiflikeVideo({url}){
  return <ReactPlayer 
    playing loop muted controls 
    width="100%" height=""
    url={url} />
}