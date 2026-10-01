import {useCallback} from 'react';
import clsx from 'clsx';
import useIsBrowser from '@docusaurus/useIsBrowser';
import {useColorMode} from '@docusaurus/theme-common';
import {Moon, Sun} from 'lucide-react';

/**
 * Shadcn-style theme switch for the navbar (light ↔ dark).
 * Thumb shows the active mode icon.
 */
export default function ThemeSwitch({className}: {className?: string}) {
  const isBrowser = useIsBrowser();
  const {colorMode, setColorMode} = useColorMode();
  const isDark = colorMode === 'dark';

  const toggle = useCallback(() => {
    setColorMode(isDark ? 'light' : 'dark');
  }, [isDark, setColorMode]);

  return (
    <button
      type="button"
      role="switch"
      aria-checked={isDark}
      aria-label={isDark ? 'Switch to light mode' : 'Switch to dark mode'}
      title={isDark ? 'Light mode' : 'Dark mode'}
      disabled={!isBrowser}
      onClick={toggle}
      className={clsx('sc-theme-switch', isDark && 'sc-theme-switch--on', className)}
    >
      <span className="sc-theme-switch__track" aria-hidden>
        <Sun
          className="sc-theme-switch__icon sc-theme-switch__icon--sun"
          size={14}
          strokeWidth={2.25}
        />
        <Moon
          className="sc-theme-switch__icon sc-theme-switch__icon--moon"
          size={14}
          strokeWidth={2.25}
        />
      </span>
      <span className="sc-theme-switch__thumb" aria-hidden>
        <Sun
          className="sc-theme-switch__thumb-icon sc-theme-switch__thumb-icon--sun"
          size={14}
          strokeWidth={2.25}
        />
        <Moon
          className="sc-theme-switch__thumb-icon sc-theme-switch__thumb-icon--moon"
          size={14}
          strokeWidth={2.25}
        />
      </span>
    </button>
  );
}
