"use strict";(self.webpackChunkwebsite=self.webpackChunkwebsite||[]).push([[622],{2354:(e,s,i)=>{i.r(s),i.d(s,{assets:()=>r,contentTitle:()=>o,default:()=>u,frontMatter:()=>c,metadata:()=>a,toc:()=>d});var n=i(4848),l=i(8453),t=i(2267);const c={title:"Publish Local \u26a1\ufe0f",sidebar_position:21},o=void 0,a={id:"commands/publishing/publish-local",title:"Publish Local \u26a1\ufe0f",description:"The Publish Local command is restricted and requires setting the --power option to be used.",source:"@site/docs/commands/publishing/publish-local.md",sourceDirName:"commands/publishing",slug:"/commands/publishing/publish-local",permalink:"/docs/commands/publishing/publish-local",draft:!1,unlisted:!1,editUrl:"https://github.com/Virtuslab/scala-cli/edit/main/website/docs/commands/publishing/publish-local.md",tags:[],version:"current",sidebarPosition:21,frontMatter:{title:"Publish Local \u26a1\ufe0f",sidebar_position:21},sidebar:"tutorialSidebar",previous:{title:"Publish \u26a1\ufe0f",permalink:"/docs/commands/publishing/publish"},next:{title:"Bloop \u26a1\ufe0f",permalink:"/docs/commands/misc/bloop"}},r={},d=[{value:"Usage",id:"usage",level:2},{value:"Required settings",id:"required-settings",level:2}];function h(e){const s={a:"a",admonition:"admonition",code:"code",h2:"h2",p:"p",pre:"pre",...(0,l.R)(),...e.components};return(0,n.jsxs)(n.Fragment,{children:[(0,n.jsxs)(s.admonition,{type:"caution",children:[(0,n.jsxs)(s.p,{children:["The Publish Local command is restricted and requires setting the ",(0,n.jsx)(s.code,{children:"--power"})," option to be used.\nYou can pass it explicitly or set it globally by running:"]}),(0,n.jsx)(s.p,{children:"scala-cli config power true"})]}),"\n",(0,n.jsxs)(s.admonition,{type:"caution",children:[(0,n.jsxs)(s.p,{children:["The ",(0,n.jsx)(s.code,{children:"publish local"})," sub-command is an experimental feature."]}),(0,n.jsxs)(s.p,{children:["Please bear in mind that non-ideal user experience should be expected.\nIf you encounter any bugs or have feedback to share, make sure to reach out to the maintenance team\non ",(0,n.jsx)(s.a,{href:"https://github.com/VirtusLab/scala-cli",children:"GitHub"}),"."]})]}),"\n","\n","\n",(0,n.jsxs)(s.p,{children:["The ",(0,n.jsx)(s.code,{children:"publish local"})," sub-command publishes a Scala CLI project in the local Ivy2\nrepository, just like how ",(0,n.jsx)(s.code,{children:"sbt publishLocal"})," or ",(0,n.jsx)(s.code,{children:"mill __.publishLocal"})," do. This\nrepository usually lives under ",(0,n.jsx)(s.code,{children:"~/.ivy2/local"}),", and is taken into account most of\nthe time by most Scala tools when fetching artifacts."]}),"\n",(0,n.jsx)(s.h2,{id:"usage",children:"Usage"}),"\n",(0,n.jsx)(s.p,{children:"To publish locally a Scala CLI project, run"}),"\n",(0,n.jsxs)(t.Z,{children:[(0,n.jsx)(s.pre,{children:(0,n.jsx)(s.code,{className:"language-sh",children:"scala-cli publish local .\n"})}),(0,n.jsx)(s.pre,{children:(0,n.jsx)(s.code,{className:"language-text",children:"Publishing io.github.scala-cli:hello-scala-cli_3:0.1.0-SNAPSHOT\n \u2714 Computed 10 checksums\n \ud83d\ude9a Wrote 15 files\n\n \ud83d\udc40 Check results at\n  ~/.ivy2/local/io.github.scala-cli/hello-scala-cli_3/0.1.0-SNAPSHOT/\n"})}),(0,n.jsxs)(s.admonition,{type:"caution",children:[(0,n.jsxs)(s.p,{children:["The ",(0,n.jsx)(s.code,{children:"publish local"})," sub-command does not currently support publishing of the test scope.\nThis includes any file that is placed in ",(0,n.jsx)(s.code,{children:"test"})," directory or with the ",(0,n.jsx)(s.code,{children:".test.scala"})," suffix."]}),(0,n.jsxs)(s.p,{children:["Read more about test sources in ",(0,n.jsx)(s.a,{href:"/docs/commands/test#test-sources",children:"testing documentation"}),"."]})]})]}),"\n",(0,n.jsx)(s.h2,{id:"required-settings",children:"Required settings"}),"\n",(0,n.jsxs)(s.p,{children:["The ",(0,n.jsx)(s.code,{children:"publish local"})," command needs the ",(0,n.jsxs)(s.a,{href:"/docs/commands/publishing/publish#required-settings",children:["same required settings as the ",(0,n.jsx)(s.code,{children:"publish"})," command"]}),". Like for ",(0,n.jsx)(s.code,{children:"publish"}),", Scala CLI might already be able to compute sensible defaults\nfor those."]})]})}function u(e={}){const{wrapper:s}={...(0,l.R)(),...e.components};return s?(0,n.jsx)(s,{...e,children:(0,n.jsx)(h,{...e})}):h(e)}},2267:(e,s,i)=>{i.d(s,{Z:()=>t,b:()=>c});i(6540);var n=i(3554),l=i(4848);function t(e){let{children:s}=e;return(0,l.jsx)("div",{className:"runnable-command",children:s})}function c(e){let{url:s}=e;return(0,l.jsx)(n.A,{playing:!0,loop:!0,muted:!0,controls:!0,width:"100%",height:"",url:s})}}}]);