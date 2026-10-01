import {ExternalLink} from 'lucide-react';
import {cn} from '../lib/utils';

type DownloadButtonProps = {
  href: string;
  desc: string;
  /** @deprecated unused — kept for MDX call sites */
  width?: string;
  className?: string;
};

export default function DownloadButton({
  href,
  desc,
  className,
}: DownloadButtonProps) {
  return (
    <a
      href={href}
      target="_blank"
      rel="noopener noreferrer"
      className={cn('sc-download-btn', className)}
    >
      <span>{desc}</span>
      <ExternalLink size={14} strokeWidth={2} aria-hidden />
    </a>
  );
}
