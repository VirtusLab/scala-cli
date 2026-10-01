/// <reference types="@docusaurus/module-type-aliases" />
/// <reference types="@docusaurus/theme-classic" />

declare module '*.mdx' {
  import type {ComponentType} from 'react';
  const Component: ComponentType<Record<string, unknown>>;
  export default Component;
}

declare module '@theme/ThemedImage' {
  import type {ComponentType} from 'react';
  type Sources = {light: string; dark: string};
  type Props = {
    sources: Sources;
    className?: string;
    alt?: string;
  };
  const ThemedImage: ComponentType<Props>;
  export default ThemedImage;
}

declare module '@theme/Tabs' {
  import type {ComponentType, ReactNode} from 'react';
  type Props = {
    groupId?: string;
    defaultValue?: string;
    values: {label: string; value: string}[];
    children?: ReactNode;
  };
  const Tabs: ComponentType<Props>;
  export default Tabs;
}

declare module '@theme/TabItem' {
  import type {ComponentType, ReactNode} from 'react';
  type Props = {value: string; children?: ReactNode};
  const TabItem: ComponentType<Props>;
  export default TabItem;
}
