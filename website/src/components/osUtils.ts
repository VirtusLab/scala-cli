export type OsTab = 'windows' | 'linux' | 'mac' | 'gha';

export function currentOs(): OsTab {
  const isCurrentOsEqual = (osShortName: string): boolean => {
    if (typeof window !== 'undefined') {
      return window.navigator.userAgent.indexOf(osShortName) !== -1;
    }
    return false;
  };
  if (isCurrentOsEqual('Win')) return 'windows';
  if (isCurrentOsEqual('Mac')) return 'mac';
  return 'linux';
}
