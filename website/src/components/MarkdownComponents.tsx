import type {ReactNode} from 'react';
import ReactPlayer from 'react-player';

export function ChainedSnippets({children}: {children: ReactNode}) {
  return <div className="runnable-command">{children}</div>;
}

export function GiflikeVideo({url}: {url: string}) {
  return (
    <ReactPlayer
      playing
      loop
      muted
      controls
      width="100%"
      height=""
      src={url}
    />
  );
}
