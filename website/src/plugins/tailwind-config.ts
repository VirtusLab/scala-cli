import type {Plugin, PostCssOptions} from '@docusaurus/types';
import tailwindcss from '@tailwindcss/postcss';

export default function tailwindPlugin(): Plugin {
  return {
    name: 'tailwindcss-plugin',
    configurePostCss(postcssOptions: PostCssOptions) {
      postcssOptions.plugins.push(tailwindcss);
      return postcssOptions;
    },
  };
}
