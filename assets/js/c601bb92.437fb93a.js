"use strict";(self.webpackChunkwebsite=self.webpackChunkwebsite||[]).push([[557],{3905:function(e,t,n){n.d(t,{Zo:function(){return u},kt:function(){return f}});var r=n(7294);function o(e,t,n){return t in e?Object.defineProperty(e,t,{value:n,enumerable:!0,configurable:!0,writable:!0}):e[t]=n,e}function a(e,t){var n=Object.keys(e);if(Object.getOwnPropertySymbols){var r=Object.getOwnPropertySymbols(e);t&&(r=r.filter((function(t){return Object.getOwnPropertyDescriptor(e,t).enumerable}))),n.push.apply(n,r)}return n}function c(e){for(var t=1;t<arguments.length;t++){var n=null!=arguments[t]?arguments[t]:{};t%2?a(Object(n),!0).forEach((function(t){o(e,t,n[t])})):Object.getOwnPropertyDescriptors?Object.defineProperties(e,Object.getOwnPropertyDescriptors(n)):a(Object(n)).forEach((function(t){Object.defineProperty(e,t,Object.getOwnPropertyDescriptor(n,t))}))}return e}function i(e,t){if(null==e)return{};var n,r,o=function(e,t){if(null==e)return{};var n,r,o={},a=Object.keys(e);for(r=0;r<a.length;r++)n=a[r],t.indexOf(n)>=0||(o[n]=e[n]);return o}(e,t);if(Object.getOwnPropertySymbols){var a=Object.getOwnPropertySymbols(e);for(r=0;r<a.length;r++)n=a[r],t.indexOf(n)>=0||Object.prototype.propertyIsEnumerable.call(e,n)&&(o[n]=e[n])}return o}var l=r.createContext({}),s=function(e){var t=r.useContext(l),n=t;return e&&(n="function"==typeof e?e(t):c(c({},t),e)),n},u=function(e){var t=s(e.components);return r.createElement(l.Provider,{value:t},e.children)},p={inlineCode:"code",wrapper:function(e){var t=e.children;return r.createElement(r.Fragment,{},t)}},d=r.forwardRef((function(e,t){var n=e.components,o=e.mdxType,a=e.originalType,l=e.parentName,u=i(e,["components","mdxType","originalType","parentName"]),d=s(n),f=o,m=d["".concat(l,".").concat(f)]||d[f]||p[f]||a;return n?r.createElement(m,c(c({ref:t},u),{},{components:n})):r.createElement(m,c({ref:t},u))}));function f(e,t){var n=arguments,o=t&&t.mdxType;if("string"==typeof e||o){var a=n.length,c=new Array(a);c[0]=d;var i={};for(var l in t)hasOwnProperty.call(t,l)&&(i[l]=t[l]);i.originalType=e,i.mdxType="string"==typeof e?e:o,c[1]=i;for(var s=2;s<a;s++)c[s]=n[s];return r.createElement.apply(null,c)}return r.createElement.apply(null,n)}d.displayName="MDXCreateElement"},7195:function(e,t,n){n.r(t),n.d(t,{assets:function(){return u},contentTitle:function(){return l},default:function(){return f},frontMatter:function(){return i},metadata:function(){return s},toc:function(){return p}});var r=n(3117),o=n(102),a=(n(7294),n(3905)),c=["components"],i={title:"Clean",sidebar_position:16},l=void 0,s={unversionedId:"commands/clean",id:"commands/clean",title:"Clean",description:"The clean command deletes all of the files generated by scala-cli. This includes the .scala-build directory where Scala CLI writes outputs and stores its caches, and .bsp/scala-cli.json file which contains details for the BSP connection (necessary for importing a Scala CLI build to your IDE of choice).",source:"@site/docs/commands/clean.md",sourceDirName:"commands",slug:"/commands/clean",permalink:"/docs/commands/clean",draft:!1,editUrl:"https://github.com/Virtuslab/scala-cli/edit/main/website/docs/commands/clean.md",tags:[],version:"current",sidebarPosition:16,frontMatter:{title:"Clean",sidebar_position:16},sidebar:"tutorialSidebar",previous:{title:"Format",permalink:"/docs/commands/fmt"},next:{title:"Package",permalink:"/docs/commands/package"}},u={},p=[],d={toc:p};function f(e){var t=e.components,n=(0,o.Z)(e,c);return(0,a.kt)("wrapper",(0,r.Z)({},d,n,{components:t,mdxType:"MDXLayout"}),(0,a.kt)("p",null,"The ",(0,a.kt)("inlineCode",{parentName:"p"},"clean")," command deletes all of the files generated by ",(0,a.kt)("inlineCode",{parentName:"p"},"scala-cli"),". This includes the ",(0,a.kt)("inlineCode",{parentName:"p"},".scala-build")," directory where Scala CLI writes outputs and stores its caches, and ",(0,a.kt)("inlineCode",{parentName:"p"},".bsp/scala-cli.json")," file which contains details for the BSP connection (necessary for importing a Scala CLI build to your IDE of choice)."))}f.isMDXComponent=!0}}]);