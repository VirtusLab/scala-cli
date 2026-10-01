import type {ReactNode} from 'react';
import ThemedImage from '@theme/ThemedImage';

export type ImageBoxProps = {
  id?: string;
  image?: string;
  title: string;
  children?: ReactNode;
  education?: boolean | string;
  scripting?: boolean | string;
  prototyping?: boolean | string;
  projects?: boolean | string;
};

export default function ImageBox({id, image, title, children}: ImageBoxProps) {
  return (
    <article
      id={id}
      className="sc-feature-block grid scroll-mt-[calc(var(--ifm-navbar-height)+3.25rem)] items-center gap-5 border-b border-slate-200/70 py-14 last:border-b-0 first:pt-0 last:pb-0 md:grid-cols-2 md:gap-6 md:py-20 md:first:pt-0 md:last:pb-0 dark:border-white/10"
    >
      <div>
        <h3 className="sc-display mb-5 text-[1.65rem] leading-tight text-ink md:text-[2rem] dark:text-white">
          {title}
        </h3>
        <div className="sc-prose space-y-4 font-sans text-base leading-7 text-fg-muted md:text-lg md:leading-8 dark:text-slate-300">
          {children}
        </div>
      </div>
      <div className="relative">
        {image ? (
          <>
            <div
              className="pointer-events-none absolute -inset-4 rounded-xl bg-[radial-gradient(circle_at_center,rgba(242,49,53,0.12),transparent_70%)] blur-xl dark:bg-[radial-gradient(circle_at_center,rgba(255,213,131,0.12),transparent_70%)]"
              aria-hidden
            />
            <div className="sc-console-frame relative">
              <ThemedImage
                className="sc-console-media"
                alt={image}
                sources={{
                  light: `/img/${image}`,
                  dark: `/img/dark/${image}`,
                }}
              />
            </div>
          </>
        ) : null}
      </div>
    </article>
  );
}
